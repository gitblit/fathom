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
import controllers.HelloInstanceController;
import controllers.HelloStaticRoutes;
import dao.EmployeeDao;
import dao.ItemDao;
import fathom.conf.Fathom;
import fathom.exception.FathomException;
import fathom.metrics.Metered;
import fathom.realm.Account;
import fathom.rest.Context;
import fathom.rest.RoutesModule;
import fathom.rest.controller.HttpMethod;
import fathom.rest.route.CORSFilter;
import fathom.rest.security.AuthConstants;
import fathom.rest.security.CSRFHandler;
import fathom.rest.security.FormAuthenticationGuard;
import fathom.rest.security.FormAuthenticationHandler;
import fathom.rest.security.LogoutHandler;
import fathom.rest.security.aop.RequireToken;
import models.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.route.RouteHandler;
import routes.XmlRpcMethods;

import java.io.File;
import java.util.Calendar;

public class Routes extends RoutesModule {

    private final Logger log = LoggerFactory.getLogger(Routes.class);

    @Inject
    ItemDao dao;

    @Inject
    EmployeeDao employeeDao;

    @Inject
    Fathom ftm;

    @Override
    protected void setup() {

        /*
         * Setup resource handlers
         */
        addWebjarsResourceRoute().named("webjars resource route");
        addPublicResourceRoute().named("public resource route");
        addFileResourceRoute("/ext", new File("src/main/resources/public/css")).named("file resource route");

        /*
         * Define a resource exclusion regular expression.
         * This ensures that we don't waste time processing
         * the resource routes registered above.
         */
        final String appFilter = getResourceExclusionExpression();

        /*
         * Register a language filter which processes a lang=? query parameter,
         * sets the preferred language in the Context, and sets a Response cookie.
         *
         * This filter only applies to GET requests.
         */
        addLanguageFilter(appFilter, true, true).named("language filter");

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

            Account account = ctx.getSession(AuthConstants.ACCOUNT_ATTRIBUTE);
            if (account != null) {
                ctx.setLocal(AuthConstants.ACCOUNT_ATTRIBUTE, account);
            }

            ctx.next();

        }).named("response header & bindings filter");

        /*
         * Create a form authentication handler and guard for the "secure" routes
         */
        ALL("/login", FormAuthenticationHandler.class);
        ALL("/logout", new LogoutHandler());

        /*
         * Register a CSRF token generator and validator.
         * This creates a session for all matching requests.
         */
        CSRFHandler csrfHandler = new CSRFHandler();
        ALL("/(secure/.*|collections|content|api)", csrfHandler).named("CSRF handler");

        /*
         * Create a form authentication guard for secure routes.
         * In the absence of an authenticated session, the browser is redirected
         * to the login url.
         */
        FormAuthenticationGuard guard = new FormAuthenticationGuard("/login");
        GET("/(secure/.*|collections|content|api)", guard);
        POST("/(secure/.*|collections|content|api)", guard);

        /*
         * Root page
         */
        GET("/", (ctx) -> {
            ctx.setLocal("items", dao.getAll());
            ctx.render("index");
        }).named("root page");

        /*
         * Register XML-RPC methods
         */
        POST("/RPC2", XmlRpcMethods.class);

        /*
         * /secure is available for authenticated sessions.
         */
        addRouteGroup("/secure")
                .GET("/hello", ctx -> ctx.text().send("Hello World!"))
                .named("RouteGroup example route")
                .routeGroup()
                /* add nested RouteGroup /secure/employees */
                .addRouteGroup("/employees")
                /* Get the employees list. */
                .GET((ctx) -> {
                    ctx.setLocal("employees", employeeDao.getAll());
                    ctx.render("employees");
                }).named("employees list").meteredAs("getEmployeesList")
                .routeGroup()
                /* Get an employee by their id. */
                .GET("{id: [0-9]+}", new RouteHandler<Context>() {
                    @Metered("getEmployee")
                    @Override
                    public void handle(Context ctx) {
                        int id = ctx.getParameter("id").toInt(0);
                        Employee employee;
                        if (id > 0) {
                            // get an existing employee
                            employee = employeeDao.get(id);
                            if (employee == null) {
                                ctx.flashError("No employee #{}", id);
                                ctx.redirect("/secure/employees");
                                return;
                            }
                        } else {
                            // new employee
                            employee = new Employee();

                            // set the initial start date to the next business day
                            Calendar calendar = Calendar.getInstance();
                            int dow = calendar.get(Calendar.DAY_OF_WEEK);
                            if (dow >= Calendar.FRIDAY) {
                                // start Monday
                                calendar.add(Calendar.DATE, 9 - dow);
                            } else {
                                // start tomorrow
                                calendar.add(Calendar.DATE, 1);
                            }
                            employee.setStartDate(calendar.getTime());
                        }
                        ctx.setLocal("employee", employee);
                        ctx.setLocal("positions", employeeDao.getPositions());
                        ctx.setLocal("offices", employeeDao.getOffices());
                        ctx.render("employee");
                    }

                }).named("get employee")
                .routeGroup()
                /*
                 * Save an Employee through a POST.
                 *
                 * This Route is guarded by the CSRF Handler which
                 * ensures that the request is being sent by the form
                 * generated by Fathom.
                 */
                .POST("{id: [0-9]+}", (ctx) -> {
                    // create or update an employee
                    Employee employee = ctx.createEntityFromParameters(Employee.class);
                    employee = employeeDao.save(employee);
                    log.info("saved employee '{}'", employee.getName());
                    ctx.flashInfo("{} has been saved", employee.getName());
                    ctx.redirect("/secure/employees");
                }).named("save employee")
                .routeGroup()
                /*
                 * Delete an Employee through a POST.
                 *
                 * This Route is guarded by the CSRF Handler which
                 * ensures that the request is being sent by the form
                 * generated by Fathom.
                 */
                .POST("{id: [0-9]+}/delete", (ctx) -> {
                    // delete an employee
                    int id = ctx.getParameter("id").toInt(0);
                    if (id > 0) {
                        Employee employee = employeeDao.delete(id);
                        log.info("Deleted employee '{}'", employee.getName());
                    } else {
                        ctx.flashError("Can't delete employee 0");
                    }
                    ctx.redirect("/secure/employees");
                }).named("delete an employee");


        /*
         * Add a route that throws an exception
         */
        GET("/internalError", (ctx) -> {
            throw new FathomException("This is an example exception");
        });


        /*
         * Add a CORS filter for our API routes
         */

        CORSFilter corsFilter = new CORSFilter();
        corsFilter.setAllowOrigin("*");
        corsFilter.setAllowMethods(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE, HttpMethod.HEAD);
        corsFilter.setAllowHeaders("Content-Type", RequireToken.DEFAULT, "Authorization", CSRFHandler.HEADER);
        ALL("/api/?.*", corsFilter).named("CORS Filter");

        /*
         * Discover and add annotated controllers
         */
        addControllers();

        /*
         * Add some ignore path definitions
          */
        getRouter().ignorePaths("/favicon.ico");

        /*
         * Manually add some controller examples
         */
        GET("/instance", HelloInstanceController.class, "hello").meteredAs("hello.instance");
        GET("/static", HelloStaticRoutes::hello).meteredAs("hello.static");

    }

}
