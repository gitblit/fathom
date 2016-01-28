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
import fathom.conf.Settings;
import fathom.rest.controller.Controller;
import fathom.rest.controller.ControllerHandler;
import fathom.rest.controller.ControllerRegistrar;
import fathom.rest.controller.HttpMethod;
import fathom.rest.route.LanguageHandler;
import fathom.utils.RequireUtil;
import fathom.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.Languages;
import ro.pippo.core.route.ClasspathResourceHandler;
import ro.pippo.core.route.FileResourceHandler;
import ro.pippo.core.route.PublicResourceHandler;
import ro.pippo.core.route.Route;
import ro.pippo.core.route.RouteHandler;
import ro.pippo.core.route.Router;
import ro.pippo.core.route.UrlResourceHandler;
import ro.pippo.core.route.WebjarsResourceHandler;
import ro.pippo.core.util.HttpCacheToolkit;
import ro.pippo.core.util.MimeTypes;
import ro.pippo.core.util.StringUtils;
import ro.pippo.metrics.CountedRouteHandler;
import ro.pippo.metrics.MeteredRouteHandler;
import ro.pippo.metrics.TimedRouteHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Base class for Fathom RESTful Route registration.
 * Extend this class as conf/Routes.java to set up your routes.
 *
 * @author James Moger
 */
public abstract class RoutesModule {

    private final Logger log = LoggerFactory.getLogger(RoutesModule.class);

    @Inject
    private Injector injector;

    @Inject
    private Settings settings;

    @Inject
    private Router router;

    @Inject
    private MimeTypes mimeTypes;

    @Inject
    private HttpCacheToolkit httpCacheToolkit;

    @Inject
    private Languages languages;

    @Inject
    private MetricRegistry metricRegistry;

    private Set<String> resourcePaths;

    private List<RouteRegistration> routeRegistrations;

    public final void init() {
        resourcePaths = new TreeSet<>();
        routeRegistrations = new ArrayList<>();
        setup();
        compileRoutes();
    }

    protected Injector getInjector() {
        return injector;
    }

    protected Settings getSettings() {
        return settings;
    }

    protected Router getRouter() {
        return router;
    }

    protected MimeTypes getMimeTypes() {
        return mimeTypes;
    }

    protected HttpCacheToolkit getHttpCacheToolkit() {
        return httpCacheToolkit;
    }

    protected abstract void setup();

    protected String getInclusionExpression(String... paths) {
        return getInclusionExpression(Arrays.asList(paths));
    }

    protected String getInclusionExpression(Collection<String> paths) {
        String joined = paths.stream().map(path -> StringUtils.removeStart(path, "/")).collect(Collectors.joining("|"));
        return "^(/(" + joined + ")/).*";
    }

    protected String getExclusionExpression(String... paths) {
        return getExclusionExpression(Arrays.asList(paths));
    }

    protected String getExclusionExpression(Collection<String> paths) {
        String joined = paths.stream().map(path -> StringUtils.removeStart(path, "/")).collect(Collectors.joining("|"));
        return "^(?!/(" + joined + ")/).*";
    }

    protected String getResourceExclusionExpression() {
        return getExclusionExpression(resourcePaths);
    }

    protected RouteRegistration addWebjarsResourceRoute() {
        return addWebjarsResourceRoute("/webjars");
    }

    protected RouteRegistration addWebjarsResourceRoute(String basePath) {
        resourcePaths.add(StringUtils.removeStart(basePath, "/"));
        return GET(new WebjarsResourceHandler(basePath));
    }

    protected RouteRegistration addPublicResourceRoute() {
        return addPublicResourceRoute("/public");
    }

    protected RouteRegistration addPublicResourceRoute(String basePath) {
        resourcePaths.add(StringUtils.removeStart(basePath, "/"));
        return GET(new PublicResourceHandler(basePath));
    }

    protected RouteRegistration addFileResourceRoute(String basePath, File directory) {
        resourcePaths.add(StringUtils.removeStart(basePath, "/"));
        return GET(new FileResourceHandler(basePath, directory));
    }

    protected RouteRegistration addClasspathResourceRoute(String basePath, String classpathDirectory) {
        resourcePaths.add(StringUtils.removeStart(basePath, "/"));
        return GET(new ClasspathResourceHandler(basePath, classpathDirectory));
    }


    protected RouteRegistration addLanguageFilter(boolean allowQueryParameter, boolean setCookie) {
        return addLanguageFilter(getResourceExclusionExpression(), allowQueryParameter, setCookie);
    }

    protected RouteRegistration addLanguageFilter(String uriPattern, boolean allowQueryParameter, boolean setCookie) {
        return GET(uriPattern, new LanguageHandler(languages, allowQueryParameter, setCookie));
    }

    protected void addControllers() {
        String applicationPackage = Optional.fromNullable(settings.getApplicationPackage()).or("");
        String controllerPackage = StringUtils.removeStart(applicationPackage + ".controllers", ".");
        String controllersPackage = settings.getString(Settings.Setting.application_controllersPackage, controllerPackage);
        addControllers(controllersPackage);
    }

