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
import fathom.rest.controller.PUT;
import fathom.rest.controller.Path;
import fathom.rest.controller.Produces;
import fathom.rest.swagger.Desc;
import fathom.rest.swagger.Notes;
import fathom.rest.swagger.Password;
import fathom.rest.swagger.ResponseCode;
import fathom.rest.swagger.Tag;
import models.petstore.User;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Implementation of the Swagger Petstore /user API.
 *
 * @author James Moger
 */
@Path("/user")
@Tag(name = "user", description = "Operations about user")
@Produces({Produces.JSON, Produces.XML})
public class UserController extends ApiV2 {

    @POST("/createWithArray")
    @Named("Create list of users with given input array")
    public void createUsersWithArrayInput(@Desc("List of User object") @Body User[] users) {
        getResponse().ok();
    }

    @POST("/createWithList")
    @Named("Create list of users with given input array")
    public void createUsersWithListInput(@Desc("List of User object") @Body List<User> users) {
        getResponse().ok();
    }

    @PUT("/{username}")
    @Named("Update user")
    @Notes("classpath:swagger/controllers/UserController/note.md")
    @ResponseCode(code = 400, message = "Invalid username supplied")
    @ResponseCode(code = 404, message = "User not found")
    public void updateUser(@Desc("Username of User to be updated") @NotNull String username,
                           @Desc("Updated User object") @Body User user) {
        getResponse().ok();
    }

    @DELETE("/{username}")
    @Named("Delete user")
    @Notes("classpath:swagger/controllers/UserController/note.md")
    @ResponseCode(code = 400, message = "Invalid username supplied")
    @ResponseCode(code = 404, message = "User not found")
    public void deleteUser(@Desc("Username of User to be deleted") @NotNull String username) {
        getResponse().ok();
    }

    @GET("/{username}")
    @Named("Get user by username")
    @ResponseCode(code = 200, message = "User", returns = User.class)
    @ResponseCode(code = 400, message = "Invalid username supplied")
    @ResponseCode(code = 404, message = "User not found")
    public void getUserByName(@Desc("Username of User to be fetched. Use user1 for testing.") @NotNull String username) {
        getResponse().ok();
    }

    @GET("/login")
    @Named("Logs user into the system")
    @ResponseCode(code = 200, message = "Invalid username and password combination", returns = String.class)
    @ResponseCode(code = 400, message = "Invalid username and password combination")
    public void loginUser(@Desc("The Username for login") @NotNull String username,
                          @Desc("The password for login in clear text") @Password @NotNull String password) {
        getResponse().ok();
    }

    @GET("/logout")
    @Named("Logs out current logged in user session")
    public void logoutUser() {
        getResponse().ok();
    }

    @POST("/")
    @Named("Create user")
    @Notes("classpath:swagger/controllers/UserController/note.md")
    @ResponseCode(code = 400, message = "Invalid username supplied")
    @ResponseCode(code = 404, message = "User not found")
    public void createUser(@Desc("User object to create") @Body User user) {
        getResponse().ok();
    }

}