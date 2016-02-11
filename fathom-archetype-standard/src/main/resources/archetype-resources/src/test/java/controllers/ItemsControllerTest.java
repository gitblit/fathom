package ${package}.controllers;

import com.jayway.restassured.response.Header;
import fathom.rest.security.aop.RequireToken;
import fathom.test.RestIntegrationTest;
import ${package}.models.Item;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Tests that our simple items api controller generates JSON and XML.
 * Each unit test starts an instance of our Fathom app in TEST mode.
 */
public class ItemsControllerTest extends RestIntegrationTest {

    private final Header testHeader = new Header(RequireToken.DEFAULT, "cafebabe");

    @Test
    public void testGetJSON() {

        // The JSON response should look like:
        //
        // {
        //   "id" : 1,
        //   "name" : "Apples"
        // }

        given().accept(JSON).when().header(testHeader).get("/api/items/{id}", 1).then().body("id", equalTo(1));

    }

    @Test
    public void testGetXML() {

        // The XML response should look like:
        //
        // <item id="1">
        //   <name>Apples</name>
        // </item>

        given().accept(XML).when().header(testHeader).get("/api/items/{id}", 1).then().body("item.@id", equalTo("1"));

    }

    @Test
    public void testGetObject() {

        // The JSON response should look like:
        //
        // {
        //   "id" : 1,
        //   "name" : "Apples"
        // }

        Item item = given().accept(JSON).when().header(testHeader).get("/api/items/{id}", 1).as(Item.class);
        assertEquals("Item id does not match", 1, item.getId());
        assertEquals("Item name does not match", "Apples", item.getName());
    }
}