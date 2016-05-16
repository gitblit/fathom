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

package fathom.rest;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import fathom.Constants;
import fathom.Service;
import fathom.conf.Settings;
import fathom.exception.FathomException;
import fathom.exception.StatusCodeException;
import fathom.rest.controller.ControllerHandler;
import fathom.utils.ClassUtil;
import fathom.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.Application;
import ro.pippo.core.ContentTypeEngine;
import ro.pippo.core.ContentTypeEngines;
import ro.pippo.core.TemplateEngine;
import ro.pippo.core.route.Route;
import ro.pippo.core.route.Router;
import ro.pippo.metrics.MetricsDispatchListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author James Moger
 */
@Singleton
class RestService implements Service {

    private static final Logger log = LoggerFactory.getLogger(RestService.class);

    private static final String ROUTES_CLASS = "conf.Routes";

    private static final String SETTING_ENGINES_LOG = "rest.engines.log";

    private static final String SETTING_ROUTES_LOG = "rest.routes.log";

    private static final String SETTING_ROUTES_MAX_LINE_LENGTH = "rest.routes.maxLineLength";

    private static final String REST_ROUTES_LOG_HANDLERS = "rest.routes.logHandlers";

    @Inject
    Injector injector;

    @Inject
    Settings settings;

    @Inject
    Application application;

    @Inject
    Router router;

    @Inject
    MetricRegistry metricRegistry;

    private boolean isRunning;

    @Override
    public int getPreferredStartOrder() {
        return 100;
    }

