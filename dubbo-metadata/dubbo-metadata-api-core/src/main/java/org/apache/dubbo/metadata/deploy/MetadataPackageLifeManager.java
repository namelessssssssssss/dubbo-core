package org.apache.dubbo.metadata.deploy;

import jdk.nashorn.internal.runtime.regexp.joni.Config;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.common.deploy.PackageLifeManager;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.metadata.report.MetadataReportFactory;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.utils.StringUtils.isEmpty;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;

public class MetadataPackageLifeManager implements PackageLifeManager {

    public static final String NAME = "MetadataPackageLifeManager";

    public static final Logger logger = LoggerFactory.getLogger(MetadataPackageLifeManager.class);

    /**
     * Returns the name of this initializer.
     *
     * @return the name of this initializer. Can not be null.
     */
    @Override
    public String name() {
        return NAME;
    }

    /**
     * Specifies which PackageLifeManager should be called before this one when {@link DefaultApplicationDeployer#initialize()} called. They are guaranteed to be called before this one.
     * <br>
     * Note that if there are cyclic dependencies between PackageLifeManagers, an {@link IllegalStateException} will be thrown.
     *
     * @return PackageLifeManager names. Can be null or empty list.
     */
    @Override
    public List<String> dependOnInit() {
        return Arrays.asList("");
    }

    /**
     * Initialize. This method will be called when {@link DefaultApplicationDeployer#initialize()} called.
     *
     * @param applicationDeployer The ApplicationDeployer that called this PackageLifeManager.
     */
    @Override
    public void initialize(DefaultApplicationDeployer applicationDeployer) {
        startMetadataCenter(applicationDeployer);
    }

    private void startMetadataCenter(DefaultApplicationDeployer applicationDeployer) {

        useRegistryAsMetadataCenterIfNecessary(applicationDeployer);

        ApplicationConfig applicationConfig = applicationDeployer.getApplication();
        ConfigManager configManager = applicationDeployer.getConfigManager();

        String metadataType = applicationConfig.getMetadataType();
        // FIXME, multiple metadata config support.
        Collection<MetadataReportConfig> metadataReportConfigs = configManager.getMetadataConfigs();
        if (CollectionUtils.isEmpty(metadataReportConfigs)) {
            if (REMOTE_METADATA_STORAGE_TYPE.equals(metadataType)) {
                throw new IllegalStateException("No MetadataConfig found, Metadata Center address is required when 'metadata=remote' is enabled.");
            }
            return;
        }

        MetadataReportInstance metadataReportInstance = applicationDeployer.getApplicationModel().getBeanFactory().getBean(MetadataReportInstance.class);
        List<MetadataReportConfig> validMetadataReportConfigs = new ArrayList<>(metadataReportConfigs.size());
        for (MetadataReportConfig metadataReportConfig : metadataReportConfigs) {
            if (ConfigValidationUtils.isValidMetadataConfig(metadataReportConfig)) {
                ConfigValidationUtils.validateMetadataConfig(metadataReportConfig);
                validMetadataReportConfigs.add(metadataReportConfig);
            }
        }
        metadataReportInstance.init(validMetadataReportConfigs);
        if (!metadataReportInstance.inited()) {
            throw new IllegalStateException(String.format("%s MetadataConfigs found, but none of them is valid.", metadataReportConfigs.size()));
        }
    }

    private void useRegistryAsMetadataCenterIfNecessary(DefaultApplicationDeployer applicationDeployer) {

        ConfigManager configManager = applicationDeployer.getConfigManager();

        Collection<MetadataReportConfig> originMetadataConfigs = configManager.getMetadataConfigs();

        if (originMetadataConfigs.stream().anyMatch(m -> Objects.nonNull(m.getAddress()))) {
            return;
        }

        Collection<MetadataReportConfig> metadataConfigsToOverride = originMetadataConfigs
            .stream()
            .filter(m -> Objects.isNull(m.getAddress()))
            .collect(Collectors.toList());

        if (metadataConfigsToOverride.size() > 1) {
            return;
        }

        MetadataReportConfig metadataConfigToOverride = metadataConfigsToOverride.stream().findFirst().orElse(null);

        List<RegistryConfig> defaultRegistries = configManager.getDefaultRegistries();
        if (!defaultRegistries.isEmpty()) {
            defaultRegistries
                .stream()
                .filter(this::isUsedRegistryAsMetadataCenter)
                .map(registryConfig -> registryAsMetadataCenter(applicationDeployer,registryConfig, metadataConfigToOverride))
                .forEach(metadataReportConfig -> {
                    overrideMetadataReportConfig(applicationDeployer,metadataConfigToOverride, metadataReportConfig);
                });
        }
    }

