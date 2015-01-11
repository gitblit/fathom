package controllers;

import fathom.test.FathomTest;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Tests that our simple api controller generates JSON and XML.
 * Each unit test starts an instance of our Fathom app in TEST mode.
 */
public class ApiControllerTest extends FathomTest {

    @Test
    public void testGetJSON() {

        // The JSON response should look like:
        //
        // {
        //   "id" : 1,
        //   "name" : "Item 1"
        // }

        given().accept(JSON).when().get("/api/{id}", 1).then().body("id", equalTo(1));

    }

    @Test
    public void testGetXML() {

        // The XML response should look like:
        //
        // <item id="1">
        //   <name>Item 1</name>
        // </item>

        given().accept(XML).when().get("/api/{id}", 1).then().body("item.@id", equalTo("1"));

    }
}