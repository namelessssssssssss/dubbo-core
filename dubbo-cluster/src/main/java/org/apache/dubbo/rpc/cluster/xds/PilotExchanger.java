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
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.rpc.cluster.directory.XdsDirectory;
import org.apache.dubbo.rpc.cluster.xds.protocol.impl.CdsProtocol;
import org.apache.dubbo.rpc.cluster.xds.protocol.impl.EdsProtocol;
import org.apache.dubbo.rpc.cluster.xds.protocol.impl.LdsProtocol;
import org.apache.dubbo.rpc.cluster.xds.protocol.impl.RdsProtocol;
import org.apache.dubbo.rpc.cluster.xds.resource.XdsCluster;
import org.apache.dubbo.rpc.cluster.xds.resource.XdsRouteConfiguration;
import org.apache.dubbo.rpc.cluster.xds.resource.XdsVirtualHost;
import org.apache.dubbo.rpc.cluster.xds.security.api.MeshCredentialProvider;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class PilotExchanger {

    // protected final XdsChannel xdsChannel;

    protected final AdsClient adsClient;

    protected final LdsProtocol ldsProtocol;

    protected final RdsProtocol rdsProtocol;

    protected final EdsProtocol edsProtocol;

    protected final CdsProtocol cdsProtocol;

    private final Set<String> domainObserveRequest = new ConcurrentHashSet<String>();

    private static PilotExchanger GLOBAL_PILOT_EXCHANGER = null;

    private final ApplicationModel applicationModel;

    private static final Map<String, XdsVirtualHost> xdsVirtualHostMap = new ConcurrentHashMap<>();

    private static final Map<String, XdsCluster> xdsClusterMap = new ConcurrentHashMap<>();

    private final Map<String, Set<XdsDirectory>> rdsListeners = new ConcurrentHashMap<>();

    private final Map<String, Set<XdsDirectory>> cdsListeners = new ConcurrentHashMap<>();

    protected PilotExchanger(URL url) {
        // xdsChannel = new XdsChannel(url);
        int pollingTimeout = url.getParameter("pollingTimeout", 10);
        this.applicationModel = url.getOrDefaultApplicationModel();

        MeshCredentialProvider credentialProvider =
                applicationModel.getBeanFactory().getOrRegisterBean(MeshCredentialProvider.class);
        credentialProvider.setUrl(url);

        adsClient = new AdsClient(url, NodeBuilder.build());

        // rds 资源回调函数，将 RdsProtocol 的资源存放起来
        Consumer<List<XdsRouteConfiguration>> rdsCallback = (xdsRouteConfigurations) -> {
            xdsRouteConfigurations.forEach(xdsRouteConfiguration -> {
                xdsRouteConfiguration.getVirtualHosts().forEach((serviceName, xdsVirtualHost) -> {
                    this.xdsVirtualHostMap.put(serviceName, xdsVirtualHost);
                    // 回调更新
                    if (rdsListeners.containsKey(serviceName)) {
                        for (XdsDirectory listener : rdsListeners.get(serviceName)) {
                            listener.onRdsChange(serviceName, xdsVirtualHost);
                        }
                    }
                });
            });
        };

        // eds 资源回调函数
        Consumer<List<XdsCluster>> edsCallback = (xdsClusters) -> {
            xdsClusters.forEach(xdsCluster -> {
                this.xdsClusterMap.put(xdsCluster.getName(), xdsCluster);
                if (cdsListeners.containsKey(xdsCluster.getName())) {
                    for (XdsDirectory listener : cdsListeners.get(xdsCluster.getName())) {
                        listener.onCdsChange(xdsCluster.getName(), xdsCluster);
                    }
                }
            });
        };
        this.rdsProtocol = new RdsProtocol(adsClient, NodeBuilder.build(), pollingTimeout, rdsCallback);
        this.edsProtocol = new EdsProtocol(adsClient, NodeBuilder.build(), pollingTimeout, edsCallback);

        this.ldsProtocol = new LdsProtocol(adsClient, NodeBuilder.build(), pollingTimeout);
        this.cdsProtocol = new CdsProtocol(adsClient, NodeBuilder.build(), pollingTimeout);
        // lds 回调函数，在回调函数中监听所有的 rds 资源
        Consumer<Set<String>> ldsCallback = rdsProtocol::subscribeResource;
        ldsProtocol.setUpdateCallback(ldsCallback);
        ldsProtocol.subscribeListeners();

        // cds 回调函数，在回调函数中监听所有的 eds 资源
        Consumer<Set<String>> cdsCallback = edsProtocol::subscribeResource;
        cdsProtocol.setUpdateCallback(cdsCallback);
        cdsProtocol.subscribeClusters();
    }

    public static Map<String, XdsVirtualHost> getXdsVirtualHostMap() {
        return xdsVirtualHostMap;
    }

    public static Map<String, XdsCluster> getXdsClusterMap() {
        return xdsClusterMap;
    }

    public void subscribeRds(String applicationName, XdsDirectory listener) {
        rdsListeners.computeIfAbsent(applicationName, key -> new ConcurrentHashSet<>());
        rdsListeners.get(applicationName).add(listener);
        if (xdsVirtualHostMap.containsKey(applicationName)) {
            listener.onRdsChange(applicationName, this.xdsVirtualHostMap.get(applicationName));
        }
    }

    public void unSubscribeRds(String applicationName, XdsDirectory listener) {
        rdsListeners.get(applicationName).remove(listener);
    }

    public void subscribeCds(String clusterName, XdsDirectory listener) {
        cdsListeners.computeIfAbsent(clusterName, key -> new ConcurrentHashSet<>());
        cdsListeners.get(clusterName).add(listener);
        if (xdsClusterMap.containsKey(clusterName)) {
            listener.onCdsChange(clusterName, xdsClusterMap.get(clusterName));
        }
    }

    public void unSubscribeCds(String clusterName, XdsDirectory listener) {
        cdsListeners.get(clusterName).remove(listener);
    }

    public static PilotExchanger initialize(URL url) {
        synchronized (PilotExchanger.class) {
            if (GLOBAL_PILOT_EXCHANGER != null) {
                return GLOBAL_PILOT_EXCHANGER;
            }
            return (GLOBAL_PILOT_EXCHANGER = new PilotExchanger(url));
        }
    }

    public static PilotExchanger getInstance() {
        synchronized (PilotExchanger.class) {
            return GLOBAL_PILOT_EXCHANGER;
        }
    }

    public static boolean isEnabled() {
        return GLOBAL_PILOT_EXCHANGER != null;
    }

    public void destroy() {
        // xdsChannel.destroy();
        this.adsClient.destroy();
    }
}
