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

package ${package}.models;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

import fathom.rest.controller.Required;
import fathom.rest.swagger.ApiModel;
import fathom.rest.swagger.ApiProperty;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@ApiModel(name = "Item", description = "a simple item object")
public class Item implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlAttribute
    @ApiProperty
    @Required
    public int id;

    @ApiProperty
    @NotNull
    public String name;

    private Item() {
        // JAXB constructor
    }

    public Item(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}