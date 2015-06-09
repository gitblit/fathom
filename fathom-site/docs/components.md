## Components.java

In all the but the simplest of applications you will want to specify re-usable and injectable components.  The `conf/Components.java` class is an implicit [Module](/modules/#fathommodule) within your Fathom application and where you should bind your DAOs, managers, and *services*.

This class does not support `@RequireSetting` or mode-specific (`@DEV`, `@TEST`, & `@PROD`) annotations, however the same functionality can be achieved using `getSettings().isDev()`, etc.

You are not required to use or even have this class in your classpath - but it's very likely that you will.

### Layout

This *class* depends on the value of the `application.package` setting.  If you have specified an application package then your Components class must be `${package}/conf/Components.java`.

```
YourApp
└── src
    └── main
        └── java
            └── conf
                └── Components.java
```

### Configuration

```java
package conf;

public class Components extends Module {

    @Override
    protected void setup() {

        bind(EmployeeService.class);
        bind(EmployeeDao.class);

    }

}
```
