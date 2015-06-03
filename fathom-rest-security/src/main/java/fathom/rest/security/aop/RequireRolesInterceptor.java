/*
 * Copyright (C) 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fathom.rest.security.aop;

import com.google.inject.Inject;
import fathom.realm.Account;
import fathom.rest.controller.extractors.AuthExtractor;
import fathom.rest.Context;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ro.pippo.core.route.RouteDispatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James Moger
 */
public class RequireRolesInterceptor implements MethodInterceptor {

    @Inject
    public RequireRolesInterceptor() {
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        RequireRoles annotation = invocation.getMethod().getAnnotation(RequireRoles.class);
        List<String> roles = new ArrayList<>();
        for (RequireRole role : annotation.value()) {
            roles.add(role.value());
        }

        Context context = RouteDispatcher.getRouteContext();
        AuthExtractor extractor = new AuthExtractor();
        Account account = extractor.extract(context);

        account.checkRoles(roles.toArray(new String[roles.size()]));

        return invocation.proceed();
    }

}