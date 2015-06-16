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

package models.petstore;

import fathom.rest.swagger.Desc;
import fathom.rest.swagger.Example;

import javax.validation.constraints.NotNull;

@fathom.rest.swagger.Tag(name = "Pet", description = "")
public class Pet {

    @Desc("unique identifier for the pet")
    long id;

    Category category;

    @Example("doggie")
    @NotNull
    String name;

    @NotNull
    String[] photoUrls;

    Tag[] tags;

    @Desc("pet status in the store")
    PetStatus status;

}
