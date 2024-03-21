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
package org.apache.dubbo.rpc.cluster.xds.utils;

import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.cluster.xds.exception.XdsResourceUpdateException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.envoyproxy.envoy.config.core.v3.DataSource;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_RESPONSE_XDS;

public class XdsIoUtils {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(XdsIoUtils.class);

    public static <T extends Message> T unpack(Any any, Class<T> target) {
        try {
            return any.unpack(target);
        } catch (InvalidProtocolBufferException e) {
            logger.error(REGISTRY_ERROR_RESPONSE_XDS, "", "", "Error occur when decode xDS response.", e);
            return null;
        }
    }

    public static InputStream readFromDataSource(DataSource dataSource) {
        if (dataSource.hasFilename()) {
            File file = new File(dataSource.getFilename());
            try (FileInputStream fis = new FileInputStream(file)) {
                return fis;
            } catch (Exception e) {
                throw new XdsResourceUpdateException(
                        "Some problems occurred when try to read DataSource in file system." + dataSource.getFilename(),
                        e);
            }
        } else if (dataSource.hasInlineBytes()) {

            return new ByteArrayInputStream(
                    dataSource.getInlineBytes().asReadOnlyByteBuffer().array());

        } else if (dataSource.hasEnvironmentVariable()) {
            String value = System.getenv().get(dataSource.getEnvironmentVariable());
            return new ByteArrayInputStream(value.getBytes());

        } else if (dataSource.hasInlineString()) {

            String value = dataSource.getInlineString();
            return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
        }
        throw new XdsResourceUpdateException("Empty DataSource from control pane!");
    }

    public static void logTrace(Logger logger, String prefix, Supplier<Object> supplier) {
        try {
            Object o = supplier.get();
            if (o instanceof InputStream) {
                logger.trace(prefix + new String(StreamUtils.readBytes((InputStream) o)));
            } else {
                logger.trace(prefix + o.toString());
            }
        } catch (Exception e) {
            logger.warn("Log trace failed for prefix{" + prefix + "}");
        }
    }
}
