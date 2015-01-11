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

import java.util.Properties;

import static org.junit.Assert.assertEquals;

@RunWith(OnamiRunner.class)
public class WithPropertiesTestCase {

    private static final String INSTANCE_NAME = "fathom-quartz";
    @Inject
    private Scheduler scheduler;

    @GuiceProvidedModules
    public static Module createTestModule() {
        return new JobsModule() {

            @Override
            protected void schedule() {
                Properties properties = new Properties() {
                    private static final long serialVersionUID = 1L;

                    {
                        put("org.quartz.scheduler.instanceName", INSTANCE_NAME);
                        put("org.quartz.threadPool.class", "org.quartz.simpl.ZeroSizeThreadPool");
                    }
                };
                configureScheduler().withProperties(properties);
            }

        };
    }

    @Before
    public void startup() throws Exception {
        scheduler.start();
    }

    @After
    public void tearDown() throws Exception {
        scheduler.shutdown();
    }

    @Test
    public void testPropertiesConfiguredInstanceName() throws Exception {
        assertEquals(scheduler.getSchedulerName(), INSTANCE_NAME);
    }

}