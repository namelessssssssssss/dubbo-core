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
package org.apache.dubbo.config.utils;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.status.reporter.FrameworkStatusReportService;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.registry.Constants;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_SERVICE_CLASS_NAME;
import static org.apache.dubbo.common.constants.CommonConstants.REMOVE_VALUE_PREFIX;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_PARAMETER_FORMAT_ERROR;

import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_REGISTER_MODE_ALL;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_REGISTER_MODE_INSTANCE;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_REGISTER_MODE_INTERFACE;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTER_MODE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_TYPE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.DUBBO_REGISTER_MODE_DEFAULT_KEY;
import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;
import static org.apache.dubbo.config.Constants.IGNORE_CHECK_KEYS;
import static org.apache.dubbo.config.Constants.REGISTER_KEY;

public class ConfigValidationUtils {
    public static final String IPV6_START_MARK = "[";
    public static final String IPV6_END_MARK = "]";
    /**
     * The maximum length of a <b>parameter's value</b>
     */
    private static final int MAX_LENGTH = 200;
    /**
     * The maximum length of a <b>path</b>
     */
    private static final int MAX_PATH_LENGTH = 200;
    /**
     * The rule qualification for <b>name</b>
     */
    private static final Pattern PATTERN_NAME = Pattern.compile("[\\-._0-9a-zA-Z]+");
    /**
     * The rule qualification for <b>multiply name</b>
     */
    private static final Pattern PATTERN_MULTI_NAME = Pattern.compile("[,\\-._0-9a-zA-Z]+");
    /**
     * The rule qualification for <b>method names</b>
     */
    private static final Pattern PATTERN_METHOD_NAME = Pattern.compile("[a-zA-Z][0-9a-zA-Z]*");
    /**
     * The rule qualification for <b>path</b>
     */
    private static final Pattern PATTERN_PATH = Pattern.compile("[/\\-$._0-9a-zA-Z]+");
    /**
     * The pattern matches a value who has a symbol
     */
    private static final Pattern PATTERN_NAME_HAS_SYMBOL = Pattern.compile("[:*,\\s/\\-._0-9a-zA-Z]+");
    /**
     * The pattern matches a property key
     */
    private static final Pattern PATTERN_KEY = Pattern.compile("[*,\\-._0-9a-zA-Z]+");
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ConfigValidationUtils.class);

    public static List<URL> loadRegistries(AbstractInterfaceConfig interfaceConfig, boolean provider) {
        // check && override if necessary
        List<URL> registryList = new ArrayList<>();
        ApplicationConfig application = interfaceConfig.getApplication();
        List<RegistryConfig> registries = interfaceConfig.getRegistries();
        if (CollectionUtils.isNotEmpty(registries)) {
            for (RegistryConfig config : registries) {
                // try to refresh registry in case it is set directly by user using config.setRegistries()
                if (!config.isRefreshed()) {
                    config.refresh();
                }
                String address = config.getAddress();
                if (StringUtils.isEmpty(address)) {
                    address = ANYHOST_VALUE;
                }
                if (!RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(address)) {
                    Map<String, String> map = new HashMap<String, String>();
                    AbstractConfig.appendParameters(map, application);
                    AbstractConfig.appendParameters(map, config);
                    map.put(PATH_KEY, REGISTRY_SERVICE_CLASS_NAME);
                    AbstractInterfaceConfig.appendRuntimeParameters(map);
                    if (!map.containsKey(PROTOCOL_KEY)) {
                        map.put(PROTOCOL_KEY, DUBBO_PROTOCOL);
                    }
                    List<URL> urls = UrlUtils.parseURLs(address, map);

                    for (URL url : urls) {
                        url = URLBuilder.from(url)
                            .addParameter(REGISTRY_KEY, url.getProtocol())
                            .setProtocol(extractRegistryType(url))
                            .setScopeModel(interfaceConfig.getScopeModel())
                            .build();
                        // provider delay register state will be checked in RegistryProtocol#export
                        if (provider && url.getParameter(REGISTER_KEY, true)) {
                            registryList.add(url);
                        }
                        if (!provider && url.getParameter(Constants.SUBSCRIBE_KEY, true)) {
                            registryList.add(url);
                        }
                    }
                }
            }
        }
        return genCompatibleRegistries(interfaceConfig.getScopeModel(), registryList, provider);
    }

    private static List<URL> genCompatibleRegistries(ScopeModel scopeModel, List<URL> registryList, boolean provider) {
        List<URL> result = new ArrayList<>(registryList.size());
        registryList.forEach(registryURL -> {
            if (provider) {
                // for registries enabled service discovery, automatically register interface compatible addresses.
                String registerMode;
                if (SERVICE_REGISTRY_PROTOCOL.equals(registryURL.getProtocol())) {
                    registerMode = registryURL.getParameter(REGISTER_MODE_KEY, ConfigurationUtils.getCachedDynamicProperty(scopeModel, DUBBO_REGISTER_MODE_DEFAULT_KEY, DEFAULT_REGISTER_MODE_INSTANCE));
                    if (!isValidRegisterMode(registerMode)) {
                        registerMode = DEFAULT_REGISTER_MODE_INSTANCE;
                    }
                    result.add(registryURL);
                    if (DEFAULT_REGISTER_MODE_ALL.equalsIgnoreCase(registerMode)
                        && registryNotExists(registryURL, registryList, REGISTRY_PROTOCOL)) {
                        URL interfaceCompatibleRegistryURL = URLBuilder.from(registryURL)
                            .setProtocol(REGISTRY_PROTOCOL)
                            .removeParameter(REGISTRY_TYPE_KEY)
                            .build();
                        result.add(interfaceCompatibleRegistryURL);
                    }
                } else {
                    registerMode = registryURL.getParameter(REGISTER_MODE_KEY, ConfigurationUtils.getCachedDynamicProperty(scopeModel, DUBBO_REGISTER_MODE_DEFAULT_KEY, DEFAULT_REGISTER_MODE_ALL));
                    if (!isValidRegisterMode(registerMode)) {
                        registerMode = DEFAULT_REGISTER_MODE_INTERFACE;
                    }
                    if ((DEFAULT_REGISTER_MODE_INSTANCE.equalsIgnoreCase(registerMode) || DEFAULT_REGISTER_MODE_ALL.equalsIgnoreCase(registerMode))
                        && registryNotExists(registryURL, registryList, SERVICE_REGISTRY_PROTOCOL)) {
                        URL serviceDiscoveryRegistryURL = URLBuilder.from(registryURL)
                            .setProtocol(SERVICE_REGISTRY_PROTOCOL)
                            .removeParameter(REGISTRY_TYPE_KEY)
                            .build();
                        result.add(serviceDiscoveryRegistryURL);
                    }

                    if (DEFAULT_REGISTER_MODE_INTERFACE.equalsIgnoreCase(registerMode) || DEFAULT_REGISTER_MODE_ALL.equalsIgnoreCase(registerMode)) {
                        result.add(registryURL);
                    }
                }

                FrameworkStatusReportService reportService = ScopeModelUtil.getApplicationModel(scopeModel).getBeanFactory().getBean(FrameworkStatusReportService.class);
                reportService.reportRegistrationStatus(reportService.createRegistrationReport(registerMode));
            } else {
                result.add(registryURL);
            }
        });

        return result;
    }

    private static boolean isValidRegisterMode(String mode) {
        return isNotEmpty(mode)
            && (DEFAULT_REGISTER_MODE_INTERFACE.equalsIgnoreCase(mode)
            || DEFAULT_REGISTER_MODE_INSTANCE.equalsIgnoreCase(mode)
            || DEFAULT_REGISTER_MODE_ALL.equalsIgnoreCase(mode)
        );
    }

    private static boolean registryNotExists(URL registryURL, List<URL> registryList, String registryType) {
        return registryList.stream().noneMatch(
            url -> registryType.equals(url.getProtocol()) && registryURL.getBackupAddress().equals(url.getBackupAddress())
        );
    }

    public static void validateConfigCenterConfig(ConfigCenterConfig config) {
        if (config != null) {
            checkParameterName(config.getParameters());
        }
    }

    private static String extractRegistryType(URL url) {
        return UrlUtils.hasServiceDiscoveryRegistryTypeKey(url) ? SERVICE_REGISTRY_PROTOCOL : getRegistryProtocolType(url);
    }

    private static String getRegistryProtocolType(URL url) {
        String registryProtocol = url.getParameter("registry-protocol-type");
        return isNotEmpty(registryProtocol) ? registryProtocol : REGISTRY_PROTOCOL;
    }

    public static void checkExtension(ScopeModel scopeModel, Class<?> type, String property, String value) {
        checkName(property, value);
        if (isNotEmpty(value)
            && !scopeModel.getExtensionLoader(type).hasExtension(value)) {
            throw new IllegalStateException("No such extension " + value + " for " + property + "/" + type.getName());
        }
    }

    /**
     * Check whether there is a <code>Extension</code> who's name (property) is <code>value</code> (special treatment is
     * required)
     *
     * @param type     The Extension type
     * @param property The extension key
     * @param value    The Extension name
     */
    public static void checkMultiExtension(ScopeModel scopeModel, Class<?> type, String property, String value) {
        checkMultiExtension(scopeModel, Collections.singletonList(type), property, value);
    }

    public static void checkMultiExtension(ScopeModel scopeModel, List<Class<?>> types, String property, String value) {
        checkMultiName(property, value);
        if (isNotEmpty(value)) {
            String[] values = value.split("\\s*[,]+\\s*");
            for (String v : values) {
                v = StringUtils.trim(v);
                if (v.startsWith(REMOVE_VALUE_PREFIX)) {
                    continue;
                }
                if (DEFAULT_KEY.equals(v)) {
                    continue;
                }
                boolean match = false;
                for (Class<?> type : types) {
                    if (scopeModel.getExtensionLoader(type).hasExtension(v)) {
                        match = true;
                    }
                }
                if (!match) {
                    throw new IllegalStateException("No such extension " + v + " for " + property + "/" +
                        types.stream().map(Class::getName).collect(Collectors.joining(",")));
                }
            }
        }
    }

    public static void checkLength(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, null);
    }

    public static void checkPathLength(String property, String value) {
        checkProperty(property, value, MAX_PATH_LENGTH, null);
    }

    public static void checkName(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_NAME);
    }

    public static void checkHost(String property, String value) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        if (value.startsWith(IPV6_START_MARK) && value.endsWith(IPV6_END_MARK)) {
            // if the value start with "[" and end with "]", check whether it is IPV6
            try {
                InetAddress.getByName(value);
                return;
            } catch (UnknownHostException e) {
                // not a IPv6 string, do nothing, go on to checkName
            }
        }
        checkName(property, value);
    }

    public static void checkNameHasSymbol(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_NAME_HAS_SYMBOL);
    }

    public static void checkKey(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_KEY);
    }

    public static void checkMultiName(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_MULTI_NAME);
    }

    public static void checkPathName(String property, String value) {
        checkProperty(property, value, MAX_PATH_LENGTH, PATTERN_PATH);
    }

    public static void checkMethodName(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_METHOD_NAME);
    }

    public static void checkParameterName(Map<String, String> parameters) {
        if (CollectionUtils.isEmptyMap(parameters)) {
            return;
        }
        List<String> ignoreCheckKeys = new ArrayList<>();
        ignoreCheckKeys.add(BACKUP_KEY);
        String ignoreCheckKeysStr = parameters.get(IGNORE_CHECK_KEYS);
        if (!StringUtils.isBlank(ignoreCheckKeysStr)) {
            ignoreCheckKeys.addAll(Arrays.asList(ignoreCheckKeysStr.split(",")));
        }
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (!ignoreCheckKeys.contains(entry.getKey())) {
                checkNameHasSymbol(entry.getKey(), entry.getValue());
            }
        }
    }

    public static void checkProperty(String property, String value, int maxlength, Pattern pattern) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        if (value.length() > maxlength) {
            logger.error(CONFIG_PARAMETER_FORMAT_ERROR, "the value content is too long", "", "Parameter value format error. Invalid " +
                property + "=\"" + value + "\" is longer than " + maxlength);
        }
        if (pattern != null) {
            Matcher matcher = pattern.matcher(value);
            if (!matcher.matches()) {
                logger.error(CONFIG_PARAMETER_FORMAT_ERROR, "the value content is illegal character", "", "Parameter value format error. Invalid " +
                    property + "=\"" + value + "\" contains illegal " +
                    "character, only digit, letter, '-', '_' or '.' is legal.");
            }
        }
    }

}