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
import com.google.inject.Singleton;
import fathom.Service;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quartz starts and stops the Quartz Scheduler.
 */
@Singleton
public class Quartz implements Service {

    private static final Logger log = LoggerFactory.getLogger(Quartz.class);

    @Inject
    Scheduler scheduler;

    @Override
    public int getPreferredStartOrder() {
        return 50;
    }

    @Override
    public void start() {
        try {
            log.debug("Starting Quartz scheduler");
            scheduler.start();
        } catch (SchedulerException e) {
            log.error("Failed to start Quartz scheduler", e);
        }
    }

    @Override
    public boolean isRunning() {
        try {
            return scheduler.isStarted();
        } catch (SchedulerException e) {
            return false;
        }
    }

    @Override
    public void stop() {
        try {
            log.debug("Stopping Quartz scheduler");
            scheduler.shutdown();
        } catch (SchedulerException e) {
            log.error("Failed to stop Quartz scheduler", e);
        }
    }
}