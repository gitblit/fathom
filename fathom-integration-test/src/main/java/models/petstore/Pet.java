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

import fathom.rest.swagger.ApiProperty;
import fathom.rest.swagger.ApiModel;

import javax.validation.constraints.NotNull;

@ApiModel(description = "a pet in the store")
public class Pet {

    @ApiProperty(description = "unique identifier for the pet")
    public long id;

    @ApiProperty
    public Category category;

    @ApiProperty(example = "doggie")
    @NotNull
    public String name;

    @ApiProperty
    @NotNull
    public String[] photoUrls;

    @ApiProperty
    public Tag[] tags;

    @ApiProperty(description = "pet status in the store")
    public PetStatus status;

}
