package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.*;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.demo.DemoService;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.service.GenericService;

public class ConsumerApp {

    public static void main(String[] args) {
        if (isClassic(args)) {
            runWithRefer();
        } else {
            runWithBootstrap();
        }
    }

    private static boolean isClassic(String[] args) {
        return args.length > 0 && "classic".equalsIgnoreCase(args[0]);
    }

    private static void runWithBootstrap() {
        ReferenceConfig<DemoService> reference = new ReferenceConfig<>();
        reference.setInterface(DemoService.class);
        reference.setGeneric("true");

        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap.application(new ApplicationConfig("dubbo-demo-api-consumer"))
            .registry(new RegistryConfig("nacos://10.21.23.91:8848?namespace=DUBBO-TEST&username=nacos&password=nacos"))
            .protocol(new ProtocolConfig(CommonConstants.DUBBO, -1))
            .reference(reference)
            .start();

        DemoService demoService = bootstrap.getCache().get(reference);
        String message = demoService.sayHello("dubbo");
        System.out.println(message);

        // generic invoke
        GenericService genericService = (GenericService) demoService;
        Object genericInvokeResult = genericService.$invoke("sayHello", new String[]{String.class.getName()},
            new Object[]{"dubbo generic invoke"});
        System.out.println(genericInvokeResult);
    }


    private static void runWithRefer() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        applicationModel.setModelName("dubbo-demo-api-consumer");

        ReferenceConfig<DemoService> reference = new ReferenceConfig<>();
        reference.setScopeModel(applicationModel);
        reference.setRegistry(new RegistryConfig("zookeeper://127.0.0.1:2181"));

        ConfigManager configManager = applicationModel.getApplicationConfigManager();
        configManager.addMetadataReport(new MetadataReportConfig("zookeeper://127.0.0.1:2181"));

        reference.setInterface(DemoService.class);
        DemoService service = reference.get();
        String message = service.sayHello("dubbo");
        System.out.println(message);
    }

}
