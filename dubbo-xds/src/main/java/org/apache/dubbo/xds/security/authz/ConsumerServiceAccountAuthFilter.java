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
package org.apache.dubbo.xds.security.authz;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.security.api.ServiceAccountSource;

import java.util.Arrays;
import java.util.List;

@Activate(group = CommonConstants.CONSUMER, order = -10000)
public class ConsumerServiceAccountAuthFilter implements Filter {

    private final ServiceAccountSource accountJwtSource;

    public ConsumerServiceAccountAuthFilter(ApplicationModel applicationModel) {
        this.accountJwtSource = applicationModel.getAdaptiveExtension(ServiceAccountSource.class);
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String security = invoker.getUrl().getParameter("security");
        if (StringUtils.isNotEmpty(security)) {
            List<String> parts = Arrays.asList(security.split(","));
            boolean enable = parts.stream().anyMatch("sa_jwt"::equals);
            if (enable) {
                invocation.setObjectAttachment("authz", accountJwtSource.getJwt(invoker.getUrl()));
            }
        }
        return invoker.invoke(invocation);
    }
}
