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
import fathom.rest.controller.ControllerPath;
import fathom.rest.controller.Controller;
import fathom.rest.controller.GET;
import fathom.rest.controller.Produces;
import models.Item;

/**
 * To be discoverable, a controller must be annotated with {@code @ControllerPath}.
 */
@ControllerPath("/api/")
public class ApiController extends Controller {

    @Inject
    ItemDao dao;

    /**
     * Responds to a GET request of an integer id like "/api/1".
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
     * @return Reply
     */
    @GET("{id: [0-9]+}")
    @Produces({Produces.JSON, Produces.XML})
    @Metered
    public void get(int id, @Auth Account account) {

        log.debug("GET item #{} for '{}'", id, account);
        Item item = dao.get(id);
        if (item == null) {
            getResponse().notFound().send("Item #{} does not exist", id);
        } else {
            getResponse().ok().send(item);
        }
    }

}
