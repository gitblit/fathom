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
import fathom.rest.controller.Path;
import fathom.rest.controller.Controller;
import fathom.rest.controller.GET;
import fathom.rest.controller.PUT;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * To be discoverable, a controller must be annotated with {@code @Path}.
 */
@Path("/collections")
public class CollectionsController extends Controller {

    @Inject
    private CollectionsDao dao;

    @GET("/")
    @Metered
    public void get() {
        getResponse()
                .bind("mySet", dao.myInts)
                .bind("myList", dao.myYears)
                .bind("myTreeSet", dao.myColors)
                .render("collections");
    }

    @PUT("/")
    @Metered
    public void put(Set<Integer> mySet, List<Integer> myList, TreeSet<String> myTreeSet) {
        dao.myInts = new ArrayList<>(mySet);
        dao.myYears = new ArrayList<>(myList);
        dao.myColors = new ArrayList<>(myTreeSet);

        redirectTo("/collections");
    }

}
