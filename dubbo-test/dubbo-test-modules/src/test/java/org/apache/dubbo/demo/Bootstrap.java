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
package org.apache.dubbo.demo;

import org.apache.dubbo.common.constants.RegisterTypeEnum;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.demo.api.DemoService;
import org.apache.dubbo.demo.impl.DemoServiceImpl;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

public class Bootstrap {

    public static void main(String[] args) throws InterruptedException {

        FrameworkModel framework1 = new FrameworkModel();
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ConfigManager configManager = applicationModel.getApplicationConfigManager();
        configManager.setSsl(new SslConfig());

        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setProtocol(new ProtocolConfig("tri"));
        serviceConfig.setRegistry(new RegistryConfig("zookeeper://localhost:2181"));
        serviceConfig.setRef(new DemoServiceImpl());
        serviceConfig.setAuth(true);
        serviceConfig.setScopeModel(framework1.newApplication().newModule());
        serviceConfig.export(RegisterTypeEnum.AUTO_REGISTER);

        FrameworkModel framework2 = new FrameworkModel();
        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(DemoService.class);
        referenceConfig.setProtocol("tri");
        referenceConfig.setRegistry(new RegistryConfig("zookeeper://localhost:2181"));
        referenceConfig.setScopeModel(framework2.newApplication().newModule());
        DemoService demoService = referenceConfig.get();


        while (true) {
            demoService.echo("mtls from client");
            Thread.sleep(1000L);
        }
    }
}