    private MetadataReportConfig registryAsMetadataCenter(DefaultApplicationDeployer applicationDeployer,RegistryConfig registryConfig, MetadataReportConfig originMetadataReportConfig) {
        ApplicationModel applicationModel = applicationDeployer.getApplicationModel();

        MetadataReportConfig metadataReportConfig = originMetadataReportConfig == null ?
            new MetadataReportConfig(registryConfig.getApplicationModel()) : originMetadataReportConfig;

        if (metadataReportConfig.getId() == null) {
            metadataReportConfig.setId(registryConfig.getId());
        }
        metadataReportConfig.setScopeModel(applicationModel);
        if (metadataReportConfig.getParameters() == null) {
            metadataReportConfig.setParameters(new HashMap<>());
        }
        if (CollectionUtils.isNotEmptyMap(registryConfig.getParameters())) {
            for (Map.Entry<String, String> entry : registryConfig.getParameters().entrySet()) {
                metadataReportConfig.getParameters().putIfAbsent(entry.getKey(), entry.getValue()); // copy the parameters
            }
        }
        metadataReportConfig.getParameters().put(Constants.CLIENT_KEY, registryConfig.getClient());
        if (metadataReportConfig.getGroup() == null) {
            metadataReportConfig.setGroup(registryConfig.getGroup());
        }
        if (metadataReportConfig.getAddress() == null) {
            metadataReportConfig.setAddress(getRegistryCompatibleAddress(registryConfig));
        }
        if (metadataReportConfig.getUsername() == null) {
            metadataReportConfig.setUsername(registryConfig.getUsername());
        }
        if (metadataReportConfig.getPassword() == null) {
            metadataReportConfig.setPassword(registryConfig.getPassword());
        }
        if (metadataReportConfig.getTimeout() == null) {
            metadataReportConfig.setTimeout(registryConfig.getTimeout());
        }
        return metadataReportConfig;
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
    private void overrideMetadataReportConfig(DefaultApplicationDeployer applicationDeployer,MetadataReportConfig metadataConfigToOverride, MetadataReportConfig metadataReportConfig) {

        ConfigManager configManager = applicationDeployer.getConfigManager();

        if (metadataReportConfig.getId() == null) {

            Collection<MetadataReportConfig> metadataReportConfigs = configManager.getMetadataConfigs();

            if (CollectionUtils.isNotEmpty(metadataReportConfigs)) {

                for (MetadataReportConfig existedConfig : metadataReportConfigs) {
                    if (existedConfig.getId() == null && existedConfig.getAddress().equals(metadataReportConfig.getAddress())) {
                        return;
                    }
                }
            }

            configManager.removeConfig(metadataConfigToOverride);
            configManager.addMetadataReport(metadataReportConfig);

        } else {
            Optional<MetadataReportConfig> configOptional = configManager.getConfig(MetadataReportConfig.class, metadataReportConfig.getId());

            if (configOptional.isPresent()) {
                return;
            }

            configManager.removeConfig(metadataConfigToOverride);
            configManager.addMetadataReport(metadataReportConfig);
        }
        logger.info("use registry as metadata-center: " + metadataReportConfig);
    }

    private boolean isUsedRegistryAsMetadataCenter(DefaultApplicationDeployer defaultApplicationDeployer,RegistryConfig registryConfig) {
        return isUsedRegistryAsCenter(
            defaultApplicationDeployer,registryConfig, registryConfig::getUseAsMetadataCenter, "metadata", MetadataReportFactory.class
        );
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
    private boolean isUsedRegistryAsCenter(DefaultApplicationDeployer applicationDeployer,
                                           RegistryConfig registryConfig, Supplier<Boolean> usedRegistryAsCenter,
                                           String centerType,
                                           Class<?> extensionClass) {
        final boolean supported;

        Boolean configuredValue = usedRegistryAsCenter.get();

        if (configuredValue != null) { // If configured, take its value.
            supported = configuredValue.booleanValue();
        } else {
            // Or check the extension existence
            String protocol = registryConfig.getProtocol();
            supported = supportsExtension(applicationDeployer,extensionClass, protocol);

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
    private boolean supportsExtension(DefaultApplicationDeployer applicationDeployer,Class<?> extensionClass, String name) {
        if (isNotEmpty(name)) {
            ExtensionLoader<?> extensionLoader = applicationDeployer.getApplicationModel().getExtensionLoader(extensionClass);
            return extensionLoader.hasExtension(name);
        }
        return false;
    }



}
