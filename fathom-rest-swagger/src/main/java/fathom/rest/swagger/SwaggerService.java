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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fathom.Service;
import fathom.conf.Settings;
import fathom.rest.controller.HttpMethod;
import io.swagger.models.Swagger;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.route.Route;
import ro.pippo.core.route.RouteContext;
import ro.pippo.core.route.RouteHandler;
import ro.pippo.core.route.Router;
import ro.pippo.core.route.WebjarsResourceHandler;
import ro.pippo.core.util.HttpCacheToolkit;
import ro.pippo.core.util.MimeTypes;
import ro.pippo.core.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * SwaggerService registers the Swagger Routes and triggers the Swagger document generation step.
 *
 * @author James Moger
 */
@Singleton
public class SwaggerService implements Service {

    private final static Logger log = LoggerFactory.getLogger(SwaggerService.class);

    @Inject
    Settings settings;

    @Inject
    Router router;

    @Inject
    HttpCacheToolkit httpCacheToolkit;

    @Inject
    MimeTypes mimeTypes;

    long startTime;

    Map<String, String> specifications;

    @Override
    public int getPreferredStartOrder() {
        return 110;
    }

    @Override
    public void start() {

        startTime = System.currentTimeMillis();

        log.info("Building Swagger specification from registered routes...");
        specifications = buildSpecification();
        log.info("Swagger specifications generated from registered routes in {} msecs",
                System.currentTimeMillis() - startTime);

        log.debug("Registering Swagger UI and Swagger specification routes");
        registerRoutes();

    }

    /**
     * Uses the SwaggerBuilder to generate Swagger specifications from registered routes.
     *
     * @return a map of Swagger specifications
     */
    protected Map<String, String> buildSpecification() {
        SwaggerBuilder builder = new SwaggerBuilder(settings, router);
        Swagger swagger = builder.build(router.getRoutes());

        Map<String, String> specs = new HashMap<>();
        try {
            specs.put("swagger.json", Json.pretty().writeValueAsString(swagger));
        } catch (JsonProcessingException e) {
            log.error("Failed to generate Swagger 2.0 specification as JSON", e);
        }

        try {
            specs.put("swagger.yaml", Yaml.pretty().writeValueAsString(swagger));
        } catch (JsonProcessingException e) {
            log.error("Failed to generate Swagger 2.0 specification as YAML", e);
        }

        return specs;
    }

    /**
     * Register the Routes for serving the Swagger UI and generated specifications.
     */
    protected void registerRoutes() {

        String swaggerPath = settings.getString("swagger.ui.path", "/api");
        boolean showApiKey = settings.getBoolean("swagger.ui.showApiKey", false);

        // Swagger UI route
        GET(swaggerPath, (ctx) -> {
            ctx.setLocal("apiTitle", settings.getString("swagger.api.title", settings.getApplicationName()));
            ctx.setLocal("bannerText", settings.getString("swagger.ui.bannerText", "swagger"));
            ctx.setLocal("swaggerPath", StringUtils.removeStart(swaggerPath, "/"));
            ctx.setLocal("showApiKey", showApiKey);
            ctx.render("swagger/index");
        });

        // Swagger specification routes
        for (String filename : specifications.keySet()) {
            String specPath = Joiner.on("/").join(swaggerPath, filename);
            HEAD(specPath, (ctx) -> serveSpecification(ctx, filename));
            GET(specPath, (ctx) -> serveSpecification(ctx, filename));
        }

        // Register a WebJars Route if we don't already have one
        String webJarsUri = router.uriPatternFor(WebjarsResourceHandler.class);
        if (webJarsUri == null) {
            WebjarsResourceHandler webjars = new WebjarsResourceHandler();
            webjars.setHttpCacheToolkit(httpCacheToolkit);
            webjars.setMimeTypes(mimeTypes);
            router.addRoute(new Route(webjars.getUriPattern(), HttpMethod.GET, webjars));
        }
    }

    protected void GET(String uriPattern, RouteHandler handler) {
        Route route = new Route(uriPattern, HttpMethod.GET, handler);
        router.addRoute(route);
    }

    protected void HEAD(String uriPattern, RouteHandler handler) {
        Route route = new Route(uriPattern, HttpMethod.HEAD, handler);
        router.addRoute(route);
    }

    protected void serveSpecification(RouteContext ctx, String specificationName) {
        String extension = Files.getFileExtension(specificationName);
        if (extension.isEmpty()) {
            // default to json
            specificationName += ".json";
        }

        final String finalDocumentName = specificationName;
        final String finalExtension = Files.getFileExtension(finalDocumentName);

        String document = specifications.get(finalDocumentName.toLowerCase());
        if (document == null) {
            ctx.getResponse().notFound();
        } else {
            ctx.getResponse().ok();

            httpCacheToolkit.addEtag(ctx, startTime);

            if ("json".equals(finalExtension)) {
                ctx.json();
            } else if ("yaml".equals(finalExtension)) {
                ctx.yaml();
            } else {
                ctx.text();
            }

            if (HttpMethod.GET.equals(ctx.getRequestMethod())) {
                ctx.getResponse().send(document);
            }
        }
    }

    @Override
    public void stop() {
        specifications.clear();
    }
}
