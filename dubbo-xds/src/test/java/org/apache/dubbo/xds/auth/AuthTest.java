package org.apache.dubbo.xds.auth;

import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.xds.istio.IstioConstant;
import org.apache.dubbo.xds.kubernetes.KubeApiClient;
import org.apache.dubbo.xds.kubernetes.KubeEnv;
import org.apache.dubbo.xds.security.authz.KubeRuleSourceProvider;
import org.apache.dubbo.xds.security.authz.RoleBasedAuthorizer;
import org.apache.dubbo.xds.security.authz.rule.DefaultRuleFactory;

import org.junit.Test;

public class AuthTest {


    @Test
    public void authZTest() throws Exception {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        applicationModel.getBeanFactory().registerBean(new KubeEnv());
        applicationModel.getBeanFactory().registerBean(new KubeApiClient(applicationModel));
        KubeRuleSourceProvider provider = new KubeRuleSourceProvider(applicationModel);
        applicationModel.getBeanFactory().registerBean(provider);
        applicationModel.getBeanFactory().registerBean(DefaultRuleFactory.class);
        provider.getSource(null,null);
        RoleBasedAuthorizer authorizer = new RoleBasedAuthorizer(applicationModel);

    }

    @Test
    public void mTlsTest() throws InterruptedException {

        IstioConstant.KUBERNETES_SA_PATH = "/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/token_bar";
        FrameworkModel f1 = new FrameworkModel();
        ServiceConfig<DemoService> serviceConfig =new ServiceConfig<>();
        serviceConfig.setRef(new DemoServiceImpl());
        ProtocolConfig triConf =new ProtocolConfig("tri");
        triConf.setPort(10086);
        triConf.setHost("10.23.151.135");
        serviceConfig.setRegistry(new RegistryConfig("zookeeper://localhost:2181"));
        serviceConfig.setProtocol(triConf);
        serviceConfig.setScopeModel(f1.newApplication().newModule());
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setTimeout(1000000);
        serviceConfig.export();

        IstioConstant.KUBERNETES_SA_PATH = "/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/token_foo";
        FrameworkModel f2 = new FrameworkModel();
        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(DemoService.class);
        referenceConfig.setRegistry(new RegistryConfig("zookeeper://localhost:2181"));
        referenceConfig.setScopeModel(f2.newApplication().newModule());
        referenceConfig.setTimeout(1000000);
        DemoService demoService = referenceConfig.get();

        while (true) {
            System.out.println(demoService.sayHello());
            Thread.sleep(1000L);
        }
    }

}
