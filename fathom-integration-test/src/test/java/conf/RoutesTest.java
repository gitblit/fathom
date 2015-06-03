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

package conf;

import fathom.test.FathomTest;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.get;

public class RoutesTest extends FathomTest {

    @Test
    public void testIndex() {
        get("/").then().assertThat().statusCode(200);
    }

    @Test
    public void testException() {
        get("/internalError").then().assertThat().statusCode(500);
    }

}