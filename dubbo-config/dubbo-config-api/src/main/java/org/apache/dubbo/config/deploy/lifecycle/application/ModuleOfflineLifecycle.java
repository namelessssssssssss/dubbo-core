package org.apache.dubbo.config.deploy.lifecycle.application;

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.deploy.context.ApplicationContext;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;

import java.util.List;

/**
 * This lifecycle will execute module offline operations when a module pre destroys.
 */
@Activate(order = 1000)
public class ModuleOfflineLifecycle implements ApplicationLifecycle{

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ModuleOfflineLifecycle.class);

    @Override
    public boolean needInitialize(ApplicationContext context) {
        return true;
    }

    @Override
    public void postModuleChanged(ApplicationContext applicationContext, ModuleModel changedModule, DeployState moduleNewState, DeployState applicationOldState, DeployState applicationNewState) {
        if (moduleNewState == DeployState.STOPPING) {
            offline(changedModule);
        }
    }

    private void offline(ModuleModel moduleModel) {
        try {
            ModuleServiceRepository serviceRepository = moduleModel.getServiceRepository();
            List<ProviderModel> exportedServices = serviceRepository.getExportedServices();
            for (ProviderModel exportedService : exportedServices) {
                List<ProviderModel.RegisterStatedURL> statedUrls = exportedService.getStatedUrl();
                for (ProviderModel.RegisterStatedURL statedURL : statedUrls) {
                    if (statedURL.isRegistered()) {
                        doOffline(statedURL);
                    }
                }
            }
        } catch (Throwable t) {
            logger.error(LoggerCodeConstants.INTERNAL_ERROR, "", "", "Exceptions occurred when unregister services.", t);
        }
    }

    private void doOffline(ProviderModel.RegisterStatedURL statedURL) {
        RegistryFactory registryFactory =
            statedURL.getRegistryUrl().getOrDefaultApplicationModel().getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
        Registry registry = registryFactory.getRegistry(statedURL.getRegistryUrl());
        registry.unregister(statedURL.getProviderUrl());
        statedURL.setRegistered(false);
    }
}
