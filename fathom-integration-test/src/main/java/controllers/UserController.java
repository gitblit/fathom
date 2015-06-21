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
import fathom.rest.controller.Return;
import fathom.rest.controller.exceptions.RequiredException;
import fathom.rest.controller.exceptions.ValidationException;
import fathom.rest.swagger.Desc;
import fathom.rest.swagger.Form;
import fathom.rest.swagger.ApiNotes;
import fathom.rest.swagger.Password;
import fathom.rest.swagger.ApiTag;
import models.petstore.User;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Implementation of the Swagger Petstore /user API.
 *
 * @author James Moger
 */
@Path("/user")
@ApiTag(name = "user", description = "Operations about user")
@Produces({Produces.JSON, Produces.XML})
public class UserController extends ApiV2 {

    @POST("/createWithArray")
    @Named("Create list of users with given input array")
    @Return(code = 201, description = "Users created")
    public void createUsersWithArrayInput(@Desc("List of User object") @Body User[] users) {
    }

    @POST("/createWithList")
    @Named("Create list of users with given input array")
    @Return(code = 201, description = "Users created")
    public void createUsersWithListInput(@Desc("List of User object") @Body List<User> users) {
    }

    @PUT("/{username}")
    @Named("Update user")
    @ApiNotes("classpath:swagger/controllers/UserController/note.md")
    @Return(code = 400, description = "Invalid username supplied", onResult = RequiredException.class)
    @Return(code = 404, description = "User not found")
    public void updateUser(@Desc("Username of User to be updated") @NotNull String username,
                           @Desc("Updated User object") @Body User user) {
    }

    @DELETE("/{username}")
    @Named("Delete user")
    @ApiNotes("classpath:swagger/controllers/UserController/note.md")
    @Return(code = 400, description = "Invalid username supplied", onResult = RequiredException.class)
    @Return(code = 404, description = "User not found")
    public void deleteUser(@Desc("Username of User to be deleted") @NotNull String username) {
    }

    @GET("/{username}")
    @Named("Get user by username")
    @Return(code = 200, description = "User", onResult = User.class)
    @Return(code = 400, description = "Invalid username supplied", onResult = RequiredException.class)
    @Return(code = 404, description = "User not found")
    public User getUserByName(@Desc("Username of User to be fetched. Use user1 for testing.") @NotNull String username) {
        User user = new User();
        return user;
    }

    @POST("/login")
    @Named("Log user into the system")
    @Return(code = 200, description = "User logged in", onResult = String.class)
    @Return(code = 400, description = "Invalid credentials", onResult = ValidationException.class)
    public String loginUser(@Desc("The Username for login") @NotNull @Form String username,
                          @Desc("The password for login in clear text") @Form @Password @NotNull String password) {
        if (username.equals(password)) {
            return "Welcome!";
        }
        throw new ValidationException();
    }

    @POST("/logout")
    @Named("Log user out of the system")
    @Return(code = 200, description = "User logged out")
    public void logoutUser() {
    }

    @POST("/")
    @Named("Create user")
    @ApiNotes("classpath:swagger/controllers/UserController/note.md")
    @Return(code = 400, description = "Invalid username supplied", onResult = RequiredException.class)
    @Return(code = 404, description = "User not found")
    public void createUser(@Desc("User object to create") @Body User user) {
    }

}