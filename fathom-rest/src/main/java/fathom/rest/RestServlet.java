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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import ro.pippo.core.Application;
import ro.pippo.core.Request;
import ro.pippo.core.Response;
import ro.pippo.core.route.RouteContextFactory;
import ro.pippo.core.route.RouteDispatcher;
import ro.pippo.core.route.RouteMatch;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * RestServlet must be registered in your Servlets class.
 * It's job is to service incoming servlet requests and dispatch them to the
 * appropriate Handler.
 */
@Singleton
public class RestServlet extends HttpServlet {

    public static String SETTING_URL = "servlets." + RestServlet.class.getName();

    private static final long serialVersionUID = 1L;

    private final Application application;

    private final RouteDispatcher routeDispatcher;

    @Inject
    public RestServlet(Application application) {
        this.application = application;
        this.routeDispatcher = new RouteDispatcher(application) {
            protected RouteContextFactory<?> getRouteContextFactory() {
                return new RouteContextFactory<Context>() {
                    @Override
                    public Context createRouteContext(Application application, Request request, Response response, List<RouteMatch> list) {
                        return new Context(application, request, response, list);
                    }

                    @Override
                    public void init(Application application) {
                    }

                    @Override
                    public void destroy(Application application) {
                    }
                };
            }
        };
    }

    @Override
    public void init() {
        routeDispatcher.init();
    }

    @Override
    public void service(ServletRequest req, ServletResponse resp) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) req;
        HttpServletResponse httpResponse = (HttpServletResponse) resp;

        Request request = new Request(httpRequest, application);
        Response response = new Response(httpResponse, application);

        routeDispatcher.dispatch(request, response);

    }

}
