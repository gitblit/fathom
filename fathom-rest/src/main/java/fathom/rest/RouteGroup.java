/*
 * Copyright (C) 2016 the original author or authors.
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

import fathom.rest.controller.Controller;
import fathom.rest.controller.ControllerHandler;
import fathom.rest.controller.HttpMethod;
import ro.pippo.core.route.RouteHandler;
import ro.pippo.core.util.StringUtils;

import static ro.pippo.core.util.StringUtils.addStart;

/**
 * Route groups allow you to prefix <code>routeGroupUriPattern</code>, across a large number of
 * routes without needing to define this attribute on each individual route.
 * <p>
 * Also you may add (route) filters for all routes of the group.
 *
 * @author ScienJus
 * @author James Moger
 */
public class RouteGroup {

    private RoutesModule routesModule;
    private RouteGroup parentRouteGroup;
    private String routeGroupUriPattern;

    RouteGroup(RoutesModule routesModule, String uriPattern) {
        this.routesModule = routesModule;
        this.routeGroupUriPattern = uriPattern;
    }

    private RouteGroup(RoutesModule routesModule, RouteGroup parentRouteGroup, String uriPattern) {
        this.routesModule = routesModule;
        this.parentRouteGroup = parentRouteGroup;
        this.routeGroupUriPattern = uriPattern;
    }

    public RouteGroup addRouteGroup(String uriPattern) {
        return new RouteGroup(routesModule, this, uriFor(uriPattern));
    }

    public RouteGroup parentGroup() {
        return parentRouteGroup;
    }

    public RouteRegistration ALL(String uriPattern, Class<? extends Controller> controllerClass, String methodName) {
        return registerRoute(uriPattern, HttpMethod.ALL, controllerClass, methodName);
    }

    public RouteRegistration ALL(String uriPattern, Class<? extends RouteHandler<Context>> handlerClass) {
        return registerRoute(uriPattern, HttpMethod.ALL, handlerClass);
    }

    public RouteRegistration ALL(String uriPattern, RouteHandler<Context> routeHandler) {
        return registerRoute(uriPattern, HttpMethod.ALL, routeHandler);
    }

    public RouteRegistration ALL(RouteHandler<Context> routeHandler) {
        return registerRoute("", HttpMethod.ALL, routeHandler);
    }

    public RouteRegistration OPTIONS(String uriPattern, Class<? extends Controller> controllerClass, String methodName) {
        return registerRoute(uriPattern, HttpMethod.OPTIONS, controllerClass, methodName);
    }

    public RouteRegistration OPTIONS(String uriPattern, Class<? extends RouteHandler<Context>> handlerClass) {
        return registerRoute(uriPattern, HttpMethod.OPTIONS, handlerClass);
    }

    public RouteRegistration OPTIONS(String uriPattern, RouteHandler<Context> routeHandler) {
        return registerRoute(uriPattern, HttpMethod.OPTIONS, routeHandler);
    }

    public RouteRegistration OPTIONS(RouteHandler<Context> routeHandler) {
        return registerRoute("", HttpMethod.OPTIONS, routeHandler);
    }

    public RouteRegistration HEAD(String uriPattern, Class<? extends Controller> controllerClass, String methodName) {
        return registerRoute(uriPattern, HttpMethod.HEAD, controllerClass, methodName);
    }

    public RouteRegistration HEAD(String uriPattern, Class<? extends RouteHandler<Context>> handlerClass) {
        return registerRoute(uriPattern, HttpMethod.HEAD, handlerClass);
    }

    public RouteRegistration HEAD(String uriPattern, RouteHandler<Context> routeHandler) {
        return registerRoute(uriPattern, HttpMethod.HEAD, routeHandler);
    }

    public RouteRegistration HEAD(RouteHandler<Context> routeHandler) {
        return registerRoute("", HttpMethod.HEAD, routeHandler);
    }

    public RouteRegistration GET(String uriPattern, Class<? extends Controller> controllerClass, String methodName) {
        return registerRoute(uriPattern, HttpMethod.GET, controllerClass, methodName);
    }

    public RouteRegistration GET(String uriPattern, Class<? extends RouteHandler<Context>> handlerClass) {
        return registerRoute(uriPattern, HttpMethod.GET, handlerClass);
    }

    public RouteRegistration GET(String uriPattern, RouteHandler<Context> routeHandler) {
        return registerRoute(uriPattern, HttpMethod.GET, routeHandler);
    }

