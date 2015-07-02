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

import fathom.test.FathomIntegrationTest;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class ApiControllerTest extends FathomIntegrationTest {

    @Test
    public void testGetJSON() {

        // The JSON response should look like:
        //
        // {
        //   "id" : 1,
        //   "name" : "Item 1"
        // }

        given().accept(JSON).when().get("/api/v1/items/{id}", 1).then().body("id", equalTo(1));

    }

    @Test
    public void testGetXML() {

        // The XML response should look like:
        //
        // <item id="1">
        //   <name>Item 1</name>
        // </item>

        given().accept(XML).when().get("/api/v1/items/{id}", 1).then().body("item.@id", equalTo("1"));

    }
}