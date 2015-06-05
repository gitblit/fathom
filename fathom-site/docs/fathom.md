## Fathom.java

The `conf/Fathom.java` class allows you to customize the final startup of your application.

You are not required to use or even have this class in your classpath.

### Layout

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

/**
 * Class which allows you to perform custom actions at the completion
 * of startup and the beginning of shutdown.
 */
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
