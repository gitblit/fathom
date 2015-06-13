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
import dao.ItemDao;
import fathom.metrics.Metered;
import fathom.realm.Account;
import fathom.rest.controller.Auth;
import fathom.rest.controller.Body;
import fathom.rest.controller.Controller;
import fathom.rest.controller.DELETE;
import fathom.rest.controller.GET;
import fathom.rest.controller.PATCH;
import fathom.rest.controller.POST;
import fathom.rest.controller.PUT;
import fathom.rest.controller.Path;
import fathom.rest.controller.Produces;
import fathom.rest.swagger.Desc;
import fathom.rest.swagger.Notes;
import fathom.rest.swagger.Summary;
import fathom.rest.swagger.Tag;
import models.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * To be discoverable, a controller must be annotated with {@code @Path}.
 */
@Path("/api/v2/items")
@Produces({Produces.JSON, Produces.XML})
@Tag(description = "Example Item API v2")
public class ApiControllerV2 extends Controller {

    private final Logger log = LoggerFactory.getLogger(ApiControllerV2.class);

    @Inject
    ItemDao dao;

    /**
     * Responds to a GET request of an integer id like "/api/v2/1".
     * <p>
     * Notice that the <code>id</code> parameter is specified in the
     * <code>@GET</code> annotation but not in the method signature.
     * </p>
     * <p>
     * This technique is relying on use of the Java 8 <code>-parameters</code>
     * flag passed to <code>javac</code>.  That flag preserves method parameter
     * names in the compiled class files.
     * </p>
     * <p>
     * This same technique is applied to the {@code @Auth} annotation which
     * references an object in the request session named "account". If the
     * session is unauthenticated, this extractor will return the Guest Account.
     * </p>
     *
     * @param id
     * @param account
     */
    @GET("{id: [0-9]+}")
    @Metered
    @Summary("Get an item")
    public void get(@Desc("item id") int id, @Auth Account account) {

        log.debug("GET item #{} for '{}'", id, account);
        Item item = dao.get(id);
        if (item == null) {
            getResponse().notFound().send("Item #{} does not exist", id);
        } else {
            getResponse().ok().send(item);
        }
    }

    @POST
    @Metered
    @Summary("Create an item")
    @Notes
    public void post(@Desc("new item to create") @Body Item item, @Auth Account account) {
        int id = 0;
        log.debug("POST item #{} for '{}'", id, account);
        getResponse().ok();
    }

    @PUT("{id: [0-9]+}")
    @Metered
    @Summary("Update an item")
    public void put(
            @Desc("item id") int id,
            @Desc("revised item to update") @Body Item item,
            @Auth Account account) {

        log.debug("PUT item #{} for '{}'", id, account);
        getResponse().ok();
    }

    @PATCH("{id: [0-9]+}")
    @Metered
    @Summary("Rename an item")
    public void patch(
            @Desc("item id") int id,
            @Desc("new name of item") String name,
            @Auth Account account) {

        log.debug("PATCH item #{} for '{}'", id, account);
        getResponse().ok();
    }

    @DELETE("{id: [0-9]+}")
    @Metered
    @Summary("Delete an item")
    @Notes
    public void delete(@Desc("item id") int id, @Auth Account account) {
        log.debug("DELETE item #{} for '{}'", id, account);
        getResponse().ok();
    }

}
