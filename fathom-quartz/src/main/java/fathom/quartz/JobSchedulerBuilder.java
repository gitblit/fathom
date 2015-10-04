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
import com.google.inject.ProvisionException;
import org.quartz.Job;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import java.util.TimeZone;

import static java.lang.String.format;
import static java.util.TimeZone.getDefault;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;
import static org.quartz.utils.Key.DEFAULT_GROUP;

/**
 * DSL to produce {@code Job} and add to a {@code Scheduler},
 * and associate the related {@code Trigger} with it.
 */
public final class JobSchedulerBuilder {

    /**
     * The type of the {@code Job} to be executed.
     */
    private final Class<? extends Job> jobClass;

    /**
     * The {@code Job} name, must be unique within the group.
     */
    private String jobName = Scheduled.DEFAULT;

    /**
     * The {@code Job} description.
     */
    private String jobDescription;

    /**
     * The {@code Job} group name.
     */
    private String jobGroup = DEFAULT_GROUP;

    /**
     * Instructs the {@code Scheduler} whether or not the {@code Job} should
     * be re-executed if a {@code recovery} or {@code fail-over} situation is
     * encountered.
     */
    private boolean requestRecovery = false;

    /**
     * Whether or not the {@code Job} should remain stored after it is
     * orphaned (no {@code Trigger}s point to it).
     */
    private boolean storeDurably = false;

    /**
     * The {@code Trigger} name, must be unique within the group.
     */
    private String triggerName = Scheduled.DEFAULT;

    /**
     * The {@code Trigger} group.
     */
    private String triggerGroup = DEFAULT_GROUP;

    /**
     * The cron expression to base the schedule on.
     */
    private String cronExpression;

    /**
     * The time zone for which the {@code cronExpression}
     * of this {@code CronTrigger} will be resolved.
     */
    private TimeZone timeZone = getDefault();

    /**
     * The {@code Trigger}'s priority.  When more than one {@code Trigger} have the same
     * fire time, the scheduler will fire the one with the highest priority
     * first.
     */
    private int priority = 0;

    /**
     * The {@code Trigger} to be used to schedule the {@code Job}
     *
     * @since 1.2
     */
    private Trigger trigger;

    /**
     * Indicates whether the job's trigger should be updated if it is already existing when being
     * scheduled (which is typically the case with a persistent {@link org.quartz.spi.JobStore}.
     *
     * @since 1.3
     */
    private boolean updateExistingTrigger = false;

    /**
     * Creates a new {@code JobSchedulerBuilder} instance.
     * <p>
     * This class can't be instantiated by users.
     *
     * @param jobClass The type of the {@code Job} to be executed
     */
    JobSchedulerBuilder(final Class<? extends Job> jobClass) {
        this.jobClass = jobClass;
    }

    /**
     * Sets the {@code Job} name, must be unique within the group.
     *
     * @param jobName The {@code Job} name, must be unique within the group
     * @return This builder instance
     */
    public JobSchedulerBuilder withJobName(String jobName) {
        this.jobName = jobName;
        return this;
    }

