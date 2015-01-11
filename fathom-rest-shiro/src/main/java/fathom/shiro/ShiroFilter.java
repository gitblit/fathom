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

package fathom.shiro;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fathom.rest.Context;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.web.env.WebEnvironment;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.servlet.AbstractShiroFilter;
import ro.pippo.core.Application;
import ro.pippo.core.ErrorHandler;
import ro.pippo.core.Request;
import ro.pippo.core.Response;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * A simple Shiro filter which relies on shiro.ini for configuration.
 *
 * @author James Moger
 */
@Singleton
public class ShiroFilter extends AbstractShiroFilter {

    @Inject
    WebEnvironment env;

    @Inject
    Application application;

    @Inject
    ErrorHandler errorHandler;

    @Override
    public void init() throws Exception {

        setSecurityManager(env.getWebSecurityManager());

        FilterChainResolver resolver = env.getFilterChainResolver();
        if (resolver != null) {
            setFilterChainResolver(resolver);
        }

        errorHandler.setExceptionHandler(AuthenticationException.class, (exception, ctx) -> {
            ctx.setLocal("message", exception.getMessage());
            errorHandler.handle(401, ctx);
        });

        errorHandler.setExceptionHandler(AuthorizationException.class, (exception, ctx) -> {
            ctx.setLocal("message", exception.getMessage());
            errorHandler.handle(403, ctx);
        });
    }

    protected void doFilterInternal(ServletRequest servletRequest, ServletResponse servletResponse, final FilterChain chain)
            throws ServletException, IOException {
        try {
            super.doFilterInternal(servletRequest, servletResponse, chain);
        } catch (Exception exception) {
            Exception root = (Exception) Throwables.getRootCause(exception);
            Request request = new Request((HttpServletRequest) servletRequest, application);
            Response response = new Response((HttpServletResponse) servletResponse, application);
            Context routeContext = new Context(application, request, response, Collections.emptyList());
            errorHandler.handle(root, routeContext);
        }
    }
}
