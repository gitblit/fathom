/*
 * Copyright (C) 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ${package}.conf;

import fathom.quartz.JobsModule;
import fathom.quartz.Scheduled;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
