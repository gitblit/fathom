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
import fathom.rest.controller.Named;
import fathom.rest.controller.POST;
import fathom.rest.controller.Path;
import fathom.rest.controller.Produces;
import fathom.rest.controller.Return;
import fathom.rest.controller.exceptions.RangeException;
import fathom.rest.controller.exceptions.ValidationException;
import fathom.rest.swagger.Desc;
import fathom.rest.swagger.ApiNotes;
import fathom.rest.swagger.ApiTag;
import models.petstore.Order;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * Implementation of the Swagger Petstore /store API.
 *
 * @author James Moger
 */
@Path("/store")
@Produces({Produces.JSON, Produces.XML})
@ApiTag(name = "store", description = "Access to Petstore orders")
public class StoreController extends ApiV2 {

    @DELETE("/order/{orderId}")
    @Named("Delete purchase order by ID")
    @ApiNotes
    @Return(code = 400, description = "Invalid ID supplied", onResult = RangeException.class)
    @Return(code = 404, description = "Order not found")
    public void deleteOrder(@Desc("ID of the order that needs to be deleted") @Max(5) @Min(1) long orderId) {
    }

    @GET("/order/{orderId}")
    @Named("Find purchase order by ID")
    @ApiNotes
    @Return(code = 200, description = "Valid order", onResult = Order.class)
    @Return(code = 400, description = "Invalid ID supplied", onResult = RangeException.class)
    @Return(code = 404, description = "Order not found")
    public Order getOrderById(@Desc("ID of the order that needs to be fetched") @Max(5) @Min(1) long orderId) {
        Order order = new Order();
        return order;
    }

    @POST("/order")
    @Named("Place an order for a pet")
    @Return(code = 400, description = "Invalid order", onResult = ValidationException.class)
    public void placeOrder(@Desc("Order placed for purchasing the pet") @Body Order order) {
    }

}
