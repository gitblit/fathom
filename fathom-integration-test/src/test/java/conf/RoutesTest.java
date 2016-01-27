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

import fathom.test.XmlRpcIntegrationTest;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.get;

public class RoutesTest extends XmlRpcIntegrationTest {

    @Test
    public void testIndex() {
        get("/").then().assertThat().statusCode(200);
    }

    @Test
    public void testException() {
        get("/internalError").then().assertThat().statusCode(500);
    }

    @Test
    public void testXmlrpcInsecureMinAsAnon() {
        int value = callAnon("insecure.min", 1, 2);
        assertEquals("Unexpected minimum value!", 1, value);
    }

    @Test
    public void testXmlrpcInsecureItemNameAsAnon() {
        String name = callAnon("insecure.nameOfItem", 1);
        assertEquals("Unexpected item name!", "Apples", name);
    }

    @Test(expected = RuntimeException.class)
    public void testXmlrpcSecureMinAsAnon() {
        int value = callAnon("secure.min", 1, 2);
        assertEquals("Unexpected minimum value!", 1, value);
    }

    @Test
    public void testXmlrpcSecureMinAsUser() {
        int value = callAuth("secure.min", 1, 2);
        assertEquals("Unexpected minimum value!", 1, value);
    }

    @Test(expected = RuntimeException.class)
    public void testXmlrpcSecureItemNameAsAnon() {
        String name = callAnon("secure.nameOfItem", 1);
        assertEquals("Unexpected item name!", "Apples", name);
    }

    @Test
    public void testXmlrpcSecureItemNameAsUser() {
        String name = callAuth("secure.nameOfItem", 1);
        assertEquals("Unexpected item name!", "Apples", name);
    }

    protected <X> X callAuth(String methodName, Object... args) {
        return call("admin", "admin", "/RPC2", methodName, args);
    }

}