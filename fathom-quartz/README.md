Fathom Quartz Scheduler
=====================

Fathom Module for the Quartz Scheduler.

This is a fork of [Apache Onami Scheduler](https://onami.apache.org/scheduler)

Setup
-----

1) Add the dependency to your pom.xml:
```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-quartz</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

2) Create a `conf/Jobs` class.
```java
public class Jobs extends JobsModule {

    @Override
    protected void schedule() {

        if (getSettings().isProd()) {
            scheduleJob(ProdJob.class).withCronExpression("0/60 * * * * ?");
        } else {
            scheduleJob(DevJob.class);
        }

    }
}
```

Usage
-----

By default, this module will try to configure Quartz from a `conf/quartz.properties` file, if it exists.

Jobs
----
Don't create jobs instances manually, let Guice do the job for you!
`org.quartz.Job` instances and scheduling are managed as well, a typical example of scheduling `org.quartz.Job` is:

```java
public class Jobs extends JobsModule {

    @Override
    protected void schedule() {
       ...
       scheduleJob(com.acme.MyJobImpl.class).withCronExpression( "0/2 * * * * ?" );
       ...
   }

});
```

Annotated Scheduling

Job classes annotated with `fathom.quartz.Scheduled` will be automatically scheduled.

```java
@Singleton
@Scheduled(jobName = "test", cronExpression = "0/2 * * * * ?")
public class com.acme.MyJobImpl implements org.quartz.Job {

    @Inject
    private MyCustomService service;

    public void execute(JobExecutionContext context) throws JobExecutionException {
        service.customOperation();
    }

}
```

[1]: http://quartz-scheduler.org/documentation/quartz-2.2.x/quick-start