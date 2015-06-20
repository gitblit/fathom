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

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Provider;
import fathom.authc.TokenCredentials;
import fathom.authz.AuthorizationException;
import fathom.realm.Account;
import fathom.rest.Context;
import fathom.rest.controller.extractors.AuthExtractor;
import fathom.rest.security.AuthConstants;
import fathom.security.SecurityManager;
import fathom.utils.ClassUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.route.RouteDispatcher;

import java.lang.reflect.Method;
import java.util.List;

/**
 * ControllerInterceptor enforces authentication and authorization requirements on controllers.
 *
 * @author James Moger
 */
public class ControllerInterceptor implements MethodInterceptor {

    private final Logger log = LoggerFactory.getLogger(ControllerInterceptor.class);

    private final Provider<SecurityManager> securityManager;

    @Inject
    public ControllerInterceptor(Provider<SecurityManager> securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        Method method = invocation.getMethod();
        checkRequireToken(method);
        checkRequirePermissions(method);
        checkRequireRoles(method);
        checkRequireAdministrator(method);
        checkRequireAuthenticated(method);
        checkRequireGuest(method);

        return invocation.proceed();
    }

    Account getAccount() {
        Context context = RouteDispatcher.getRouteContext();
        AuthExtractor extractor = new AuthExtractor();
        Account account = extractor.extract(context);
        return account;
    }

    protected void checkRequireToken(Method method) {
        RequireToken requireToken = ClassUtil.getAnnotation(method, RequireToken.class);
        if (requireToken != null) {

            String tokenName = requireToken.value();

            Context context = RouteDispatcher.getRouteContext();
            // extract the named token from a header or a query parameter
            String token = Strings.emptyToNull(context.getHeader(tokenName));
            token = Optional.fromNullable(token).or(context.getParameter(tokenName).toString(""));

            if (Strings.isNullOrEmpty(token)) {
                throw new AuthorizationException("Missing '{}' token", tokenName);
            }

            Account account = getAccount();
            if (account.isGuest()) {
                // authenticate by token
                TokenCredentials credentials = new TokenCredentials(token);
                account = securityManager.get().authenticate(credentials);
                if (account == null) {
                    throw new AuthorizationException("Invalid '{}' value '{}'", tokenName, token);
                }
                context.setLocal(AuthConstants.ACCOUNT_ATTRIBUTE, account);
                log.debug("'{}' account authenticated by token '{}'", account.getUsername(), token);
            } else {
                // validate token
                account.checkToken(token);
            }
        }
    }

    protected void checkRequireRoles(Method method) {
        List<String> roles = SecurityUtil.collectRoles(method);
        if (!roles.isEmpty()) {
            Account account = getAccount();
            account.checkRoles(roles.toArray(new String[roles.size()]));
        }
    }

    protected void checkRequirePermissions(Method method) {
        List<String> permissions = SecurityUtil.collectPermissions(method);
        if (!permissions.isEmpty()) {
            Account account = getAccount();
            account.checkPermissions(permissions.toArray(new String[permissions.size()]));
        }
    }

    protected void checkRequireAdministrator(Method method) {
        RequireAdministrator annotation = ClassUtil.getAnnotation(method, RequireAdministrator.class);
        if (annotation != null) {
            Account account = getAccount();
            account.checkAdministrator();
        }
    }

    protected void checkRequireAuthenticated(Method method) {
        RequireAuthenticated annotation = ClassUtil.getAnnotation(method, RequireAuthenticated.class);
        if (annotation != null) {
            Account account = getAccount();
            account.checkAuthenticated();
        }
    }

    protected void checkRequireGuest(Method method) {
        RequireGuest annotation = ClassUtil.getAnnotation(method, RequireGuest.class);
        if (annotation != null) {
            Account account = getAccount();
            account.checkGuest();
        }
    }

}