    protected void addControllers(String... packageNames) {
        ControllerRegistrar registrar = new ControllerRegistrar(injector, settings);
        registrar.init(packageNames);
        routeRegistrations.addAll(registrar.getRouteRegistrations());
    }

    protected void addControllers(Package... packages) {
        ControllerRegistrar registrar = new ControllerRegistrar(injector, settings);
        registrar.init(packages);
        routeRegistrations.addAll(registrar.getRouteRegistrations());
    }

    protected void addControllers(Class<? extends Controller>... controllers) {
        ControllerRegistrar registrar = new ControllerRegistrar(injector, settings);
        registrar.init(controllers);
        routeRegistrations.addAll(registrar.getRouteRegistrations());
    }

    protected RouteRegistration ALL(String uriPattern, RouteHandler<Context> handler) {
        return registerRoute(uriPattern, HttpMethod.ALL, handler);
    }

    protected RouteRegistration ALL(String uriPattern, Class<? extends RouteHandler<Context>> handlerClass) {
        return registerRoute(uriPattern, HttpMethod.ALL, getInjector().getInstance(handlerClass));
    }

    protected RouteRegistration OPTIONS(String uriPattern, RouteHandler<Context> handler) {
        return registerRoute(uriPattern, HttpMethod.OPTIONS, handler);
    }

    protected RouteRegistration OPTIONS(String uriPattern, Class<? extends RouteHandler<Context>> handlerClass) {
        return registerRoute(uriPattern, HttpMethod.OPTIONS, getInjector().getInstance(handlerClass));
    }

    protected RouteRegistration OPTIONS(String uriPattern, Class<? extends Controller> controllerClass, String methodName) {
        return registerRoute(uriPattern, HttpMethod.OPTIONS, controllerClass, methodName);
    }

    protected RouteRegistration HEAD(String uriPattern, RouteHandler<Context> handler) {
        return registerRoute(uriPattern, HttpMethod.HEAD, handler);
    }

    protected RouteRegistration HEAD(String uriPattern, Class<? extends RouteHandler<Context>> handlerClass) {
        return registerRoute(uriPattern, HttpMethod.HEAD, getInjector().getInstance(handlerClass));
    }

    protected RouteRegistration HEAD(String uriPattern, Class<? extends Controller> controllerClass, String methodName) {
        return registerRoute(uriPattern, HttpMethod.HEAD, controllerClass, methodName);
    }

    protected RouteRegistration GET(UrlResourceHandler resourceHandler) {
        return registerRoute(resourceHandler.getUriPattern(), HttpMethod.GET, resourceHandler);
    }

    protected RouteRegistration GET(String uriPattern, RouteHandler<Context> handler) {
        return registerRoute(uriPattern, HttpMethod.GET, handler);
    }

    protected RouteRegistration GET(String uriPattern, Class<? extends RouteHandler<Context>> handlerClass) {
        return registerRoute(uriPattern, HttpMethod.GET, getInjector().getInstance(handlerClass));
    }

    protected RouteRegistration GET(String uriPattern, Class<? extends Controller> controllerClass, String methodName) {
        return registerRoute(uriPattern, HttpMethod.GET, controllerClass, methodName);
    }

    protected RouteRegistration POST(String uriPattern, RouteHandler<Context> handler) {
        return registerRoute(uriPattern, HttpMethod.POST, handler);
    }

    protected RouteRegistration POST(String uriPattern, Class<? extends RouteHandler<Context>> handlerClass) {
        return registerRoute(uriPattern, HttpMethod.POST, getInjector().getInstance(handlerClass));
    }

    protected RouteRegistration POST(String uriPattern, Class<? extends Controller> controllerClass, String methodName) {
        return registerRoute(uriPattern, HttpMethod.POST, controllerClass, methodName);
    }

    protected RouteRegistration PUT(String uriPattern, RouteHandler<Context> handler) {
        return registerRoute(uriPattern, HttpMethod.PUT, handler);
    }

    protected RouteRegistration PUT(String uriPattern, Class<? extends RouteHandler<Context>> handlerClass) {
        return registerRoute(uriPattern, HttpMethod.PUT, getInjector().getInstance(handlerClass));
    }

    protected RouteRegistration PUT(String uriPattern, Class<? extends Controller> controllerClass, String methodName) {
        return registerRoute(uriPattern, HttpMethod.PUT, controllerClass, methodName);
    }

    protected RouteRegistration PATCH(String uriPattern, RouteHandler<Context> handler) {
        return registerRoute(uriPattern, HttpMethod.PATCH, handler);
    }

    protected RouteRegistration PATCH(String uriPattern, Class<? extends RouteHandler<Context>> handlerClass) {
        return registerRoute(uriPattern, HttpMethod.PATCH, getInjector().getInstance(handlerClass));
    }

    protected RouteRegistration PATCH(String uriPattern, Class<? extends Controller> controllerClass, String methodName) {
        return registerRoute(uriPattern, HttpMethod.PATCH, controllerClass, methodName);
    }

