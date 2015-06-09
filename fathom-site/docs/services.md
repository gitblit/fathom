# Services

*Services* are singleton components that are *started* when your Fathom application starts and *stopped* when your Fathom application stops.

Many of the available Fathom modules include services such as:

- [Fathom-JCache](jcache.md)
- [Fathom-Mailer](mail.md)
- [Fathom-Metrics](metrics.md)
- [Fathom-Quartz](quartz.md)
- [Fathom-REST](rest.md)
- [Fathom-Security](security.md)

## Configuration

You must define your *Service*...

```java
@Singleton
class EmployeeService implements Service {

  Logger log = LoggerFactory.getLogger(EmployeeService.class);

  @Override
  public int getPreferredStartOrder() {
    return 100;
  }

  @Override
  public void start() {
    log.info("Employee Service started!");
  }

  @Override
  public void stop() {
    log.info("Employee Service stopped!");
  }

}
```

... and you must register your *Service* as a bound component in order for Fathom to be able to properly inject, start, & stop it.

```java
public class Components extends Module {

  @Override
  protected void setup() {

    bind(EmployeeService.class);
    bind(EmployeeDao.class);

  }

}
```

### Preferred Start Order

*Services* are ordered according to their *preferred start order* (lower numbers first) and then started serially.  *Services* are stopped in the reverse of the start order.

For reference, here are the start orders of several Fathom modules. This may be helpful in deciding how to order your modules.  In general, you should probably order your *Services* after all dependent services and components have been started.

| Service         | Preferred Start Order |
|-----------------|-----------------------|
| Fathom-JCache   | 10                    |
| Fathom-Metrics  | 50                    |
| Fathom-Security | 50                    |
| Fathom-Quartz   | 50                    |
| Fathom-Mailer   | 50                    |
| Fathom-REST     | 100                   |

### Requiring Settings

Your *service* may need one or more settings to function and you may specify them as annotated requirements.

Each required setting must be present in the runtime profile [configuration](configuration.md) and must have a non-empty value otherwise the *service* will not be registered.

```java
@RequireSetting("employeeService.url")
@RequireSetting("employeeService.username")
@RequireSetting("employeeService.password")
@Singleton
class EmployeeService implements Service {

  Logger log = LoggerFactory.getLogger(EmployeeService.class);

  @Override
  public int getPreferredStartOrder() {
    return 100;
  }

  @Override
  public void start() {
    log.info("Employee Service started!");
  }

  @Override
  public void stop() {
    log.info("Employee Service stopped!");
  }

}
```

### Requiring Modes

You might only want to load your *service* in a particular runtime *mode*. This is easily accomplished by using one or more of the mode-specific annotations: `@DEV`, `@TEST`, and `@PROD`.

```java
@DEV @TEST
@Singleton
class DebugService implements Service {

  Logger log = LoggerFactory.getLogger(DebugService.class);

  @Override
  public int getPreferredStartOrder() {
    return 100;
  }

  @Override
  public void start() {
    log.info("Debug Service started!");
  }

  @Override
  public void stop() {
    log.info("Debug Service stopped!");
  }

}
```

## Usage

Inject your *Service* where you need it.

```java
@Singleton
public void EmployeeDao {

  @Inject
  EmployeeService employeeService;
}
```
