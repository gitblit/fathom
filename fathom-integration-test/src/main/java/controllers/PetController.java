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
import fathom.rest.controller.Header;
import fathom.rest.controller.POST;
import fathom.rest.controller.PUT;
import fathom.rest.controller.Path;
import fathom.rest.controller.Produces;
import fathom.rest.swagger.Desc;
import fathom.rest.swagger.Form;
import fathom.rest.swagger.Notes;
import fathom.rest.swagger.ResponseCode;
import fathom.rest.swagger.Summary;
import fathom.rest.swagger.Tag;
import models.petstore.Pet;
import models.petstore.PetStatus;
import ro.pippo.core.FileItem;

import javax.validation.constraints.NotNull;

/**
 * Implementation of the Swagger Petstore /pet API.
 *
 * @author James Moger
 */
@Path("/pet")
@Tag(name = "pet", description = "Operations about pets")
@Produces({Produces.JSON, Produces.XML})
public class PetController extends ApiV2 {

    @PUT
    @Summary("Update an existing pet")
    @ResponseCode(code = 400, message = "Invalid ID supplied")
    @ResponseCode(code = 404, message = "Pet not found")
    @ResponseCode(code = 405, message = "Validation exception")
    public void updatePet(@Desc("Pet object that needs to be updated in the store") @Body Pet pet) {
        getResponse().ok();
    }

    @POST
    @Summary("Add a new pet to the store")
    @ResponseCode(code = 405, message = "Invalid input")
    public void addPet(@Desc("Pet object that needs to be added to the store") @Body Pet pet) {
        getResponse().ok();
    }

    @GET("/findByStatus")
    @Summary("Finds pets by status")
    @Notes
    @ResponseCode(code = 200, message = "Successful operation", returns = Pet[].class)
    @ResponseCode(code = 400, message = "Invalid status value")
    public void findPetsByStatus(@Desc("Status values that need to be considered for filter") @NotNull PetStatus[] status) {
        getResponse().ok().send(status);
    }

    @GET("/findByTags")
    @Summary("Finds pets by tags")
    @Notes
    @ResponseCode(code = 200, message = "Successful operation", returns = Pet[].class)
    @ResponseCode(code = 400, message = "Invalid tag value")
    public void findPetsByTags(@Desc("Tags to filter by") @NotNull String[] tag) {
        getResponse().ok().send(tag);
    }

    @POST("/{petId}")
    @Summary("Updates a pet in the store with form data")
    @ResponseCode(code = 405, message = "Invalid input")
    public void updatePetWithForm(
            @Desc("ID of pet that needs to be updated") long petId,
            @Desc("Updated name of the pet") @Form String name,
            @Desc("Updated status of the pet") @Form PetStatus status) {
        getResponse().ok();
    }

    @DELETE("/{petId}")
    @Summary("Deletes a pet")
    @ResponseCode(code = 400, message = "Invalid pet value")
    public void deletePet(@Desc("Pet id to delete") long petId, @Header String api_key) {
        getResponse().ok();
    }

    @GET("/{petId}")
    @Summary("Finds pet by ID")
    @Notes
    @ResponseCode(code = 200, message = "Successful operation", returns = Pet.class)
    @ResponseCode(code = 400, message = "Invalid ID supplied value")
    @ResponseCode(code = 404, message = "Pet not found")
    public void getPetById(@Desc("ID of pet that needs to be fetched") long petId) {
        getResponse().ok();
    }

    @POST("/{petId}/uploadImage")
    @Summary("uploads an image")
    @Produces(Produces.JSON)
    public void uploadFile(
            @Desc("ID of pet to update") long petId,
            @Desc("Additional data to pass to server") @Form String additionalMetadata,
            @Desc("file to upload") @Form FileItem file) {

        getResponse().ok();
    }

}
