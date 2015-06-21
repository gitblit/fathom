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
import fathom.rest.controller.Named;
import fathom.rest.controller.POST;
import fathom.rest.controller.PUT;
import fathom.rest.controller.Path;
import fathom.rest.controller.Produces;
import fathom.rest.controller.Range;
import fathom.rest.controller.Required;
import fathom.rest.controller.Return;
import fathom.rest.controller.exceptions.RangeException;
import fathom.rest.controller.exceptions.ValidationException;
import fathom.rest.security.aop.RequirePermission;
import fathom.rest.security.aop.RequireToken;
import fathom.rest.swagger.Desc;
import fathom.rest.swagger.Form;
import fathom.rest.swagger.Notes;
import fathom.rest.swagger.Tag;
import models.petstore.Pet;
import models.petstore.PetStatus;
import ro.pippo.core.FileItem;

/**
 * Implementation of the Swagger Petstore /pet API.
 *
 * @author James Moger
 */
@Path("/pet")
@Tag(name = "pet", description = "Operations about pets")
@Produces({Produces.JSON, Produces.XML})
@RequireToken
@RequirePermission("read:pet")
public class PetController extends ApiV2 {

    @PUT
    @Named("Update an existing pet")
    @RequirePermission("update:pet")
    @Return(code = 400, description = "Invalid ID supplied", onResult = RangeException.class)
    @Return(code = 404, description = "Pet not found")
    @Return(code = 405, description = "Validation exception", onResult = ValidationException.class)
    public void updatePet(@Desc("Pet object that needs to be updated in the store") @Body Pet pet) {
        if (pet.id < 1 || pet.id > 5) {
            throw new RangeException();
        }
    }

    @POST
    @Named("Add a new pet to the store")
    @RequirePermission("add:pet")
    @Return(code = 405, description = "Invalid input", onResult = ValidationException.class)
    public void addPet(@Desc("Pet object that needs to be added to the store") @Body Pet pet) {
        getResponse().ok();
    }

    @GET("/findByStatus")
    @Named("Finds pets by status")
    @Notes
    @Return(code = 200, description = "Successful operation", onResult = Pet[].class)
    @Return(code = 400, description = "Invalid status value", onResult = ValidationException.class)
    public Pet[] findPetsByStatus(@Desc("Status values that need to be considered for filter") @Required PetStatus[] status) {
        Pet[] pets = new Pet[0];
        return pets;
    }

    @GET("/findByTags")
    @Named("Finds pets by tags")
    @Notes
    @Return(code = 200, description = "Successful operation", onResult = Pet[].class)
    @Return(code = 400, description = "Invalid tag value", onResult = ValidationException.class)
    public Pet[] findPetsByTags(@Desc("Tags to filter by") @Required String[] tag) {
        Pet[] pets = new Pet[0];
        return pets;
    }

    @POST("/{petId}")
    @Named("Updates a pet in the store with form data")
    @RequirePermission("update:pet")
    @Return(code = 400, description = "Invalid ID supplied", onResult = RangeException.class)
    @Return(code = 405, description = "Invalid input", onResult = ValidationException.class)
    public void updatePetWithForm(
            @Desc("ID of pet that needs to be updated") @Range(min = 1, max = 5) long petId,
            @Desc("Updated name of the pet") @Form String name,
            @Desc("Updated code of the pet") @Form PetStatus status) {
    }

    @DELETE("/{petId}")
    @Named("Deletes a pet")
    @RequirePermission("delete:pet")
    @Return(code = 400, description = "Invalid pet id", onResult = RangeException.class)
    public void deletePet(@Desc("Pet id to delete") @Range(min = 1, max = 5) long petId, @Header String api_key) {
    }

    @GET("/{petId}")
    @Named("Finds pet by ID")
    @Notes
    @Return(code = 200, description = "Successful operation", onResult = Pet.class)
    @Return(code = 400, description = "Invalid ID supplied value", onResult = RangeException.class)
    @Return(code = 404, description = "Pet not found")
    public Pet getPetById(@Desc("ID of pet that needs to be fetched") @Range(min = 1, max = 5) long petId) {
        Pet pet = new Pet();
        return pet;
    }

    @POST("/{petId}/uploadImage")
    @Named("uploads an image")
    @RequirePermission("update:pet")
    @Produces(Produces.JSON)
    @Return(code = 200, description = "Successful operation")
    public void uploadFile(
            @Desc("ID of pet to update") long petId,
            @Desc("Additional data to pass to server") @Form String additionalMetadata,
            @Desc("file to upload") @Form FileItem file) {

        getResponse().ok();
    }

}
