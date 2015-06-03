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

package conf;

import com.google.inject.Inject;
import dao.ItemDao;
import fathom.conf.Fathom;
import fathom.exception.FathomException;
import fathom.realm.Account;
import fathom.rest.RoutesModule;
import fathom.rest.security.CSRFHandler;
import fathom.rest.security.FormAuthenticationGuard;
import fathom.rest.security.FormAuthenticationHandler;
import fathom.rest.security.LogoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Routes extends RoutesModule {

    private final Logger log = LoggerFactory.getLogger(Routes.class);

    @Inject
    FormAuthenticationHandler formAuthenticationHandler;

    @Inject
    ItemDao dao;

    @Inject
    Fathom ftm;

    @Override
    protected void setup() {

        /*
         * Setup classpath resource handlers
         */
        addWebjarResourceHandler().named("webjar resource handler");
        addPublicResourceHandler().named("public resource handler");

        /*
         * Define a resource exclusion regular expression.
         * This ensures that we don't waste time processing
         * the resource routes registered above.
         */
        final String appFilter = getResourceExclusionExpression();

        /*
         * Register a language handler which processes a lang=? query parameter,
         * sets the preferred language in the Context, and sets a Response cookie.
         *
         * This handler only applies to GET requests.
         */
        addLanguageHandler(appFilter, true, true).named("language handler");

        /*
         * Register a handler that binds some values to use on GET requests
         */
        GET(appFilter, (ctx) -> {
            // sets some response headers
            ctx.setHeader("app-name", getSettings().getApplicationName());
            ctx.setHeader("app-version", getSettings().getApplicationVersion());
            ctx.setHeader("fathom-mode", getSettings().getMode().toString());

            // put some values for the template engine or downstream handlers
            ctx.setLocal("appName", getSettings().getApplicationName());
            ctx.setLocal("appVersion", getSettings().getApplicationVersion());
            ctx.setLocal("bootDate", ftm.getBootDate());

            Account account = ctx.getSession("account");
            if (account != null) {
                ctx.setLocal("account", account);
            }

            ctx.next();

        }).named("response header & bindings filter");

        /*
         * Create a form authentication handler and guard for the "secure" routes
         */
        ALL("/login", formAuthenticationHandler);
        ALL("/logout", new LogoutHandler());

         /*
         * Register an CSRF token generator and validator.
         */
        ALL("/secure/.*", new CSRFHandler()).named("CSRF handler");

        /*
         * Create a form authentication guard for secure routes.
         * In the absence of an authenticated session, the browser is redirected
         * to the login url.
         */
        FormAuthenticationGuard guard = new FormAuthenticationGuard("/login");
        GET("/secure/.*", guard);
        POST("/secure/.*", guard);

        /*
         * Root page
         */
        GET("/", (ctx) -> {
            ctx.setLocal("items", dao.getAll());
            ctx.render("index");
        }).named("root page");

        /*
         * Add a route that throws an exception
         */
        GET("/internalError", (ctx) -> {
            throw new FathomException("This is an example exception");
        });

        /*
         * Discover and add annotated controllers
         */
        addAnnotatedControllers();

        /*
         * Add some ignore path definitions
          */
        getRouter().ignorePaths("/favicon.ico");

    }
}