    @Override
    public void start() {

        initializeApplication();

        String border = Strings.padEnd("", Constants.MIN_BORDER_LENGTH, '-');
        Optional<String> applicationPackage = Optional.fromNullable(settings.getApplicationPackage());

        log.info(border);
        log.info("Registered engines");
        log.info(border);
        logEngines();

        log.debug(border);
        log.debug("Initializing router");
        log.debug(border);
        initializeRouter(applicationPackage);

        log.info("");
        log.info(border);
        log.info("RESTful routes ({}) served on base path '{}'",
                router.getRoutes().size(),
                Strings.isNullOrEmpty(router.getApplicationPath()) ? "/" : router.getApplicationPath());
        log.info(border);
        logRoutes(router);

        isRunning = true;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void stop() {
        application.destroy();
    }

    private void initializeApplication() {

        // hook-up the Metrics dispatch listener
        MetricsDispatchListener metricsDispatchListener = new MetricsDispatchListener(metricRegistry);
        application.getRoutePreDispatchListeners().add(metricsDispatchListener);
        application.getRoutePostDispatchListeners().add(metricsDispatchListener);

        // set the StatusCodeException handler
        application.getErrorHandler().setExceptionHandler(StatusCodeException.class, (exception, ctx) -> {
            StatusCodeException statusCodeException = (StatusCodeException) exception;
            ctx.setLocal("message", statusCodeException.getMessage());
            application.getErrorHandler().handle(statusCodeException.getStatusCode(), ctx);
        });

    }

    private void logEngines() {
        if (!settings.getBoolean(SETTING_ENGINES_LOG, true)) {
            return;
        }

        TemplateEngine templateEngine = application.getTemplateEngine();
        ContentTypeEngines engines = application.getContentTypeEngines();

        List<String> contentTypes = new ArrayList<>(engines.getContentTypes());
        Collections.sort(contentTypes);

        int maxContentTypeLen = 0;
        int maxTemplateEngineLen = templateEngine == null ? 0 : templateEngine.getClass().getName().length();

        for (String contentType : contentTypes) {

            ContentTypeEngine engine = engines.getContentTypeEngine(contentType);

            maxContentTypeLen = Math.max(maxContentTypeLen, contentType.length());
            maxTemplateEngineLen = Math.max(maxTemplateEngineLen, engine.getClass().getName().length());

        }

        if (templateEngine != null) {
            log.info("{}  =>  {}",
                    Strings.padEnd("templates", maxContentTypeLen, ' '),
                    templateEngine.getClass().getName());
        }

        for (String contentType : contentTypes) {

            ContentTypeEngine engine = engines.getContentTypeEngine(contentType);
            log.info("{}  =>  {}",
                    Strings.padEnd(contentType, maxContentTypeLen, ' '),
                    engine.getClass().getName());

        }

    }

    private void initializeRouter(Optional<String> applicationPackage) {
        String routesClassName = ClassUtil.buildClassName(applicationPackage, ROUTES_CLASS);
        if (ClassUtil.doesClassExist(routesClassName)) {
            Class<?> routesClass = ClassUtil.getClass(routesClassName);
            if (RoutesModule.class.isAssignableFrom(routesClass)) {
                RoutesModule routes = (RoutesModule) injector.getInstance(routesClass);
                routes.init();
            } else {
                throw new FathomException("Your Routes class '{}' does not subclass '{}'!", routesClassName,
                        RoutesModule.class.getName());
            }
        } else {
            log.debug("Did not find '{}' in your application!", routesClassName);
        }
    }

    private void logRoutes(Router router) {
        if (!settings.getBoolean(SETTING_ROUTES_LOG, true)) {
            return;
        }

        // determine the width of the columns
        int maxMethodLen = 0;
        int maxPathLen = 0;
        int maxControllerLen = 0;

        if (router.getRoutes().isEmpty()) {
            log.info("no routes found");
            return;
        }

        for (Route route : router.getRoutes()) {

            maxMethodLen = Math.max(maxMethodLen, route.getRequestMethod().length());
            maxPathLen = Math.max(maxPathLen, route.getUriPattern().length());

            if (route.getRouteHandler() instanceof ControllerHandler) {

                ControllerHandler handler = (ControllerHandler) route.getRouteHandler();
                int controllerLen = Util.toString(handler.getControllerMethod()).length();
                maxControllerLen = Math.max(maxControllerLen, controllerLen);

            } else if (route.getName() != null) {
                maxControllerLen = Math.max(maxControllerLen, route.getName().length());
            }

        }

        // log the routing table
        final int maxLineLength = settings.getInteger(SETTING_ROUTES_MAX_LINE_LENGTH,120);
        final boolean logHandlers = settings.getBoolean(REST_ROUTES_LOG_HANDLERS, true);
        final boolean oneLine = (maxMethodLen + maxPathLen + maxControllerLen + 11 /* whitespace */) <= maxLineLength;
        if (!oneLine || !logHandlers) {
            maxPathLen = 0;
        }

        for (Route route : router.getRoutes()) {
            if (route.getRouteHandler() instanceof ControllerHandler) {

                ControllerHandler handler = (ControllerHandler) route.getRouteHandler();
                if (oneLine) {
                    if (logHandlers) {
                        log.info("{} {}  =>  {}()",
                                Strings.padEnd(route.getRequestMethod(), maxMethodLen, ' '),
                                Strings.padEnd(route.getUriPattern(), maxPathLen, ' '),
                                Util.toString(handler.getControllerMethod()));
                    } else {
                        log.info("{} {}",
                                Strings.padEnd(route.getRequestMethod(), maxMethodLen, ' '),
                                Strings.padEnd(route.getUriPattern(), maxPathLen, ' '));
                    }
                } else {
                    log.info("{} {}",
                            Strings.padEnd(route.getRequestMethod(), maxMethodLen, ' '),
                            Strings.padEnd(route.getUriPattern(), maxPathLen, ' '));
                    if (logHandlers) {
                        log.info("{} {}()",
                                Strings.padEnd("", maxMethodLen, ' '),
                                Util.toString(handler.getControllerMethod()));
                    }
                }

            } else if (route.getName() != null) {
                if (oneLine) {
                    if (logHandlers) {
                        log.info("{} {}  =>  {}",
                                Strings.padEnd(route.getRequestMethod(), maxMethodLen, ' '),
                                Strings.padEnd(route.getUriPattern(), maxPathLen, ' '),
                                route.getName());
                    } else {
                        log.info("{} {}",
                                Strings.padEnd(route.getRequestMethod(), maxMethodLen, ' '),
                                Strings.padEnd(route.getUriPattern(), maxPathLen, ' '));
                    }
                } else {
                    log.info("{} {}",
                            Strings.padEnd(route.getRequestMethod(), maxMethodLen, ' '),
                            Strings.padEnd(route.getUriPattern(), maxPathLen, ' '));
                    if (logHandlers) {
                        log.info("{} {}",
                                Strings.padEnd("", maxMethodLen, ' '),
                                route.getName());
                    }
                }
            } else {
                log.info("{} {}",
                        Strings.padEnd(route.getRequestMethod(), maxMethodLen, ' '),
                        route.getUriPattern());
            }
        }

    }

}
