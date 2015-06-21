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
import fathom.rest.security.AuthConstants;
import fathom.security.SecurityManager;
import fathom.utils.ClassUtil;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.route.RouteDispatcher;

/**
 * @author James Moger
 */
public class RequireTokenInterceptor extends SecurityInterceptor {

    private final Logger log = LoggerFactory.getLogger(RequireTokenInterceptor.class);

    private final Provider<SecurityManager> securityManager;

    @Inject
    public RequireTokenInterceptor(Provider<SecurityManager> securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        RequireToken requireToken = ClassUtil.getAnnotation(invocation.getMethod(), RequireToken.class);
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

        return invocation.proceed();
    }

}
