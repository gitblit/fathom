## Servlets.java

The `conf/Servlets.java` class allows you to programatically specify servlets and filters to build your URL mappings.

You are not required to use or even have this class in your classpath.  For example, you might decide to use a `Module` that provides an alternative way to specify URL mappings like [Fathom-REST](rest.md).

### Layout

```
YourApp
└── src
    └── main
        └── java
            └── conf
                └── Servlets.java
```

### Configuration

```java
package conf;

/**
 * Class which allows you to bind Filters & Servlets.
 */
public class Servlets extends ServletsModule {

    @Override
    protected void setup() {

      // serve all requests to the application context with MyServlet
      serve("/*").with(MyServlet.class);

      if (getSettings().isDev()) {

        // add an audit filter for the DEV mode
        filter("/*").through(MyAuditFilter.class);

      }

    }

}
```
