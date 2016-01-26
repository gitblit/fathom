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

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fathom.exception.StatusCodeException;
import fathom.realm.Account;
import fathom.rest.Context;
import fathom.security.SecurityManager;
import ro.pippo.core.Messages;
import ro.pippo.core.route.RouteHandler;

import javax.servlet.http.Cookie;
import java.util.concurrent.TimeUnit;

/**
 * A handler that implements HTTP Form Authentication.
 *
 * @author James Moger
 */
@Singleton
public final class FormAuthenticationHandler extends StandardCredentialsHandler implements RouteHandler<Context> {

    @Inject
    Messages messages;

    @Inject
    public FormAuthenticationHandler(SecurityManager securityManager) {
        super(securityManager);
    }

    @Override
    protected boolean isCreateSessions() {
        return true;
    }

    @Override
    public void handle(Context context) {

        // redirect if already authenticated
        if (isAuthenticated(context)) {
            // touch the session to prolong it's life
            context.touchSession();
            redirectRequest(context);
            return;
        }

        if ("GET".equals(context.getRequestMethod())) {
            // show the form login page
            context.render(AuthConstants.LOGIN_TEMPLATE);

        } else if ("POST".equals(context.getRequestMethod())) {
            // validateCredentials the credentials
            String username = context.getParameter(AuthConstants.USERNAME_PARAMETER).toString();
            String password = context.getParameter(AuthConstants.PASSWORD_PARAMETER).toString();
            boolean rememberMe = context.getParameter(AuthConstants.REMEMBER_ME_PARAMETER).toBoolean(false);

            Account account = authenticate(username, password);

            if (account != null) {
                // Recreate the session to prevent session fixation
                context.recreateSession();

                Cookie c = new Cookie("fsession", username);
                c.setHttpOnly(true);
                c.setMaxAge(-1);
                context.getResponse().cookie(c);

                setupContext(context, account);

                if (rememberMe) {
                    // set a cookie
                    Cookie cookie = new Cookie(AuthConstants.REMEMBER_ME_COOKIE, username);
                    cookie.setHttpOnly(true);
                    cookie.setMaxAge((int) TimeUnit.DAYS.toSeconds(365));
                    context.getResponse().cookie(cookie);
                }

                // redirect to the original destination or to the root
                redirectRequest(context);

            } else {
                // authentication failed, set the error message and redirect to *self*
                String message = messages.getWithDefault("fathom.invalidCredentials", "Invalid Credentials", context);
                context.flashError(message);
                context.redirect(context.getRequestUri());
            }

        } else {
            // unsupported http method
            throw new StatusCodeException(405, "Only GET and POST are supported!");
        }

    }

    protected void redirectRequest(Context context) {
        String originalDestination = context.getSession(AuthConstants.DESTINATION_ATTRIBUTE);
        String redirectPath = Optional.fromNullable(Strings.emptyToNull(originalDestination)).or("/");
        context.redirect(redirectPath);
    }
}
