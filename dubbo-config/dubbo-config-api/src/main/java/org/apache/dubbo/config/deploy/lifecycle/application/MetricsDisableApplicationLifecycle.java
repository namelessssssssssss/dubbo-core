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
package org.apache.dubbo.config.deploy.lifecycle.application;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.config.deploy.context.ApplicationContext;
import org.apache.dubbo.metrics.service.MetricsServiceExporter;

/**
 * Metrics lifecycle.
 */
@Activate(order = -2000)
public class MetricsDisableApplicationLifecycle implements ApplicationLifecycle {

    @Override
    public boolean needInitialize(ApplicationContext context) {
        return isSupportMetrics();
    }

    public boolean isSupportMetrics() {
        return isClassPresent("io.micrometer.core.instrument.MeterRegistry");
    }

    private boolean isClassPresent(String className) {
        return ClassUtils.isPresent(className, DefaultApplicationDeployer.class.getClassLoader());
    }

    @Override
    public void preDestroy(ApplicationContext applicationContext) {
        disableMetricsService(applicationContext.getModel().getBeanFactory().getBean(MetricsServiceExporter.class));
    }

    private void disableMetricsService(MetricsServiceExporter metricsServiceExporter) {
        if (metricsServiceExporter != null) {
            try {
                metricsServiceExporter.unexport();
            } catch (Exception ignored) {
                // ignored
            }
        }
    }
}