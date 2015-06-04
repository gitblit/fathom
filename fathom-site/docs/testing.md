## About

**Fathom-test-tools** provides a JUnit4 `FathomTest` class for starting your application in TEST mode on a randomly available http port.  

[Rest-Assured](https://code.google.com/p/rest-assured) is automatically configured for this instance of your application allowing you to easily make http requests to your test instance and focus on unit and integration testing of your code.

## Installation

Add the **fathom-test-tools** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-test-tools</artifactId>
    <version>${fathom.version}</version>
    <scope>test</scope>
</dependency>
```

## Usage

```java
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
```