    /**
     * Sets the {@code Job} description.
     *
     * @param jobDescription The {@code Job} description
     * @return This builder instance
     */
    public JobSchedulerBuilder withJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
        return this;
    }

    /**
     * Sets the {@code Job} group.
     *
     * @param jobGroup The {@code Job} group
     * @return This builder instance
     */
    public JobSchedulerBuilder withJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
        return this;
    }

    /**
     * Instructs the {@code Scheduler} whether or not the {@code Job} should
     * be re-executed if a {@code recovery} or {@code fail-over} situation is
     * encountered.
     *
     * @param requestRecovery The activation flag
     * @return This builder instance
     */
    public JobSchedulerBuilder withRequestRecovery(boolean requestRecovery) {
        this.requestRecovery = requestRecovery;
        return this;
    }

    /**
     * Whether or not the {@code Job} should remain stored after it is
     * orphaned (no {@code Trigger}s point to it).
     *
     * @param storeDurably The activation flag
     * @return This builder instance
     */
    public JobSchedulerBuilder withStoreDurably(boolean storeDurably) {
        this.storeDurably = storeDurably;
        return this;
    }

    /**
     * Sets the {@code Trigger} name, must be unique within the group.
     *
     * @param triggerName The {@code Trigger} name, must be unique within the group
     * @return This builder instance
     */
    public JobSchedulerBuilder withTriggerName(String triggerName) {
        this.triggerName = triggerName;
        return this;
    }

    /**
     * Sets the {@code Trigger} group.
     *
     * @param triggerGroup The {@code Trigger} group
     * @return This builder instance
     */
    public JobSchedulerBuilder withTriggerGroup(String triggerGroup) {
        this.triggerGroup = triggerGroup;
        return this;
    }

    /**
     * Sets the cron expression to base the schedule on.
     *
     * @param cronExpression The cron expression to base the schedule on
     * @return This builder instance
     */
    public JobSchedulerBuilder withCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
        return this;
    }

    /**
     * Sets the time zone for which the {@code cronExpression} of this
     * {@code CronTrigger} will be resolved.
     *
     * @param timeZone The time zone for which the {@code cronExpression}
     *                 of this {@code CronTrigger} will be resolved.
     * @return This builder instance
     */
    public JobSchedulerBuilder withTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    /**
     * Sets the {@code Trigger}'s priority.  When more than one {@code Trigger} have the same
     * fire time, the scheduler will fire the one with the highest priority
     * first.
     *
     * @param priority The {@code Trigger}'s priority
     * @return This builder instance
     */
    public JobSchedulerBuilder withPriority(int priority) {
        this.priority = priority;
        return this;
    }

    /**
     * Sets the {@code Trigger} that will be used to schedule
     * the {@code Job}.
     * <p>
     * <p>
     * Be aware that using using this method will override any other
     * {@code Trigger}-related operation, like {@link #withTriggerGroup(String)}
     * or {@link #withTimeZone(TimeZone)}
     *
     * @param trigger The {@code Trigger} to associate with the {@code Job}
     * @return This builder instance
     * @since 1.2
     */
    public JobSchedulerBuilder withTrigger(Trigger trigger) {
        this.trigger = trigger;
        return this;
    }

    /**
     * Requests an existing trigger (sharing the same key as the new trigger) for this job to
     * be replaced with the new trigger.
     *
     * @return This builder instance
     * @since 1.3
     */
    public JobSchedulerBuilder updateExistingTrigger() {
        this.updateExistingTrigger = true;
        return this;
    }

    /**
     * Add the produced {@code Job} to the given {@code Scheduler},
     * and associate the related {@code Trigger} with it.
     * <p>
     * Users <b>MUST NOT</b> use this method!
     *
     * @param scheduler The given {@code Scheduler}
     * @throws Exception If any error occurs
     */
    @Inject
    public void schedule(Scheduler scheduler)
            throws Exception {
        if (cronExpression == null && trigger == null) {
            throw new ProvisionException(format("Impossible to schedule Job '%s' without cron expression",
                    jobClass.getName()));
        }
        if (cronExpression != null && trigger != null) {
            throw new ProvisionException(format("Impossible to schedule Job '%s' with cron expression " +
                    "and an associated Trigger at the same time", jobClass.getName()));
        }

        JobKey jobKey = jobKey(Scheduled.DEFAULT.equals(jobName) ? jobClass.getName() : jobName, jobGroup);
        TriggerKey triggerKey = triggerKey(Scheduled.DEFAULT.equals(triggerName) ? jobClass.getCanonicalName() : triggerName, triggerGroup);

        if (updateExistingTrigger && scheduler.checkExists(triggerKey)) {
            scheduler.unscheduleJob(triggerKey);
        }

        scheduler.scheduleJob(newJob(jobClass)
                        .withIdentity(jobKey)
                        .withDescription(jobDescription)
                        .requestRecovery(requestRecovery)
                        .storeDurably(storeDurably).build(),
                (trigger == null) ?
                        newTrigger()
                                .withIdentity(triggerKey)
                                .withSchedule(cronSchedule(cronExpression)
                                        .inTimeZone(timeZone))
                                .withPriority(priority)
                                .build()
                        : trigger);
    }

}