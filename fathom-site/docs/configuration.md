## Configuration

### Components

```Java
package conf;

import fathom.Module;

public class Components extends Module {

    @Override
    protected void setup() {

        bind(ItemDao.class);
        bind(EmployeeDao.class);

        // we have to manually specify our static controllers
        // for injection, if we choose that design
        requestStaticInjection(HelloStaticRoutes.class);

    }
}
```

### Servlets

```Java
package conf;

import fathom.ServletsModule;

/**
 * Class which allows you to bind your own servlets.
 */
public class Servlets extends ServletsModule {

    @Override
    protected void setup() {
    }

}
```
