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

package fathom.rest.swagger;

import com.google.inject.Guice;
import com.google.inject.Injector;
import fathom.conf.Settings;
import fathom.rest.controller.Controller;
import fathom.rest.controller.ControllerHandler;
import fathom.rest.controller.Produces;
import org.junit.Test;
import ro.pippo.core.Languages;
import ro.pippo.core.Messages;
import ro.pippo.core.PippoSettings;
import ro.pippo.core.route.DefaultRouter;
import ro.pippo.core.route.Route;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * @author James Moger
 */
public class SwaggerBuilderTest {

    @Test
    public void testGenerate() throws Exception {
        DefaultRouter router = new DefaultRouter();
        for (Route route : getRoutes()) {
            router.addRoute(route);
        }

        SwaggerBuilder sb = new SwaggerBuilder(new Settings(), router);
        String json = sb.generateJSON(router.getRoutes());
        assertNotNull(json);
    }

    private List<Route> getRoutes() {
        Injector injector = Guice.createInjector();

        List<Route> routes = new ArrayList<>();
        routes.add(route(injector, "GET", "/api/{id}", TestController.class));
        routes.add(route(injector, "PUT", "/api/{id}", TestController.class));
        routes.add(route(injector, "POST", "/api/{id}", TestController.class));
        routes.add(route(injector, "PATCH", "/api/{id}", TestController.class));
        routes.add(route(injector, "DELETE", "/api/{id}", TestController.class));
        return routes;
    }

    private Route route(Injector injector, String method, String path, Class<? extends Controller> controllerClass) {
        ControllerHandler handler = new ControllerHandler(injector, controllerClass, method.toLowerCase());
        return new Route(path, method, handler);
    }

    public static class TestController extends Controller {

        @Produces({Produces.JSON, Produces.XML})
        public void get(int id) {
        }

        @Produces({Produces.JSON, Produces.XML})
        public void put(int id) {
        }

        @Produces(Produces.HTML)
        public void post(int id) {
        }

        @Produces({Produces.JSON, Produces.XML})
        public void patch(int id) {
        }

        @Produces({Produces.JSON, Produces.XML})
        public void delete(int id) {
        }

    }

}