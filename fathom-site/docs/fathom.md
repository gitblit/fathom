## Fathom.java

The `conf/Fathom.java` class allows you to customize the final startup of your application.

You are not required to use or even have this class in your classpath.

### Layout

This *class* depends on the value of the `application.package` setting.  If you have specified an application package then your Fathom class must be `${package}/conf/Fathom.java`.

```
YourApp
└── src
    └── main
        └── java
            └── conf
                └── Fathom.java
```

### Configuration

```java
package conf;

public class Fathom extends Ftm {

  @Override
  public void onStartup() {
    super.onStartup();
    log.info("Woohoo! Fathom is Ready!");
  }

  @Override
  public void onShutdown() {
    log.info("Time to take a break!");
    super.onShutdown();
  }

}
```
