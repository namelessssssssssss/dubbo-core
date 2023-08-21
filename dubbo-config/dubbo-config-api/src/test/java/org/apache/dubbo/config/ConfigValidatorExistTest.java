package org.apache.dubbo.config;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.validator.ConsumerConfigValidator;
import org.apache.dubbo.config.validator.InterfaceConfigValidator;
import org.apache.dubbo.config.validator.MethodConfigValidator;
import org.apache.dubbo.config.validator.ProviderConfigValidator;
import org.apache.dubbo.config.validator.ReferenceConfigValidator;
import org.apache.dubbo.config.validator.ServiceConfigValidator;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;


public class ConfigValidatorExistTest {

    ConfigValidateFacade validateFacade;

    @Test
    void testConfigValidatorExist(){
        FrameworkModel frameworkModel = FrameworkModel.defaultModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();

        validateFacade = ConfigValidateFacade.getInstance();
        Assertions.assertNotNull(validateFacade);
        Assertions.assertTrue(validateFacade.getValidators().size() >= 7);

        try(
            MockedStatic<ConsumerConfigValidator> configValidatorMockedStatic = Mockito.mockStatic(ConsumerConfigValidator.class);
            MockedStatic<InterfaceConfigValidator> interfaceConfigValidatorMockedStatic = Mockito.mockStatic(InterfaceConfigValidator.class);
            MockedStatic<MethodConfigValidator> methodConfigValidatorMockedStatic = Mockito.mockStatic(MethodConfigValidator.class);
            MockedStatic<ProviderConfigValidator> providerConfigValidatorMockedStatic = Mockito.mockStatic(ProviderConfigValidator.class);
            MockedStatic<ReferenceConfigValidator> referenceConfigValidatorMockedStatic = Mockito.mockStatic(ReferenceConfigValidator.class);
            MockedStatic<ServiceConfigValidator> serviceConfigValidatorMockedStatic = Mockito.mockStatic(ServiceConfigValidator.class);
        ){
            configValidatorMockedStatic.when(()-> ConsumerConfigValidator.validateConsumerConfig(any())).thenCallRealMethod();
            interfaceConfigValidatorMockedStatic.when(()-> InterfaceConfigValidator.validateAbstractInterfaceConfig(any())).thenCallRealMethod();
            methodConfigValidatorMockedStatic.when(()-> MethodConfigValidator.validateMethodConfig(any())).thenCallRealMethod();
            providerConfigValidatorMockedStatic.when(()-> ProviderConfigValidator.validateProviderConfig(any())).thenCallRealMethod();
            referenceConfigValidatorMockedStatic.when(()-> ReferenceConfigValidator.validateReferenceConfig(any())).thenCallRealMethod();
            serviceConfigValidatorMockedStatic.when(()->ServiceConfigValidator.validateServiceConfig(any())).thenCallRealMethod();
            triggerValidate(new ConsumerConfig());
            triggerValidate(new AbstractInterfaceConfig() {
                @Override
                public List<URL> getExportedUrls() {
                    return super.getExportedUrls();
                }
            });
            triggerValidate(new MethodConfig());
            triggerValidate(new ProviderConfig());
            triggerValidate(new ReferenceConfig<>());
            triggerValidate(new ServiceConfig<>());

            configValidatorMockedStatic.verify(()-> ConsumerConfigValidator.validateConsumerConfig(any()),atLeastOnce());
            interfaceConfigValidatorMockedStatic.verify(()-> InterfaceConfigValidator.validateAbstractInterfaceConfig(any()),atLeastOnce());
            methodConfigValidatorMockedStatic.verify(()->MethodConfigValidator.validateMethodConfig(any()),atLeastOnce());
            providerConfigValidatorMockedStatic.verify(()->ProviderConfigValidator.validateProviderConfig(any()),atLeastOnce());
            referenceConfigValidatorMockedStatic.verify(()->ReferenceConfigValidator.validateReferenceConfig(any()),atLeastOnce());
            serviceConfigValidatorMockedStatic.verify(()-> ServiceConfigValidator.validateServiceConfig(any()),atLeastOnce());
        }
    }

    void triggerValidate(AbstractConfig config){
        try {
            validateFacade.validate(config);
        }catch (Throwable ignored){};
    }

}
