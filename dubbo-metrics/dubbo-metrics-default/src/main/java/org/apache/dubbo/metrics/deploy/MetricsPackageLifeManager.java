package org.apache.dubbo.metrics.deploy;

import org.apache.dubbo.common.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.common.deploy.PackageLifeManager;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.report.MetricsReporter;
import org.apache.dubbo.metrics.report.MetricsReporterFactory;
import org.apache.dubbo.metrics.service.MetricsServiceExporter;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collections;
import java.util.List;

import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;

public class MetricsPackageLifeManager implements PackageLifeManager {

    private static final String NAME = "MetricsPackageLifeManager";

    private MetricsServiceExporter metricsServiceExporter;

    @Override
    public String name() {
        return NAME;
    }

    /**
     * Specifies which PackageLifeManager should be called before this one when {@link DefaultApplicationDeployer#initialize()}. They are guaranteed to be called before this one.
     * <br>
     * Note that if there are cyclic dependencies between initializers, an {@link IllegalStateException} will be thrown.
     *
     * @return PackageLifeManager names. Can be null or empty list.
     */
    @Override
    public List<String> dependOnInit() {
        return Collections.singletonList("ApplicationPackageLifeManager");
    }

    /**
     * Initialize.
     *
     * @param applicationDeployer The ApplicationDeployer that called this PackageLifeManager.
     */
    @Override
    public void initialize(DefaultApplicationDeployer applicationDeployer) {
        initMetricsReporter(applicationDeployer);
        initMetricsService(applicationDeployer);
    }

    private void initMetricsService(DefaultApplicationDeployer applicationDeployer) {
        this.metricsServiceExporter = applicationDeployer.getExtensionLoader(MetricsServiceExporter.class).getDefaultExtension();
        metricsServiceExporter.init();
    }

    private void initMetricsReporter(DefaultApplicationDeployer applicationDeployer) {
        ApplicationModel applicationModel = applicationDeployer.getApplicationModel();
        DefaultMetricsCollector collector = applicationModel.getBeanFactory().getBean(DefaultMetricsCollector.class);
        MetricsConfig metricsConfig = applicationDeployer.getConfigManager().getMetrics().orElse(null);
        // TODO compatible with old usage of metrics, remove protocol check after new metrics is ready for use.
        if (metricsConfig != null && PROTOCOL_PROMETHEUS.equals(metricsConfig.getProtocol())) {
            collector.setCollectEnabled(true);
            collector.collectApplication(applicationModel);
            String protocol = metricsConfig.getProtocol();
            if (StringUtils.isNotEmpty(protocol)) {
                MetricsReporterFactory metricsReporterFactory = applicationDeployer.getExtensionLoader(MetricsReporterFactory.class).getAdaptiveExtension();
                MetricsReporter metricsReporter = metricsReporterFactory.createMetricsReporter(metricsConfig.toUrl());
                metricsReporter.init();
                applicationModel.getBeanFactory().registerBean(metricsReporter);
            }
        }
    }


    /**
     * Specifies which PackageLifeManager should be called before this one when {@link DefaultApplicationDeployer#preDestroy()}.
     * <br>
     * Works just like {@link PackageLifeManager#dependOnInit()}.
     *
     * @return PackageLifeManager names. Can be null or empty list.
     */
    @Override
    public List<String> dependOnPreDestroy() {
        return null;
    }

    /**
     * preDestroy.
     *
     * @param applicationDeployer The ApplicationDeployer that called this PackageLifeManager.
     */
    @Override
    public void preDestroy(DefaultApplicationDeployer applicationDeployer) {

    }

    /**
     * Specifies which PackageLifeManager should be called before this one when {@link DefaultApplicationDeployer#postDestroy()}}.
     * <br>
     * Works just like {@link PackageLifeManager#dependOnInit()}.
     *
     * @return PackageLifeManager names. Can be null or empty list.
     */
    @Override
    public List<String> dependOnPostDestroy() {
        return null;
    }

    /**
     * postDestroy.
     *
     * @param applicationDeployer The ApplicationDeployer that called this PackageLifeManager.
     */
    @Override
    public void postDestroy(DefaultApplicationDeployer applicationDeployer) {

    }
}
