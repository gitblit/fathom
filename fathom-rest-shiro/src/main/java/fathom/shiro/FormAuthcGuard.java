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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fathom.rest.Context;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.route.RouteHandler;

/**
 * A guard that requires an authenticated session and redirects
 * to a form login url if the session is unauthenticated.
 * <p>
 * This is used in conjunction with {@see FormAuthcHandler}.
 * </p>
 *
 * @author James Moger
 */
@Singleton
public class FormAuthcGuard implements RouteHandler<Context> {

    private final Logger log = LoggerFactory.getLogger(FormAuthcGuard.class);

    public final static String DESTINATION_ATTRIBUTE = "originalDestination";

    protected final String loginUrl;

    @Inject
    public FormAuthcGuard(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    @Override
    public void handle(Context context) {

        if (!SecurityUtils.getSubject().isAuthenticated()) {
            // unauthenticated session, save request & redirect to login url
            String requestUri = context.getRequest().getApplicationUriWithQuery();
            context.setSession(DESTINATION_ATTRIBUTE, requestUri);
            context.redirect(loginUrl);
            log.info("Unauthenticated request for {}, redirecting to {}", context.getRequestUri(), loginUrl);
        } else {
            // session already authenticated
            context.next();
        }

    }
}
