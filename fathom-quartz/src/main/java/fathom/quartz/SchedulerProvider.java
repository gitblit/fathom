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
import com.google.inject.Provider;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.TriggerListener;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;

import java.util.Set;

/**
 * Provides a {@code Scheduler} instance.
 */
final class SchedulerProvider implements Provider<Scheduler> {

    /**
     * The {@code Scheduler} instance will be provided.
     */
    private final Scheduler scheduler;

    /**
     * Initialized a new {@code Provider&lt;Scheduler&gt;} instance.
     *
     * @throws SchedulerException If any error occurs
     */
    @Inject
    public SchedulerProvider(SchedulerConfiguration schedulerConfiguration) throws SchedulerException {
        StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();

        if (schedulerConfiguration.getProperties() != null) {
            schedulerFactory.initialize(schedulerConfiguration.getProperties());
        }

        this.scheduler = schedulerFactory.getScheduler();
    }

    /**
     * Sets the {@code JobFactory} instance (it will be a {@link InjectorJobFactory} instance).
     *
     * @param jobFactory The {@code JobFactory} instance.
     * @throws SchedulerException If any error occurs
     */
    @Inject
    public void setJobFactory(JobFactory jobFactory) throws SchedulerException {
        scheduler.setJobFactory(jobFactory);
    }

    /**
     * Sets the {@code JobListener}s.
     *
     * @param jobListeners The {@code JobListener}s
     * @throws SchedulerException If any error occurs
     */
    @Inject(optional = true)
    public void addJobListeners(Set<JobListener> jobListeners) throws SchedulerException {
        for (JobListener jobListener : jobListeners) {
            scheduler.getListenerManager().addJobListener(jobListener);
        }
    }

    /**
     * Sets the {@code SchedulerListener}s.
     *
     * @param schedulerListeners The {@code SchedulerListener}s
     * @throws SchedulerException If any error occurs
     */
    @Inject(optional = true)
    public void addSchedulerListeners(Set<SchedulerListener> schedulerListeners) throws SchedulerException {
        for (SchedulerListener schedulerListener : schedulerListeners) {
            scheduler.getListenerManager().addSchedulerListener(schedulerListener);
        }
    }

    /**
     * Sets the {@code TriggerListener}s.
     *
     * @param triggerListeners The {@code TriggerListener}s
     * @throws SchedulerException If any error occurs
     */
    @Inject(optional = true)
    public void addTriggerListeners(Set<TriggerListener> triggerListeners) throws SchedulerException {
        for (TriggerListener triggerListener : triggerListeners) {
            scheduler.getListenerManager().addTriggerListener(triggerListener);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Scheduler get() {
        return scheduler;
    }

}