## Components.java

In all the but the simplest of applications you will want to specify re-usable and injectable components.  The `conf/Components.java` class is where you should bind your DAOs, managers, and services.

You are not required to use or even have this class in your classpath - but it's very likely that you will.

### Layout

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

/**
 * Class which allows you to bind your own DAOs, Services, etc.
 */
public class Components extends Module {

    @Override
    protected void setup() {

        bind(ItemDao.class);
        bind(EmployeeDao.class);

        if (getSettings().isDev()) {
          // bind something extra in DEV mode
        }
    }
}
```
