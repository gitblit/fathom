package ${package}.conf;

import fathom.test.FathomIntegrationTest;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Tests that our index page is generated.
 * Each unit test starts an instance of our Fathom app in TEST mode.
 */
public class RoutesTest extends FathomIntegrationTest {

    @Test
    public void testIndex() {

        get("/").then().assertThat().statusCode(200);

    }

}