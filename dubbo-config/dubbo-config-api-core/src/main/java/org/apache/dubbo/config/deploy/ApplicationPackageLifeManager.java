package org.apache.dubbo.config.deploy;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.config.configcenter.DynamicConfigurationFactory;
import org.apache.dubbo.common.config.configcenter.wrapper.CompositeDynamicConfiguration;
import org.apache.dubbo.common.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.common.deploy.PackageLifeManager;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.apache.dubbo.common.config.ConfigurationUtils.parseProperties;
import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_INIT_CONFIG_CENTER;
import static org.apache.dubbo.common.utils.StringUtils.isEmpty;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;

/**
 * The PackageLifeManager always called first.
 */
public class ApplicationPackageLifeManager implements PackageLifeManager {

    private static final String NAME = "ApplicationPackageLifeManager";

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ApplicationPackageLifeManager.class);


    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(DefaultApplicationDeployer applicationDeployer) {
        startConfigCenter(applicationDeployer);

        applicationDeployer.getConfigManager().loadConfigs();

        initModuleDeployers(applicationDeployer);
    }

    private void initModuleDeployers(DefaultApplicationDeployer applicationDeployer) {
        // make sure created default module
        applicationDeployer.getApplicationModel().getDefaultModule();
        // deployer initialize
        for (ModuleModel moduleModel : applicationDeployer.getApplicationModel().getModuleModels()) {
            moduleModel.getDeployer().initialize();
        }
    }


    private void startConfigCenter(DefaultApplicationDeployer applicationDeployer) {
        ConfigManager configManager = applicationDeployer.getConfigManager();
        ApplicationModel applicationModel = applicationDeployer.getApplicationModel();
        Environment environment = applicationModel.getModelEnvironment();

        // load application config
        configManager.loadConfigsOfTypeFromProps(ApplicationConfig.class);

        // try set model name
        if (StringUtils.isBlank(applicationModel.getModelName())) {
            applicationModel.setModelName(applicationModel.tryGetApplicationName());
        }

        // load config centers
        configManager.loadConfigsOfTypeFromProps(ConfigCenterConfig.class);

        useRegistryAsConfigCenterIfNecessary(applicationDeployer);

        // check Config Center
        Collection<ConfigCenterConfig> configCenters = configManager.getConfigCenters();
        if (CollectionUtils.isEmpty(configCenters)) {
            ConfigCenterConfig configCenterConfig = new ConfigCenterConfig();
            configCenterConfig.setScopeModel(applicationModel);
            configCenterConfig.refresh();
            ConfigValidationUtils.validateConfigCenterConfig(configCenterConfig);
            if (configCenterConfig.isValid()) {
                configManager.addConfigCenter(configCenterConfig);
                configCenters = configManager.getConfigCenters();
            }
        } else {
            for (ConfigCenterConfig configCenterConfig : configCenters) {
                configCenterConfig.refresh();
                ConfigValidationUtils.validateConfigCenterConfig(configCenterConfig);
            }
        }

        if (CollectionUtils.isNotEmpty(configCenters)) {
            CompositeDynamicConfiguration compositeDynamicConfiguration = new CompositeDynamicConfiguration();
            for (ConfigCenterConfig configCenter : configCenters) {
                // Pass config from ConfigCenterBean to environment
                environment.updateExternalConfigMap(configCenter.getExternalConfiguration());
                environment.updateAppExternalConfigMap(configCenter.getAppExternalConfiguration());

                // Fetch config from remote config center
                compositeDynamicConfiguration.addConfiguration(prepareEnvironment(configCenter, applicationDeployer));
            }
            environment.setDynamicConfiguration(compositeDynamicConfiguration);
        }
    }

    /**
     * For compatibility purpose, use registry as the default config center when
     * there's no config center specified explicitly and
     * useAsConfigCenter of registryConfig is null or true
     */
    private void useRegistryAsConfigCenterIfNecessary(DefaultApplicationDeployer applicationDeployer) {
        Environment environment = applicationDeployer.getEnvironment();
        ConfigManager configManager = applicationDeployer.getConfigManager();

        // we use the loading status of DynamicConfiguration to decide whether ConfigCenter has been initiated.
        if (environment.getDynamicConfiguration().isPresent()) {
            return;
        }

        if (CollectionUtils.isNotEmpty(configManager.getConfigCenters())) {
            return;
        }

        // load registry
        configManager.loadConfigsOfTypeFromProps(RegistryConfig.class);

        List<RegistryConfig> defaultRegistries = configManager.getDefaultRegistries();
        if (defaultRegistries.size() > 0) {
            defaultRegistries
                .stream()
                .filter(registryConfig -> isUsedRegistryAsConfigCenter(registryConfig, applicationDeployer))
                .map(registryConfig -> registryAsConfigCenter(registryConfig, applicationDeployer))
                .forEach(configCenter -> {
                    if (configManager.getConfigCenter(configCenter.getId()).isPresent()) {
                        return;
                    }
                    configManager.addConfigCenter(configCenter);
                    logger.info("use registry as config-center: " + configCenter);

                });
        }
    }

    private ConfigCenterConfig registryAsConfigCenter(RegistryConfig registryConfig, DefaultApplicationDeployer applicationDeployer) {
        String protocol = registryConfig.getProtocol();
        Integer port = registryConfig.getPort();
        URL url = URL.valueOf(registryConfig.getAddress(), registryConfig.getScopeModel());
        String id = "config-center-" + protocol + "-" + url.getHost() + "-" + port;
        ConfigCenterConfig cc = new ConfigCenterConfig();
        cc.setId(id);
        cc.setScopeModel(applicationDeployer.getApplicationModel());
        if (cc.getParameters() == null) {
            cc.setParameters(new HashMap<>());
        }
        if (CollectionUtils.isNotEmptyMap(registryConfig.getParameters())) {
            cc.getParameters().putAll(registryConfig.getParameters()); // copy the parameters
        }
        cc.getParameters().put(Constants.CLIENT_KEY, registryConfig.getClient());
        cc.setProtocol(protocol);
        cc.setPort(port);
        if (StringUtils.isNotEmpty(registryConfig.getGroup())) {
            cc.setGroup(registryConfig.getGroup());
        }
        cc.setAddress(getRegistryCompatibleAddress(registryConfig));
        cc.setNamespace(registryConfig.getGroup());
        cc.setUsername(registryConfig.getUsername());
        cc.setPassword(registryConfig.getPassword());
        if (registryConfig.getTimeout() != null) {
            cc.setTimeout(registryConfig.getTimeout().longValue());
        }
        cc.setHighestPriority(false);
        return cc;
    }

    private String getRegistryCompatibleAddress(RegistryConfig registryConfig) {
        String registryAddress = registryConfig.getAddress();
        String[] addresses = REGISTRY_SPLIT_PATTERN.split(registryAddress);
        if (ArrayUtils.isEmpty(addresses)) {
            throw new IllegalStateException("Invalid registry address found.");
        }
        String address = addresses[0];
        // since 2.7.8
        // Issue : https://github.com/apache/dubbo/issues/6476
        StringBuilder metadataAddressBuilder = new StringBuilder();
        URL url = URL.valueOf(address, registryConfig.getScopeModel());
        String protocolFromAddress = url.getProtocol();
        if (isEmpty(protocolFromAddress)) {
            // If the protocol from address is missing, is like :
            // "dubbo.registry.address = 127.0.0.1:2181"
            String protocolFromConfig = registryConfig.getProtocol();
            metadataAddressBuilder.append(protocolFromConfig).append("://");
        }
        metadataAddressBuilder.append(address);
        return metadataAddressBuilder.toString();
    }

    private DynamicConfiguration prepareEnvironment(ConfigCenterConfig configCenter, DefaultApplicationDeployer applicationDeployer) {
        if (configCenter.isValid()) {
            if (!configCenter.checkOrUpdateInitialized(true)) {
                return null;
            }

            DynamicConfiguration dynamicConfiguration;
            try {
                dynamicConfiguration = getDynamicConfiguration(configCenter.toUrl(), applicationDeployer);
            } catch (Exception e) {
                if (!configCenter.isCheck()) {
                    logger.warn(CONFIG_FAILED_INIT_CONFIG_CENTER, "", "", "The configuration center failed to initialize", e);
                    configCenter.setInitialized(false);
                    return null;
                } else {
                    throw new IllegalStateException(e);
                }
            }

            if (StringUtils.isNotEmpty(configCenter.getConfigFile())) {
                String configContent = dynamicConfiguration.getProperties(configCenter.getConfigFile(), configCenter.getGroup());
                String appGroup = applicationDeployer.getApplication().getName();
                String appConfigContent = null;
                if (isNotEmpty(appGroup)) {
                    appConfigContent = dynamicConfiguration.getProperties
                        (isNotEmpty(configCenter.getAppConfigFile()) ? configCenter.getAppConfigFile() : configCenter.getConfigFile(),
                            appGroup
                        );
                }
                try {
                    applicationDeployer.getEnvironment().updateExternalConfigMap(parseProperties(configContent));
                    applicationDeployer.getEnvironment().updateAppExternalConfigMap(parseProperties(appConfigContent));
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to parse configurations from Config Center.", e);
                }
            }
            return dynamicConfiguration;
        }
        return null;
    }

    /**
     * Get the instance of {@link DynamicConfiguration} by the specified connection {@link URL} of config-center
     *
     * @param connectionURL of config-center
     * @return non-null
     * @since 2.7.5
     */
    private DynamicConfiguration getDynamicConfiguration(URL connectionURL, DefaultApplicationDeployer applicationDeployer) {
        String protocol = connectionURL.getProtocol();

        DynamicConfigurationFactory factory = ConfigurationUtils.getDynamicConfigurationFactory(applicationDeployer.getApplicationModel(), protocol);
        return factory.getDynamicConfiguration(connectionURL);
    }


    private boolean isUsedRegistryAsConfigCenter(RegistryConfig registryConfig, DefaultApplicationDeployer applicationDeployer) {
        return isUsedRegistryAsCenter(registryConfig, registryConfig::getUseAsConfigCenter, "config",
            DynamicConfigurationFactory.class, applicationDeployer);
    }

    /**
     * Is used the specified registry as a center infrastructure
     *
     * @param registryConfig       the {@link RegistryConfig}
     * @param usedRegistryAsCenter the configured value on
     * @param centerType           the type name of center
     * @param extensionClass       an extension class of a center infrastructure
     * @return
     * @since 2.7.8
     */
    private boolean isUsedRegistryAsCenter(RegistryConfig registryConfig, Supplier<Boolean> usedRegistryAsCenter,
                                           String centerType,
                                           Class<?> extensionClass, DefaultApplicationDeployer applicationDeployer) {
        final boolean supported;

        Boolean configuredValue = usedRegistryAsCenter.get();
        if (configuredValue != null) { // If configured, take its value.
            supported = configuredValue.booleanValue();
        } else {                       // Or check the extension existence
            String protocol = registryConfig.getProtocol();
            supported = supportsExtension(extensionClass, protocol, applicationDeployer);
            if (logger.isInfoEnabled()) {
                logger.info(format("No value is configured in the registry, the %s extension[name : %s] %s as the %s center"
                    , extensionClass.getSimpleName(), protocol, supported ? "supports" : "does not support", centerType));
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info(format("The registry[%s] will be %s as the %s center", registryConfig,
                supported ? "used" : "not used", centerType));
        }
        return supported;
    }

    /**
     * Supports the extension with the specified class and name
     *
     * @param extensionClass the {@link Class} of extension
     * @param name           the name of extension
     * @return if supports, return <code>true</code>, or <code>false</code>
     * @since 2.7.8
     */
    private boolean supportsExtension(Class<?> extensionClass, String name, DefaultApplicationDeployer applicationDeployer) {
        if (isNotEmpty(name)) {
            ExtensionLoader<?> extensionLoader = applicationDeployer.getExtensionLoader(extensionClass);
            return extensionLoader.hasExtension(name);
        }
        return false;
    }


}
