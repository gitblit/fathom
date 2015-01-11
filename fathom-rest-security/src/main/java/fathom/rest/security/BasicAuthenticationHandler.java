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

package fathom.rest.security;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import fathom.conf.Settings;
import fathom.realm.Account;
import fathom.rest.Context;
import fathom.security.SecurityManager;
import ro.pippo.core.route.RouteHandler;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A handler that implements HTTP BASIC Authentication.
 *
 * @author James Moger
 */
public final class BasicAuthenticationHandler extends StandardCredentialsHandler implements RouteHandler<Context> {

    private final boolean createSessions;
    private final boolean isPassive;
    private final String challenge;

    @Inject
    public BasicAuthenticationHandler(SecurityManager securityManager, Settings settings) {
        this(securityManager, false, false, settings.getApplicationName());
    }

    public BasicAuthenticationHandler(SecurityManager securityManager, boolean createSessions, boolean isPassive, String realmName) {
        super(securityManager);

        this.createSessions = createSessions;
        this.isPassive = isPassive;
        this.challenge = "Basic realm=\"" + realmName + "\"";
    }

    @Override
    public void handle(Context context) {

        if (isAuthenticated(context)) {
            // already authenticated
            if (createSessions) {
                // touch the session to prolong it's life
                context.touchSession();
            }

            // continue chain
            context.next();

            return;
        }

        // unauthenticated request
        String authorization = context.getHeader("Authorization");
        if (!Strings.isNullOrEmpty(authorization) && authorization.startsWith("Basic")) {

            // Authorization: Basic BASE64PACKET
            String packet = authorization.substring("Basic".length()).trim();
            String credentials = new String(Base64.getDecoder().decode(packet), StandardCharsets.UTF_8);

            // credentials = username:password
            final String[] values = credentials.split(":", 2);
            final String username = values[0];
            final String password = values[1];

            Account account = authenticate(username, password);

            if (account != null) {
                // store the Account in the local Context
                context.setLocal(AuthConstants.ACCOUNT_ATTRIBUTE, account);

                if (createSessions) {
                    // store the Account in a Session
                    context.setSession(AuthConstants.ACCOUNT_ATTRIBUTE, account);
                }

                // continue the chain
                context.next();

                return;
            }

        }

        if (!isPassive) {
            // issue 401 challenge, this forces the browser credentials prompt
            context.setHeader("WWW-Authenticate", challenge);
            context.getResponse().unauthorized();
        }
    }
}
