package org.apache.dubbo.config.deploy;

import org.apache.dubbo.common.deploy.PackageLifeManager;

import java.util.List;

/**
 * The initializer that used to init metrics module.
 */
public class MetricsPackageLifeManager implements PackageLifeManager {

    private final static String NAME = MetricsPackageLifeManager.class.getName();


    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<String> dependOnInit() {
        return null;
    }

    /**
     * Do initialize.
     *
     * @param defaultApplicationDeployer The ApplicationDeployer that called this PackageLifeManager.
     */
    @Override
    public void init(Object defaultApplicationDeployer) {

    }
}
