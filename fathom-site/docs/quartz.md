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
                └── quartz.properties
```

## Configuration

By default, this module will try to configure Quartz from a `conf/quartz.properties` file, if it exists.

## Usage

Create a `conf/Jobs.java` class.

```java
package conf;

/**
 * This class is used to conveniently schedule your Quartz jobs.
 */
public class Jobs extends JobsModule {

    @Override
    protected void schedule() {

        if (getSettings().isProd()) {
            scheduleJob(ProdJob.class).withCronExpression("0/60 * * * * ?");
        } else {
            scheduleJob(DevJob.class);
        }

    }
```

Then create some `Job` classes.  Jobs can be manually scheduled in the `conf/Jobs.java` class or they can be annotated on each `Job` class.

```java
public class ProdJob implements Job {

    final Logger log = LoggerFactory.getLogger(ProdJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("My PROD job triggered");
    }
}
```

```Java
@Scheduled(jobName = "DEV Job", cronExpression = "0/30 * * * * ?")
public class DevJob implements Job {

    final Logger log = LoggerFactory.getLogger(DevJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("My DEV job triggered");
    }
}
```
[Quartz Scheduler]: http://quartz-scheduler.org/documentation/quartz-2.2.x/quick-start
