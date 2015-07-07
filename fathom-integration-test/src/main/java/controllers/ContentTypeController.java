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

package controllers;

import com.google.inject.Inject;
import dao.CollectionsDao;
import fathom.metrics.Metered;
import fathom.rest.controller.Consumes;
import fathom.rest.controller.Body;
import fathom.rest.controller.Path;
import fathom.rest.controller.Controller;
import fathom.rest.controller.GET;
import fathom.rest.controller.PUT;
import fathom.rest.controller.Produces;
import ro.pippo.core.ContentTypeEngines;

import java.util.List;

/**
 * To be discoverable, a controller must be annotated with {@code @Path}.
 */
@Path("/content")
public class ContentTypeController extends Controller {

    @Inject
    private CollectionsDao dao;

    @Inject
    private ContentTypeEngines engines;

    @GET("/")
    @Consumes({Consumes.HTML})
    @Metered
    public void get() {
        getResponse()
                .bind("myDesserts", engines.getContentTypeEngine(Produces.JSON).toString(dao.myDesserts))
                .render("content");
    }

    @PUT("/")
    @Consumes({Consumes.JSON})
    @Metered
    public void put(@Body List<String> desserts) {
        dao.myDesserts = desserts;

        redirectTo("/content");
    }

}
