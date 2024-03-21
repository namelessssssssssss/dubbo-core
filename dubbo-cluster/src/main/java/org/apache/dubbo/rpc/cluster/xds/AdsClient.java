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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.rpc.cluster.xds.protocol.AbstractProtocol;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.stub.StreamObserver;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_REQUEST_XDS;

public class AdsClient {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AdsClient.class);
    private final ApplicationModel applicationModel;
    private final URL url;
    private final Node node;
    private volatile XdsChannel xdsChannel;

    private final Map<String, XdsListener> listeners = new ConcurrentHashMap<>();

    protected StreamObserver<DiscoveryRequest> requestObserver;

    private final Map<String, DiscoveryRequest> observedResources = new ConcurrentHashMap<>();

    public AdsClient(URL url, Node node) {
        this.url = url;
        this.node = node;
        this.xdsChannel = new XdsChannel(url);
        this.applicationModel = url.getOrDefaultApplicationModel();
    }

    public <T> void addListener(AbstractProtocol<T> protocol) {
        listeners.put(protocol.getTypeUrl(), protocol);
    }

    public void request(DiscoveryRequest discoveryRequest) {
        if (requestObserver == null) {
            requestObserver = xdsChannel.createDeltaDiscoveryRequest(new ResponseObserver());
        }
        requestObserver.onNext(discoveryRequest);
        observedResources.put(discoveryRequest.getTypeUrl(), discoveryRequest);
    }

    private class ResponseObserver implements StreamObserver<DiscoveryResponse> {

        private String lastError;
        private int lastErrorCode;

        @Override
        public void onNext(DiscoveryResponse discoveryResponse) {
            // 当 server 回复时，调用不同类型的协议监听器进行解析
            System.out.println("Receive message from server");
            XdsListener xdsListener = listeners.get(discoveryResponse.getTypeUrl());
            xdsListener.process(discoveryResponse);
            requestObserver.onNext(buildAck(discoveryResponse));
        }

        protected DiscoveryRequest buildAck(DiscoveryResponse response) {
            // for ACK
            return DiscoveryRequest.newBuilder()
                    .setNode(node)
                    .setTypeUrl(response.getTypeUrl())
                    .setVersionInfo(response.getVersionInfo())
                    .setResponseNonce(response.getNonce())
                    .addAllResourceNames(
                            observedResources.get(response.getTypeUrl()).getResourceNamesList())
                    .build();
        }

        @Override
        public void onError(Throwable throwable) {
            logger.error(REGISTRY_ERROR_REQUEST_XDS, "", "", "xDS Client received error message! detail:", throwable);
            triggerReConnectTask();
        }

        @Override
        public void onCompleted() {
            logger.info("xDS Client completed");
            triggerReConnectTask();
        }
    }

    private void triggerReConnectTask() {
        ScheduledExecutorService scheduledFuture = applicationModel
                .getFrameworkModel()
                .getBeanFactory()
                .getBean(FrameworkExecutorRepository.class)
                .getSharedScheduledExecutor();
        scheduledFuture.schedule(this::recover, 3, TimeUnit.SECONDS);
    }

    private void recover() {
        try {
            xdsChannel = new XdsChannel(url);
            if (xdsChannel.getChannel() != null) {
                requestObserver = xdsChannel.createDeltaDiscoveryRequest(new ResponseObserver());
                observedResources.values().forEach(requestObserver::onNext);
                return;
            } else {
                logger.error(
                        REGISTRY_ERROR_REQUEST_XDS,
                        "",
                        "",
                        "Recover failed for xDS connection. Will retry. Create channel failed.");
            }
        } catch (Exception e) {
            logger.error(REGISTRY_ERROR_REQUEST_XDS, "", "", "Recover failed for xDS connection. Will retry.", e);
        }
        triggerReConnectTask();
    }

    public void destroy() {
        this.xdsChannel.destroy();
    }

    @Deprecated
    public XdsChannel getXdsChannel() {
        return xdsChannel;
    }
}
