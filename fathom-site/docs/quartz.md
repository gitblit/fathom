## About

**Fathom-Quartz** provides [Quartz Scheduler]() integration for your application.

## Installation

Add the **Fathom-Quartz** artifact.

```XML
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-quartz</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

## Configuration

```Java
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

    private static class ProdJob implements Job {

        final Logger log = LoggerFactory.getLogger(ProdJob.class);

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            log.debug("My PROD job triggered");
        }
    }

    @Scheduled(jobName = "DEV Job", cronExpression = "0/30 * * * * ?")
    private static class DevJob implements Job {

        final Logger log = LoggerFactory.getLogger(DevJob.class);

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            log.debug("My DEV job triggered");
        }
    }
}
```
