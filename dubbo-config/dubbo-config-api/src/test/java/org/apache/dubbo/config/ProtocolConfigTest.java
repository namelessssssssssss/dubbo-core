/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.config;

import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.context.ConfigManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.ProviderConstants.DEFAULT_PREFER_SERIALIZATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProtocolConfigTest {

    @BeforeEach
    public void setUp() {
        DubboBootstrap.reset();
    }

    @AfterEach
    public void afterEach() {
        SysProps.clear();
    }

    @AfterAll
    public static void afterAll() {
        DubboBootstrap.reset();
    }

    @Test
    void testName() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        String protocolName = "xprotocol";
        protocol.setName(protocolName);
        Map<String, String> parameters = new HashMap<String, String>();
        ProtocolConfig.appendParameters(parameters, protocol);
        MatcherAssert.assertThat(protocol.getName(), Matchers.equalTo(protocolName));
        MatcherAssert.assertThat(protocol.getId(), Matchers.equalTo(null));
        MatcherAssert.assertThat(parameters.isEmpty(), Matchers.is(true));
    }

    @Test
    void testHost() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setHost("host");
        Map<String, String> parameters = new HashMap<String, String>();
        ProtocolConfig.appendParameters(parameters, protocol);
        MatcherAssert.assertThat(protocol.getHost(), Matchers.equalTo("host"));
        MatcherAssert.assertThat(parameters.isEmpty(), Matchers.is(true));
    }

    @Test
    void testPort() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        int port = NetUtils.getAvailablePort();
        protocol.setPort(port);
        Map<String, String> parameters = new HashMap<String, String>();
        ProtocolConfig.appendParameters(parameters, protocol);
        MatcherAssert.assertThat(protocol.getPort(), Matchers.equalTo(port));
        MatcherAssert.assertThat(parameters.isEmpty(), Matchers.is(true));
    }

    @Test
    void testPath() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setContextpath("context-path");
        Map<String, String> parameters = new HashMap<String, String>();
        ProtocolConfig.appendParameters(parameters, protocol);
        MatcherAssert.assertThat(protocol.getPath(), Matchers.equalTo("context-path"));
        MatcherAssert.assertThat(protocol.getContextpath(), Matchers.equalTo("context-path"));
        MatcherAssert.assertThat(parameters.isEmpty(), Matchers.is(true));
        protocol.setPath("path");
        MatcherAssert.assertThat(protocol.getPath(), Matchers.equalTo("path"));
        MatcherAssert.assertThat(protocol.getContextpath(), Matchers.equalTo("path"));
    }

    @Test
    void testCorethreads() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setCorethreads(10);
        MatcherAssert.assertThat(protocol.getCorethreads(), Matchers.is(10));
    }

    @Test
    void testThreads() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setThreads(10);
        MatcherAssert.assertThat(protocol.getThreads(), Matchers.is(10));
    }

    @Test
    void testIothreads() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setIothreads(10);
        MatcherAssert.assertThat(protocol.getIothreads(), Matchers.is(10));
    }

    @Test
    void testQueues() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setQueues(10);
        MatcherAssert.assertThat(protocol.getQueues(), Matchers.is(10));
    }

    @Test
    void testAccepts() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setAccepts(10);
        MatcherAssert.assertThat(protocol.getAccepts(), Matchers.is(10));
    }

    @Test
    void testCodec() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName("dubbo");
        protocol.setCodec("mockcodec");
        MatcherAssert.assertThat(protocol.getCodec(), Matchers.equalTo("mockcodec"));
    }

    @Test
    void testAccesslog() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setAccesslog("access.log");
        MatcherAssert.assertThat(protocol.getAccesslog(), Matchers.equalTo("access.log"));
    }

    @Test
    void testTelnet() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setTelnet("mocktelnethandler");
        MatcherAssert.assertThat(protocol.getTelnet(), Matchers.equalTo("mocktelnethandler"));
    }

    @Test
    void testRegister() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setRegister(true);
        MatcherAssert.assertThat(protocol.isRegister(), Matchers.is(true));
    }

    @Test
    void testTransporter() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setTransporter("mocktransporter");
        MatcherAssert.assertThat(protocol.getTransporter(), Matchers.equalTo("mocktransporter"));
    }

    @Test
    void testExchanger() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setExchanger("mockexchanger");
        MatcherAssert.assertThat(protocol.getExchanger(), Matchers.equalTo("mockexchanger"));
    }

    @Test
    void testDispatcher() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setDispatcher("mockdispatcher");
        MatcherAssert.assertThat(protocol.getDispatcher(), Matchers.equalTo("mockdispatcher"));
    }

    @Test
    void testNetworker() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setNetworker("networker");
        MatcherAssert.assertThat(protocol.getNetworker(), Matchers.equalTo("networker"));
    }

    @Test
    void testParameters() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setParameters(Collections.singletonMap("k1", "v1"));
        MatcherAssert.assertThat(protocol.getParameters(), Matchers.hasEntry("k1", "v1"));
    }

    @Test
    void testDefault() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setDefault(true);
        MatcherAssert.assertThat(protocol.isDefault(), Matchers.is(true));
    }

    @Test
    void testKeepAlive() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setKeepAlive(true);
        MatcherAssert.assertThat(protocol.getKeepAlive(), Matchers.is(true));
    }

    @Test
    void testOptimizer() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setOptimizer("optimizer");
        MatcherAssert.assertThat(protocol.getOptimizer(), Matchers.equalTo("optimizer"));
    }

    @Test
    void testExtension() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setExtension("extension");
        MatcherAssert.assertThat(protocol.getExtension(), Matchers.equalTo("extension"));
    }

    @Test
    void testMetaData() {
        ProtocolConfig config = new ProtocolConfig();
        Map<String, String> metaData = config.getMetaData();
        Assertions.assertEquals(0, metaData.size(), "actual: "+metaData);
    }

    @Test
    void testOverrideEmptyConfig() {
        int port = NetUtils.getAvailablePort();
        //dubbo.protocol.name=rest
        //dubbo.protocol.port=port
        SysProps.setProperty("dubbo.protocol.name", "rest");
        SysProps.setProperty("dubbo.protocol.port", String.valueOf(port));

        try {
            ProtocolConfig protocolConfig = new ProtocolConfig();

            DubboBootstrap.getInstance()
                    .application("test-app")
                    .protocol(protocolConfig)
                    .initialize();

            Assertions.assertEquals("rest", protocolConfig.getName());
            Assertions.assertEquals(port, protocolConfig.getPort());
        } finally {
            DubboBootstrap.getInstance().stop();
        }
    }

    @Test
    void testOverrideConfigByName() {
        int port = NetUtils.getAvailablePort();
        SysProps.setProperty("dubbo.protocols.rest.port", String.valueOf(port));

        try {
            ProtocolConfig protocolConfig = new ProtocolConfig();
            protocolConfig.setName("rest");

            DubboBootstrap.getInstance()
                    .application("test-app")
                    .protocol(protocolConfig)
                    .initialize();

            Assertions.assertEquals("rest", protocolConfig.getName());
            Assertions.assertEquals(port, protocolConfig.getPort());
        } finally {
            DubboBootstrap.getInstance().stop();
        }
    }

    @Test
    void testOverrideConfigById() {
        int port = NetUtils.getAvailablePort();
        SysProps.setProperty("dubbo.protocols.rest1.name", "rest");
        SysProps.setProperty("dubbo.protocols.rest1.port",  String.valueOf(port));

        try {
            ProtocolConfig protocolConfig = new ProtocolConfig();
            protocolConfig.setName("xxx");
            protocolConfig.setId("rest1");

            DubboBootstrap.getInstance()
                    .application("test-app")
                    .protocol(protocolConfig)
                    .initialize();

            Assertions.assertEquals("rest", protocolConfig.getName());
            Assertions.assertEquals(port, protocolConfig.getPort());
        } finally {
            DubboBootstrap.getInstance().stop();
        }
    }

    @Test
    void testCreateConfigFromPropsWithId() {
        int port1 = NetUtils.getAvailablePort();
        int port2 = NetUtils.getAvailablePort();
        SysProps.setProperty("dubbo.protocols.rest1.name", "rest");
        SysProps.setProperty("dubbo.protocols.rest1.port", String.valueOf(port1));
        SysProps.setProperty("dubbo.protocol.name", "dubbo"); // ignore
        SysProps.setProperty("dubbo.protocol.port", String.valueOf(port2));

        try {

            DubboBootstrap bootstrap = DubboBootstrap.getInstance();
            bootstrap.application("test-app")
                    .initialize();

            ConfigManager configManager = bootstrap.getConfigManager();
            Collection<ProtocolConfig> protocols = configManager.getProtocols();
            Assertions.assertEquals(1, protocols.size());

            ProtocolConfig protocol = configManager.getProtocol("rest1").get();

            Assertions.assertEquals("rest", protocol.getName());
            Assertions.assertEquals(port1, protocol.getPort());
        } finally {
            DubboBootstrap.getInstance().stop();
        }
    }

    @Test
    void testCreateConfigFromPropsWithName() {
        int port1 = NetUtils.getAvailablePort();
        int port2 = NetUtils.getAvailablePort();
        SysProps.setProperty("dubbo.protocols.rest.port", String.valueOf(port1));
        SysProps.setProperty("dubbo.protocol.name", "dubbo"); // ignore
        SysProps.setProperty("dubbo.protocol.port", String.valueOf(port2));

        try {

            DubboBootstrap bootstrap = DubboBootstrap.getInstance();
            bootstrap.application("test-app")
                    .initialize();

            ConfigManager configManager = bootstrap.getConfigManager();
            Collection<ProtocolConfig> protocols = configManager.getProtocols();
            Assertions.assertEquals(1, protocols.size());

            ProtocolConfig protocol = configManager.getProtocol("rest").get();

            Assertions.assertEquals("rest", protocol.getName());
            Assertions.assertEquals(port1, protocol.getPort());
        } finally {
            DubboBootstrap.getInstance().stop();
        }
    }

    @Test
    void testCreateDefaultConfigFromProps() {
        int port = NetUtils.getAvailablePort();
        SysProps.setProperty("dubbo.protocol.name", "rest");
        SysProps.setProperty("dubbo.protocol.port", String.valueOf(port));
        String protocolId = "rest-protocol";
        SysProps.setProperty("dubbo.protocol.id", protocolId); // Allow override config id from props

        try {

            DubboBootstrap bootstrap = DubboBootstrap.getInstance();
            bootstrap.application("test-app")
                    .initialize();

            ConfigManager configManager = bootstrap.getConfigManager();
            Collection<ProtocolConfig> protocols = configManager.getProtocols();
            Assertions.assertEquals(1, protocols.size());

            ProtocolConfig protocol = configManager.getProtocol("rest").get();
            Assertions.assertEquals("rest", protocol.getName());
            Assertions.assertEquals(port, protocol.getPort());
            Assertions.assertEquals(protocolId, protocol.getId());

        } finally {
            DubboBootstrap.getInstance().stop();
        }
    }


    @Test
    void testPreferSerializationDefault1() throws Exception {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        assertNull(protocolConfig.getPreferSerialization());

        protocolConfig.checkDefault();
        MatcherAssert.assertThat(protocolConfig.getPreferSerialization(), Matchers.equalTo(DEFAULT_PREFER_SERIALIZATION));

        protocolConfig = new ProtocolConfig();
        protocolConfig.setSerialization("x-serialization");
        assertNull(protocolConfig.getPreferSerialization());

        protocolConfig.checkDefault();
        MatcherAssert.assertThat(protocolConfig.getPreferSerialization(), Matchers.equalTo("x-serialization"));
    }

    @Test
    void testPreferSerializationDefault2() throws Exception {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        assertNull(protocolConfig.getPreferSerialization());

        protocolConfig.refresh();
        MatcherAssert.assertThat(protocolConfig.getPreferSerialization(), Matchers.equalTo(DEFAULT_PREFER_SERIALIZATION));

        protocolConfig = new ProtocolConfig();
        protocolConfig.setSerialization("x-serialization");
        assertNull(protocolConfig.getPreferSerialization());

        protocolConfig.refresh();
        MatcherAssert.assertThat(protocolConfig.getPreferSerialization(), Matchers.equalTo("x-serialization"));
    }


}
