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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fathom.conf.Settings;
import fathom.rest.Context;
import ro.pippo.core.route.RouteHandler;

/**
 * A guard that requires an authenticated session and redirects
 * to a form login url if the session is unauthenticated.
 * <p>
 * This is used in conjunction with {@see FormAuthenticationHandler}.
 * </p>
 *
 * @author James Moger
 */
@Singleton
public class FormAuthenticationGuard implements RouteHandler<Context> {

    protected final String loginPath;

    @Inject
    public FormAuthenticationGuard(Settings settings) {
        this.loginPath = settings.getString("fathom.formLoginPath", "/login");
    }

    public FormAuthenticationGuard(String loginPath) {
        this.loginPath = loginPath;
    }

    @Override
    public void handle(Context context) {

        if (context.getSession(AuthConstants.ACCOUNT_ATTRIBUTE) == null) {
            // unauthenticated session, save request & redirect to login url
            String requestUri = context.getRequest().getApplicationUriWithQuery();
            context.setSession(AuthConstants.DESTINATION_ATTRIBUTE, requestUri);
            context.redirect(loginPath);
        } else {
            // session already authenticated
            context.next();
        }

    }
}