    protected RouteRegistration DELETE(String uriPattern, RouteHandler<Context> handler) {
        return registerRoute(uriPattern, HttpMethod.DELETE, handler);
    }

    protected RouteRegistration DELETE(String uriPattern, Class<? extends RouteHandler<Context>> handlerClass) {
        return registerRoute(uriPattern, HttpMethod.DELETE, getInjector().getInstance(handlerClass));
    }

    protected RouteRegistration DELETE(String uriPattern, Class<? extends Controller> controllerClass, String methodName) {
        return registerRoute(uriPattern, HttpMethod.DELETE, controllerClass, methodName);
    }

    protected RouteRegistration registerRoute(String uriPattern, String httpMethod, Class<? extends Controller> controllerClass, String methodName) {
        ControllerHandler controllerHandler = new ControllerHandler(injector, controllerClass, methodName);
        controllerHandler.validateMethodArgs(uriPattern);
        return registerRoute(uriPattern, httpMethod, controllerHandler);
    }

    private RouteRegistration registerRoute(String uriPattern, String httpMethod, RouteHandler routeHandler) {
        RouteRegistration routeRegistration = new RouteRegistration(httpMethod, uriPattern, routeHandler);
        routeRegistrations.add(routeRegistration);
        return routeRegistration;
    }

    /**
     * Adds Routes to the Router respecting exclusion rules.
     *
     * Also wraps RouteHandlers with Metrics handlers and sets Route names.
     */
    private void compileRoutes() {
        Iterator<RouteRegistration> iterator = routeRegistrations.iterator();

        while (iterator.hasNext()) {
            RouteRegistration routeRegistration = iterator.next();
            iterator.remove();

            //
            // Enforce mode requirements specified for Route
            //
            if (routeRegistration.getModes() != null) {
                // Enforce specified modes for the route
                if (!routeRegistration.getModes().contains(settings.getMode())) {
                    log.debug("Excluding {} '{}' because {} is not specified in mode set {}",
                            routeRegistration.getRequestMethod(), routeRegistration.getUriPattern(), settings.getMode(), routeRegistration.getModes());
                    continue;
                }
            }

            //
            // Enforce annotated requirements on Controllers
            //
            if (routeRegistration.getRouteHandler() instanceof ControllerHandler) {
                // Enforce RequireUtil rules for the controller
                ControllerHandler controllerHandler = (ControllerHandler) routeRegistration.getRouteHandler();
                if (!RequireUtil.allowMethod(settings, controllerHandler.getControllerMethod())) {
                    continue;
                }

                if (Strings.isNullOrEmpty(routeRegistration.getName())) {
                    routeRegistration.setName(Util.toString(controllerHandler.getControllerMethod()));
                }
            }
            //
            // Automatically name Route if name is not specified
            //
            else if (Strings.isNullOrEmpty(routeRegistration.getName())) {
                // try to name the route
                Class<? extends RouteHandler> routeHandlerClass = routeRegistration.getRouteHandler().getClass();
                if (routeHandlerClass.isSynthetic()) {
                    routeRegistration.setName("lambda handler");
                } else if (routeHandlerClass.isAnonymousClass()) {
                    routeRegistration.setName("anonymous handler");
                } else {
                    routeRegistration.setName(routeHandlerClass.getName());
                }
            }

            //
            // Wrap any Route designated to collect Metrics
            //
            RouteHandler routeHandler;
            if (routeRegistration.isMetered()) {
                log.debug("Wrapping {} '{}' handler with {}", routeRegistration.getRequestMethod(), routeRegistration.getUriPattern(),
                        MeteredRouteHandler.class.getSimpleName());
                routeHandler = new MeteredRouteHandler(routeRegistration.getMetricName(), routeRegistration.getRouteHandler(), metricRegistry);
            } else if (routeRegistration.isTimed()) {
                log.debug("Wrapping {} '{}' handler with {}", routeRegistration.getRequestMethod(), routeRegistration.getUriPattern(),
                        TimedRouteHandler.class.getSimpleName());
                routeHandler = new TimedRouteHandler(routeRegistration.getMetricName(), routeRegistration.getRouteHandler(), metricRegistry);
            } else if (routeRegistration.isCounted()) {
                log.debug("Wrapping {} '{}' handler with {}", routeRegistration.getRequestMethod(), routeRegistration.getUriPattern(),
                        CountedRouteHandler.class.getSimpleName());
                routeHandler = new CountedRouteHandler(routeRegistration.getMetricName(), false, routeRegistration.getRouteHandler(), metricRegistry);
            } else {
                routeHandler = routeRegistration.getRouteHandler();
            }

            Route route = new Route(routeRegistration.getRequestMethod(), routeRegistration.getUriPattern(), routeHandler);
            route.setName(routeRegistration.getName());
            if (routeRegistration.isRunAsFinally()) {
                route.runAsFinally();
            }

            router.addRoute(route);
        }
    }
}
