/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.config;

import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.config.exception.ConfigValidationException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ConfigValidateFacade implements ConfigValidator, ApplicationDeployListener {

    private static final Logger LOGGER = LoggerFactory.getErrorTypeAwareLogger(ConfigValidateFacade.class);

    private final List<ConfigValidator> validators;

    /**
     * For test
     */
    private static boolean enableValidate = true;

    /**
     * For test
     */
    private static final AtomicReference<ConfigValidateFacade> DEFAULT_INSTANCE = new AtomicReference<>();

    public ConfigValidateFacade(ScopeModel scopeModel) {
        if(scopeModel != null) {
            ExtensionLoader<ConfigValidator> extensionLoader = scopeModel.getExtensionLoader(ConfigValidator.class);
            if (extensionLoader != null) {
                this.validators = extensionLoader.getActivateExtensions();
                this.validators.forEach(scopeModel.getBeanFactory()::registerBean);
                DEFAULT_INSTANCE.set(this);
            } else {
                this.validators = Collections.emptyList();
            }
        }else {
            this.validators = Collections.emptyList();
        }
    }

    public List<ConfigValidator> getValidators() {
        return validators;
    }

    /**
     * Auto choose a appropriate validator to validate config.
     *
     * @param config the config to validate
     * @return TRUE if pass the validation.
     * FALSE if no ConfigValidator found for this config,
     * or one of the supported {@link ConfigValidator#validate(AbstractConfig)} returns FALSE.
     */
    @Override
    public boolean validate(AbstractConfig config) throws ConfigValidationException{
        if(!enableValidate){
            return true;
        }
        if (config == null) {
            return false;
        }
        boolean validated = false;
        try {
            for (ConfigValidator validator : validators) {
                if (validator.isSupport(config.getClass())) {
                    validated = true;
                    if(!validator.validate(config)){
                           return false;
                    }
                }
            }
        }catch (Throwable t){
            throw new ConfigValidationException(config.getClass().getSimpleName()+" validation failed: "+t.getMessage(),t);
        }
        if (!validated) {
            LOGGER.info(config.getClass().getSimpleName()+" is not validated. This may caused by you did not imported the relevant module.");
        }
        return validated;
    }

    @Override
    public boolean isSupport(Class configClass) {
        return true;
    }

    @Override
    public void onStarting(ApplicationModel scopeModel) {
        ConfigValidateFacade validateFacade = new ConfigValidateFacade(scopeModel);
        DEFAULT_INSTANCE.compareAndSet(null,validateFacade);
        scopeModel.getBeanFactory().registerBean(validateFacade);
    }

    @Override
    public void onStarted(ApplicationModel scopeModel) {}

    @Override
    public void onInitialize(ApplicationModel scopeModel) {}

    @Override
    public void onStopping(ApplicationModel scopeModel) {}

    @Override
    public void onStopped(ApplicationModel scopeModel) {}

    @Override
    public void onFailure(ApplicationModel scopeModel, Throwable cause) {}


    /**
     * For test
     */
    public static ConfigValidateFacade getDefaultInstance() {
        return DEFAULT_INSTANCE.updateAndGet(configValidateFacade -> configValidateFacade == null ? new ConfigValidateFacade(null) : configValidateFacade);
    }

    /**
     * For test
     */
    public static void setEnableValidate(boolean enable){
        enableValidate = enable;
    }
}
