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

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fathom.exception.StatusCodeException;
import fathom.rest.Context;
import ro.pippo.core.route.RouteHandler;

/**
 * A handler that implements HTTP Form Authentication.
 *
 * @author James Moger
 */
@Singleton
public final class FormAuthcHandler extends AbstractAuthcHandler implements RouteHandler<Context> {

    public final static String ERROR_ATTRIBUTE = "error";

    public final static String LOGIN_TEMPLATE = "login";

    public final static String USERNAME_PARAMETER = "username";

    public final static String PASSWORD_PARAMETER = "password";

    public final static String REMEMBER_ME_PARAMETER = "rememberMe";

    @Inject
    public FormAuthcHandler() {
    }

    @Override
    public void handle(Context context) {

        // redirect if already authenticated
        if (isAuthenticated(context)) {
            redirectRequest(context);
            return;
        }

        if ("GET".equals(context.getRequestMethod())) {
            // show the form login page
            String error = context.removeSession(ERROR_ATTRIBUTE);
            if (!Strings.isNullOrEmpty(error)) {
                context.setLocal(ERROR_ATTRIBUTE, error);
            }
            context.render(LOGIN_TEMPLATE);

        } else if ("POST".equals(context.getRequestMethod())) {
            // validateCredentials the credentials
            String username = context.getParameter(USERNAME_PARAMETER).toString();
            String password = context.getParameter(PASSWORD_PARAMETER).toString();
            boolean rememberMe = context.getParameter(REMEMBER_ME_PARAMETER).toBoolean(false);

            if (authenticate(context, username, password, rememberMe)) {
                // redirect to the original destination or to the root
                context.recreateSession();
                redirectRequest(context);
            } else {
                // authentication failed, set the error message and redirect to *self*
                context.flashError(ERROR_ATTRIBUTE, "Authentication failed");
                context.redirect(context.getRequestUri());
            }

        } else {
            // unsupported http method
            throw new StatusCodeException(405, "Only GET and POST are supported!");
        }

    }

    protected void redirectRequest(Context context) {
        String originalDestination = context.getSession(FormAuthcGuard.DESTINATION_ATTRIBUTE);
        String redirectPath = Optional.fromNullable(Strings.emptyToNull(originalDestination)).or("/");
        context.redirect(redirectPath);
    }
}
