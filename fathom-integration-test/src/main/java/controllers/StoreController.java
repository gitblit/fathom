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

import fathom.rest.controller.Body;
import fathom.rest.controller.DELETE;
import fathom.rest.controller.GET;
import fathom.rest.controller.POST;
import fathom.rest.controller.Path;
import fathom.rest.controller.Produces;
import fathom.rest.swagger.Desc;
import fathom.rest.swagger.Notes;
import fathom.rest.swagger.ResponseCode;
import fathom.rest.swagger.Summary;
import fathom.rest.swagger.Tag;
import models.petstore.Order;

/**
 * Implementation of the Swagger Petstore /store API.
 *
 * @author James Moger
 */
@Path("/store")
@Produces({Produces.JSON, Produces.XML})
@Tag(name = "store", description = "Access to Petstore orders")
public class StoreController extends ApiV2 {

    @DELETE("/order/{orderId}")
    @Summary("Delete purchase order by ID")
    @Notes
    @ResponseCode(code = 400, message = "Invalid ID supplied")
    @ResponseCode(code = 404, message = "Order not found")
    public void deleteOrder(@Desc("ID of the order that needs to be deleted") long orderId) {
        getResponse().ok();
    }

    @GET("/order/{orderId}")
    @Summary("Find purchase order by ID")
    @Notes
    @ResponseCode(code = 200, message = "Valid order", returns = Order.class)
    @ResponseCode(code = 400, message = "Invalid ID supplied")
    @ResponseCode(code = 404, message = "Order not found")
    public void getOrderById(@Desc("ID of the order that needs to be fetched") long orderId) {
        getResponse().ok();
    }

    @POST("/order")
    @Summary("Place an order for a pet")
    @ResponseCode(code = 400, message = "Invalid order")
    public void placeOrder(@Desc("Order placed for purchasing the pet") @Body Order order) {
        getResponse().ok();
    }

}
