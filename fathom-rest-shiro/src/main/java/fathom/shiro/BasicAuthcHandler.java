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

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fathom.conf.Settings;
import fathom.rest.Context;
import ro.pippo.core.route.RouteHandler;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A handler that implements HTTP BASIC Authentication.
 *
 * @author James Moger
 */
@Singleton
public final class BasicAuthcHandler extends AbstractAuthcHandler implements RouteHandler<Context> {

    private final String challenge;
    private final boolean isPassive;

    @Inject
    public BasicAuthcHandler(Settings settings) {
        super();

        String realmName = settings.getApplicationName();
        this.challenge = "Basic realm=\"" + realmName + "\"";
        this.isPassive = settings.getBoolean("authc.isPassive", false);
    }

    @Override
    public void handle(Context context) {

        // continue chain if already authenticated
        if (isAuthenticated(context)) {
            context.next();
            return;
        }

        // unauthenticated request
        String authorization = context.getRequest().getHeader("Authorization");
        if (!Strings.isNullOrEmpty(authorization) && authorization.startsWith("Basic")) {

            // Authorization: Basic BASE64PACKET
            String packet = authorization.substring("Basic".length()).trim();
            String credentials = new String(Base64.getDecoder().decode(packet), StandardCharsets.UTF_8);

            // credentials = username:password
            final String[] values = credentials.split(":", 2);
            final String username = values[0];
            final String password = values[1];

            if (authenticate(context, username, password, false)) {
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
