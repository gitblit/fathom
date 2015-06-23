package ${package}.controllers;

import com.jayway.restassured.response.Header;
import fathom.rest.security.aop.RequireToken;
import fathom.test.FathomTest;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Tests that our simple items api controller generates JSON and XML.
 * Each unit test starts an instance of our Fathom app in TEST mode.
 */
public class ItemsControllerTest extends FathomTest {

    private final Header testHeader = new Header(RequireToken.DEFAULT, "cafebabe");

    @Test
    public void testGetJSON() {

        // The JSON response should look like:
        //
        // {
        //   "id" : 1,
        //   "name" : "Item 1"
        // }

        given().accept(JSON).when().header(testHeader).get("/api/items/{id}", 1).then().body("id", equalTo(1));

    }

    @Test
    public void testGetXML() {

        // The XML response should look like:
        //
        // <item id="1">
        //   <name>Item 1</name>
        // </item>

        given().accept(XML).when().header(testHeader).get("/api/items/{id}", 1).then().body("item.@id", equalTo("1"));

    }
}