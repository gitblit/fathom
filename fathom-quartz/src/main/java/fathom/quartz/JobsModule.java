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

import com.google.common.base.Strings;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import fathom.Module;
import fathom.utils.RequireUtil;
import fathom.utils.Util;
import org.quartz.Job;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerListener;
import org.quartz.TriggerListener;
import org.quartz.spi.JobFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Quartz (http://www.quartz-scheduler.org/) JobsModule as Fathom extension.
 */
public abstract class JobsModule extends Module {

    private Multibinder<JobListener> jobListeners;

    private Multibinder<TriggerListener> triggerListeners;

    private Multibinder<SchedulerListener> schedulerListeners;

    private SchedulerConfiguration schedulerConfiguration;

    private static void checkState(boolean state, String message) {
        if (!state) {
            throw new IllegalStateException(message);
        }
    }

    private static void checkNotNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void setup() {
        checkState(jobListeners == null, "Re-entry is not allowed.");
        checkState(triggerListeners == null, "Re-entry is not allowed.");
        checkState(schedulerListeners == null, "Re-entry is not allowed.");
        checkState(schedulerConfiguration == null, "Re-entry is not allowed.");

        jobListeners = Multibinder.newSetBinder(binder(), JobListener.class);
        triggerListeners = Multibinder.newSetBinder(binder(), TriggerListener.class);
        schedulerListeners = Multibinder.newSetBinder(binder(), SchedulerListener.class);
        schedulerConfiguration = new SchedulerConfiguration();

        // load the quartz config, if present
        if (getSettings() != null) {
            String file = getSettings().getString("quartz.configurationFile", "classpath:conf/quartz.properties");
            if (!Strings.isNullOrEmpty(file)) {
                try (InputStream is = Util.getInputStreamForPath(file)) {
                    Properties properties = new Properties();
                    properties.load(is);
                    schedulerConfiguration.withProperties(properties);
                } catch (IOException e) {
                }
            }
        }

        try {
            schedule();
            bind(JobFactory.class).to(InjectorJobFactory.class).in(Scopes.SINGLETON);
            bind(Scheduler.class).toProvider(SchedulerProvider.class).asEagerSingleton();
            bind(SchedulerConfiguration.class).toInstance(schedulerConfiguration);
        } finally {
            jobListeners = null;
            triggerListeners = null;
            schedulerListeners = null;
            schedulerConfiguration = null;
        }
    }

    /**
     * Part of the EDSL builder language for configuring {@code Job}s.
     * Here is a typical example of scheduling {@code Job}s when creating your Guice injector:
     * <p>
     * <pre>
     * Guice.createInjector(..., new QuartzModule() {
     *
     *     {@literal @}Override
     *     protected void schedule() {
     *       <b>scheduleJob(MyJobImpl.class).withCronExpression("0/2 * * * * ?");</b>
     *     }
     *
     * });
     * </pre>
     *
     * @see JobSchedulerBuilder
     */
    protected abstract void schedule();

    /**
     * Allows to configure the scheduler.
     * <p>
     * <pre>
     * Guice.createInjector(..., new QuartzModule() {
     *
     *     {@literal @}Override
     *     protected void schedule() {
     *       configureScheduler().withManualStart().withProperties(...);
     *     }
     *
     * });
     * </pre>
     *
     * @since 1.1
     */
    protected final SchedulerConfigurationBuilder configureScheduler() {
        return schedulerConfiguration;
    }

    /**
     * Add the {@code JobListener} binding.
     *
     * @param jobListenerType The {@code JobListener} class has to be bound
     */
    protected final void addJobListener(Class<? extends JobListener> jobListenerType) {
        doBind(jobListeners, jobListenerType);
    }

    /**
     * Add the {@code TriggerListener} binding.
     *
     * @param triggerListenerType The {@code TriggerListener} class has to be bound
     */
    protected final void addTriggerListener(Class<? extends TriggerListener> triggerListenerType) {
        doBind(triggerListeners, triggerListenerType);
    }

    /**
     * Add the {@code SchedulerListener} binding.
     *
     * @param schedulerListenerType The {@code SchedulerListener} class has to be bound
     */
    protected final void addSchedulerListener(Class<? extends SchedulerListener> schedulerListenerType) {
        doBind(schedulerListeners, schedulerListenerType);
    }

    /**
     * Allows {@code Job} scheduling, delegating Guice create the {@code Job} instance
     * and inject members.
     * <p>
     * If given {@code Job} class is annotated with {@link Scheduled}, then {@code Job}
     * and related {@code Trigger} values will be extracted from it.
     *
     * @param jobClass The {@code Job} has to be scheduled
     * @return The {@code Job} builder or null if the job class may not be registered
     */
    protected final JobSchedulerBuilder scheduleJob(Class<? extends Job> jobClass) {
        checkNotNull(jobClass, "Argument 'jobClass' must be not null.");

        if (!RequireUtil.allowClass(getSettings(), jobClass)) {
            return null;
        }

        JobSchedulerBuilder builder = new JobSchedulerBuilder(jobClass);

        if (jobClass.isAnnotationPresent(Scheduled.class)) {
            Scheduled scheduled = jobClass.getAnnotation(Scheduled.class);

            builder
                    // job
                    .withJobName(scheduled.jobName())
                    .withJobGroup(scheduled.jobGroup())
                    .withRequestRecovery(scheduled.requestRecovery())
                    .withStoreDurably(scheduled.storeDurably())
                            // trigger
                    .withCronExpression(scheduled.cronExpression())
                    .withTriggerName(scheduled.triggerName());

            if (!Scheduled.DEFAULT.equals(scheduled.timeZoneId())) {
                TimeZone timeZone = TimeZone.getTimeZone(scheduled.timeZoneId());
                if (timeZone != null) {
                    builder.withTimeZone(timeZone);
                }
            }
        }

        requestInjection(builder);
        return builder;
    }

    /**
     * Utility method to respect the DRY principle.
     *
     * @param <T>
     * @param binder
     * @param type
     */
    protected final <T> void doBind(Multibinder<T> binder, Class<? extends T> type) {
        checkNotNull(type, "Impossible to bind a null type");
        binder.addBinding().to(type);
    }

}