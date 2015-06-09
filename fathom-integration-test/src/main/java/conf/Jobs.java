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

package conf;

import com.google.inject.Inject;
import dao.ItemDao;
import fathom.conf.DEV;
import fathom.conf.PROD;
import fathom.conf.TEST;
import fathom.quartz.JobsModule;
import fathom.quartz.Scheduled;
import models.Item;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to conveniently schedule your Quartz jobs.
 *
 * @author James Moger
 */
public class Jobs extends JobsModule {

    @Override
    protected void schedule() {

        scheduleJob(ProdJob.class);
        scheduleJob(DevJob.class);

    }

    @Scheduled(jobName = "PROD Job", cronExpression = "0/60 * * * * ?")
    @PROD
    @TEST
    private static class ProdJob implements Job {

        final Logger log = LoggerFactory.getLogger(ProdJob.class);

        @Inject
        ItemDao itemDao;

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            Item item = itemDao.get(2);
            log.debug("My PROD job triggered, got item {} (#{})", item.getName(), item.getId());
        }
    }

    @Scheduled(jobName = "DEV Job", cronExpression = "0/30 * * * * ?")
    @DEV
    private static class DevJob implements Job {

        final Logger log = LoggerFactory.getLogger(DevJob.class);

        @Inject
        ItemDao itemDao;

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            Item item = itemDao.get(1);
            log.debug("My DEV job triggered, got item {} (#{})", item.getName(), item.getId());
        }
    }
}
