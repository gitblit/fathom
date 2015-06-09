## Servlets.java

The `conf/Servlets.java` class is an implicit [ServletsModule](/modules/#fathomservletsmodule) within your Fathom application and allows you to programatically specify servlets and filters to build URL mappings.

This class does not support `@RequireSetting` or mode-specific (`@DEV`, `@TEST`, & `@PROD`) annotations, however the same functionality can be achieved using `getSettings().isDev()`, etc.

You are not required to use or even have this class in your classpath.  For example, you might decide to use a `Module` that provides an alternative way to specify URL mappings like [Fathom-REST](rest.md).

### Layout

This *class* depends on the value of the `application.package` setting.  If you have specified an application package then your Servlets class must be `${package}/conf/Servlets.java`.

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
