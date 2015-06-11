## About

**Fathom-Quartz** provides [Quartz Scheduler] integration for your application.  Quartz allows you to easily setup scheduled tasks within your application.

This is a fork of the [Apache Onami Scheduler](https://onami.apache.org/scheduler).

## Installation

Add the **Fathom-Quartz** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-quartz</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

## Layout

```
YourApp
└── src
    └── main
        └── java
            └── conf
                ├── Jobs.java
                └── quartz.properties
```
!!! Note
    This *module* depends on the value of the `application.package` setting.  If you have specified an application package then your Jobs class must be `${package}/conf/Jobs.java`.

## Configuration

By default, this module will try to configure Quartz from a `conf/quartz.properties` resource file.

## Usage

Create a `conf/Jobs.java` class.  Jobs can be manually scheduled in the `conf/Jobs.java` class or they can be annotated on each `Job` class.

```java
package conf;

public class Jobs extends JobsModule {

  @Override
  protected void schedule() {

    scheduleJob(MyJob.class);
    scheduleJob(OtherJob.class).withCronExpression("0/60 * * * * ?");

  }

}
```

Then create some `Job` classes.  `Job` classes support injection.

```java
@Scheduled(jobName = "My Job", cronExpression = "0/60 * * * * ?")
public class MyJob implements Job {

  final Logger log = LoggerFactory.getLogger(MyJob`.class);

  @Inject
  EmployeeDao employeeDao;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    int count = employeeDao.getAll().size();
    log.debug("My job triggered, {} employees in system", count);
  }

}
```

### Requiring Settings

Your *job* may need one or more settings to function and you may specify them as annotated requirements.

Each required setting must be present in the runtime profile [configuration](configuration.md) and must have a non-empty value otherwise the *job* will not be registered.

```java
@RequireSetting("myjob.url")
@Scheduled(jobName = "My Job", cronExpression = "0/60 * * * * ?")
public class MyJob implements Job {

  final Logger log = LoggerFactory.getLogger(MyJob.class);

  @Inject
  Settings settings;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    String url = settings.getString("myjob.url");
    log.debug("My job triggered {}", url);
  }

}
```

### Requiring Modes

You might only want to load your *job* in a particular runtime *mode*. This is easily accomplished by using one or more of the mode-specific annotations: `@DEV`, `@TEST`, and `@PROD`.

```java
@Scheduled(jobName = "DEV Job", cronExpression = "0/30 * * * * ?")
@DEV
public class DevJob implements Job {

  final Logger log = LoggerFactory.getLogger(DevJob.class);

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    log.debug("My DEV job triggered");
  }

}
```

[Quartz Scheduler]: http://quartz-scheduler.org/documentation/quartz-2.2.x/quick-start
