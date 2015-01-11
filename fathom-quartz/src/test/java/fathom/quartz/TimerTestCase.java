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

package fathom.quartz;

import com.google.inject.Inject;
import com.google.inject.Module;
import org.apache.onami.test.OnamiRunner;
import org.apache.onami.test.annotation.GuiceProvidedModules;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.Scheduler;

import static org.junit.Assert.assertTrue;

/**
 *
 */
@RunWith(OnamiRunner.class)
public class TimerTestCase {

    @Inject
    private TimedTask timedTask;
    @Inject
    private Scheduler scheduler;

    @GuiceProvidedModules
    public static Module createTestModule() {
        return new JobsModule() {

            @Override
            protected void schedule() {
                scheduleJob(TimedTask.class);
            }

        };
    }

    public void setTimedTask(TimedTask timedTask) {
        this.timedTask = timedTask;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Before
    public void startup() throws Exception {
        this.scheduler.start();
    }

    @After
    public void tearDown() throws Exception {
        this.scheduler.shutdown();
    }

    @Test
    public void minimalTest() throws Exception {
        Thread.sleep(5000);
        assertTrue(this.timedTask.getInvocationsTimedTaskA() > 0);
    }

}