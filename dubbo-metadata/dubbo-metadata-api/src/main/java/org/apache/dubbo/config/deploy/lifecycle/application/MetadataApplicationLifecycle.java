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
package org.apache.dubbo.config.deploy.lifecycle.application;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.deploy.context.ApplicationContext;
import org.apache.dubbo.registry.ApplicationMetadataUpdater;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_REFRESH_INSTANCE_ERROR;

/**
 * Metadata application lifecycle
 */
@Activate
public class MetadataApplicationLifecycle implements ApplicationLifecycle {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MetadataApplicationLifecycle.class);

    @Override
    public boolean needInitialize() {
        return true;
    }

    @Override
    public void refreshServiceInstance(ApplicationContext applicationContext) {
        if (applicationContext.getRegistered().get()) {
            try {
                ApplicationMetadataUpdater updater = applicationContext.getModel().getBeanFactory().getBean(ApplicationMetadataUpdater.class);
                if(updater == null){
                    throw new RuntimeException("MetadataUpdater not found. This may be caused by not imported dubbo-registry-api.");
                }
                updater.refreshMetadataAndInstance(applicationContext.getModel());
            } catch (Exception e) {
                logger.error(CONFIG_REFRESH_INSTANCE_ERROR, "", "", "Refresh instance and metadata error.", e);
            }
        }
    }
}