    public RouteRegistration GET(RouteHandler<Context> routeHandler) {
        return registerRoute("", HttpMethod.GET, routeHandler);
    }

    public RouteRegistration POST(String uriPattern, Class<? extends Controller> controllerClass, String methodName) {
        return registerRoute(uriPattern, HttpMethod.POST, controllerClass, methodName);
    }

    public RouteRegistration POST(String uriPattern, Class<? extends RouteHandler<Context>> handlerClass) {
        return registerRoute(uriPattern, HttpMethod.POST, handlerClass);
    }

    public RouteRegistration POST(String uriPattern, RouteHandler<Context> routeHandler) {
        return registerRoute(uriPattern, HttpMethod.POST, routeHandler);
    }

    public RouteRegistration POST(RouteHandler<Context> routeHandler) {
        return registerRoute("", HttpMethod.POST, routeHandler);
    }

    public RouteRegistration PUT(String uriPattern, Class<? extends Controller> controllerClass, String methodName) {
        return registerRoute(uriPattern, HttpMethod.PUT, controllerClass, methodName);
    }

    public RouteRegistration PUT(String uriPattern, Class<? extends RouteHandler<Context>> handlerClass) {
        return registerRoute(uriPattern, HttpMethod.PUT, handlerClass);
    }

    public RouteRegistration PUT(String uriPattern, RouteHandler<Context> routeHandler) {
        return registerRoute(uriPattern, HttpMethod.PUT, routeHandler);
    }

    public RouteRegistration PUT(RouteHandler<Context> routeHandler) {
        return registerRoute("", HttpMethod.PUT, routeHandler);
    }

    public RouteRegistration PATCH(String uriPattern, Class<? extends Controller> controllerClass, String methodName) {
        return registerRoute(uriPattern, HttpMethod.PATCH, controllerClass, methodName);
    }

    public RouteRegistration PATCH(String uriPattern, Class<? extends RouteHandler<Context>> handlerClass) {
        return registerRoute(uriPattern, HttpMethod.PATCH, handlerClass);
    }

    public RouteRegistration PATCH(String uriPattern, RouteHandler<Context> routeHandler) {
        return registerRoute(uriPattern, HttpMethod.PATCH, routeHandler);
    }

    public RouteRegistration PATCH(RouteHandler<Context> routeHandler) {
        return registerRoute("", HttpMethod.PATCH, routeHandler);
    }

    public RouteRegistration DELETE(String uriPattern, Class<? extends Controller> controllerClass, String methodName) {
        return registerRoute(uriPattern, HttpMethod.DELETE, controllerClass, methodName);
    }

    public RouteRegistration DELETE(String uriPattern, Class<? extends RouteHandler<Context>> handlerClass) {
        return registerRoute(uriPattern, HttpMethod.DELETE, handlerClass);
    }

    public RouteRegistration DELETE(String uriPattern, RouteHandler<Context> routeHandler) {
        return registerRoute(uriPattern, HttpMethod.DELETE, routeHandler);
    }

    public RouteRegistration DELETE(RouteHandler<Context> routeHandler) {
        return registerRoute("", HttpMethod.DELETE, routeHandler);
    }

    private String uriFor(String routeUriPattern) {
        String uri = addStart(addStart(routeUriPattern, "/"), routeGroupUriPattern);
        return "/".equals(uri) ? uri : StringUtils.removeEnd(uri, "/");
    }

    private RouteRegistration registerRoute(String uriPattern, String httpMethod, Class<? extends Controller> controllerClass, String methodName) {
        ControllerHandler controllerHandler = new ControllerHandler(routesModule.getInjector(), controllerClass, methodName);
        controllerHandler.validateMethodArgs(uriPattern);
        return registerRoute(uriPattern, httpMethod, controllerHandler);
    }

    private RouteRegistration registerRoute(String uriPattern, String httpMethod, Class<? extends RouteHandler> handlerClass) {
        return registerRoute(uriPattern, httpMethod, routesModule.getInjector().getInstance(handlerClass));
    }

    private RouteRegistration registerRoute(String uriPattern, String httpMethod, RouteHandler routeHandler) {
        RouteRegistration routeRegistration = new RouteRegistration(this, httpMethod, uriFor(uriPattern), routeHandler);
        this.routesModule.addRouteRegistration(routeRegistration);
        return routeRegistration;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + routeGroupUriPattern + "]";
    }

}
