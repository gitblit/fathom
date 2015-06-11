## Fathom.java

The `conf/Fathom.java` class allows you to customize the final startup of your application.

!!! Warning "Not Required"
    You are not required to have this class on your classpath.

### Layout

```
YourApp
└── src
    └── main
        └── java
            └── conf
                └── Fathom.java
```
!!! Note
    This *class* depends on the value of the `application.package` setting.  If you have specified an application package then your Fathom class must be `${package}/conf/Fathom.java`.


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
