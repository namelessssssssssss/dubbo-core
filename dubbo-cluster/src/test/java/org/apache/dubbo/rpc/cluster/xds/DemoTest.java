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
package org.apache.dubbo.rpc.cluster.xds;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.xds.istio.IstioEnv;

import javax.net.ssl.SSLException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.MetadataUtils;
import istio.v1.auth.IstioCertificateRequest;
import istio.v1.auth.IstioCertificateResponse;
import istio.v1.auth.IstioCertificateServiceGrpc;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

public class DemoTest {

    private Protocol protocol =
            ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    private ProxyFactory proxy =
            ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    @Test
    public void testXdsRouterInitial() throws InterruptedException, SSLException {
        PilotExchanger pilotExchanger = PilotExchanger.initialize(URL.valueOf("xds://localhost:15012/?secure=istio"));

        new CountDownLatch(1).await();

        IstioEnv istioEnv = IstioEnv.getInstance();

        Metadata header = new Metadata();
        Metadata.Key<String> key = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        header.put(key, "Bearer " + istioEnv.getServiceAccount());
        key = Metadata.Key.of("ClusterID", Metadata.ASCII_STRING_MARSHALLER);
        header.put(key, istioEnv.getIstioMetaClusterId());

        String caCert = istioEnv.getCaCert();
        ManagedChannel channel = NettyChannelBuilder.forTarget("localhost:15012")
                .sslContext(GrpcSslContexts.forClient()
                        .trustManager(new ByteArrayInputStream(caCert.getBytes(StandardCharsets.UTF_8)))
                        .build())
                .build();

        IstioCertificateServiceGrpc.IstioCertificateServiceStub stub = IstioCertificateServiceGrpc.newStub(channel);
        stub = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(header));

        stub.createCertificate(
                IstioCertificateRequest.newBuilder().build(),
                new ClientResponseObserver<IstioCertificateRequest, IstioCertificateResponse>() {
                    @Override
                    public void onNext(IstioCertificateResponse istioCertificateResponse) {
                        System.out.println(istioCertificateResponse);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        throwable.printStackTrace();
                    }

                    @Override
                    public void onCompleted() {}

                    @Override
                    public void beforeStart(ClientCallStreamObserver clientCallStreamObserver) {}
                });
        new CountDownLatch(1).await();

        //        new CountDownLatch(1).await();

        //         Node node = Node.newBuilder().build();
        //         AdsClient adsClient = new AdsClient(url,node);
        //
        //        ManagedChannel channel = adsClient.getXdsChannel()
        //                .getChannel();
        //
        //        SdsProtocol sdsProtocol = new SdsProtocol(ApplicationModel.defaultModel(),
        // adsClient,Node.newBuilder().build(),20);

        //        StreamObserver<DiscoveryRequest> request = SecretDiscoveryServiceGrpc.newStub(channel)
        //                .streamSecrets(new StreamObserver<DiscoveryResponse>() {
        //
        //                    @Override
        //                    public void onNext(DiscoveryResponse discoveryResponse) {
        //                        sdsProtocol.onUpdate(discoveryResponse);
        //                    }
        //
        //                    @Override
        //                    public void onError(Throwable throwable) {
        //                        throwable.printStackTrace();
        //                    }
        //
        //                    @Override
        //                    public void onCompleted() {
        //                        System.out.println("completed");
        //                    }
        //                });
        //        request.onNext(DiscoveryRequest.newBuilder().build());

        //
        //        PilotExchanger pilotExchanger = PilotExchanger.initialize(url);
        //
        //        Directory directory = Mockito.spy(Directory.class);
        //
        //
        //
        //        when(directory.getConsumerUrl())
        //
        // .thenReturn(URL.valueOf("dubbo://0.0.0.0:15010/DemoService?providedBy=dubbo-samples-xds-provider"));
        //         when(directory.getInterface()).thenReturn(DemoService.class);
        //         when(directory.getProtocol()).thenReturn(protocol);
        //         SingleRouterChain singleRouterChain =
        //                 new SingleRouterChain<>(null, Arrays.asList(new XdsRouter<>(url)), false, null);
        //         RouterChain routerChain = new RouterChain<>(new SingleRouterChain[] {singleRouterChain});
        //         when(directory.getRouterChain()).thenReturn(routerChain);
        //
        //         XdsDirectory xdsDirectory = new XdsDirectory(directory);
        //
        //         Invocation invocation = Mockito.mock(Invocation.class);
        //         Invoker invoker = Mockito.mock(Invoker.class);
        //         URL url1 = URL.valueOf("consumer://0.0.0.0:15010/DemoService?providedBy=dubbo-samples-xds-provider");
        //         when(invoker.getUrl()).thenReturn(url1);
        //         when(invocation.getInvoker()).thenReturn(invoker);
        //
        //         while (true) {
        //             Map<String, XdsVirtualHost> xdsVirtualHostMap = xdsDirectory.getXdsVirtualHostMap();
        //             Map<String, XdsCluster> xdsClusterMap = xdsDirectory.getXdsClusterMap();
        //             if (!xdsVirtualHostMap.isEmpty() && !xdsClusterMap.isEmpty()) {
        //                 // xdsRouterDemo.route(invokers, url, invocation, false, null);
        //                 xdsDirectory.list(invocation);
        //             }
        //             Thread.yield();
        //         }
    }

    private Invoker<Object> createInvoker(String app, String address) {
        URL url = URL.valueOf("dubbo://" + address + "/DemoInterface?"
                + (StringUtils.isEmpty(app) ? "" : "remote.application=" + app));
        Invoker invoker = Mockito.mock(Invoker.class);
        when(invoker.getUrl()).thenReturn(url);
        return invoker;
    }
}
