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

import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This code was extracted from JavaMelody, heavily refactored, and adapted to Fathom.
 *
 * @author Emeric Vernat
 * @author James Moger
 */
public class JobInfo implements Serializable, Comparable<JobInfo> {

    private static final long serialVersionUID = 1L;
    private final String group;
    private final String name;
    private final String description;
    private final String jobClassName;
    private final Date previousFireTime;
    private final Date nextFireTime;
    private final long elapsedTime;
    private final long repeatInterval;
    private final String cronExpression;
    private final boolean paused;
    private JobStats jobStats;

    JobInfo(JobDetail jobDetail, JobExecutionContext jobExecutionContext, Scheduler scheduler) throws SchedulerException {
        this.group = jobDetail.getKey().getGroup();
        this.name = jobDetail.getKey().getName();
        this.description = jobDetail.getDescription();
        this.jobClassName = jobDetail.getJobClass().getName();
        if (jobExecutionContext == null) {
            elapsedTime = -1;
        } else {
            elapsedTime = System.currentTimeMillis() - jobExecutionContext.getFireTime().getTime();
        }

        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobDetail.getKey());
        this.nextFireTime = getNextFireTime(triggers);
        this.previousFireTime = getPreviousFireTime(triggers);

        String cronTriggerExpression = null;
        long simpleTriggerRepeatInterval = -1;
        boolean jobPaused = true;
        for (Trigger trigger : triggers) {
            if (trigger instanceof CronTrigger) {
                cronTriggerExpression = ((CronTrigger) trigger).getCronExpression();
            } else if (trigger instanceof SimpleTrigger) {
                simpleTriggerRepeatInterval = ((SimpleTrigger) trigger).getRepeatInterval();
            }

            jobPaused = jobPaused && scheduler.getTriggerState(trigger.getKey()) == Trigger.TriggerState.PAUSED;
        }

        this.repeatInterval = simpleTriggerRepeatInterval;
        this.cronExpression = cronTriggerExpression;
        this.paused = jobPaused;
    }

    String getFullName() {
        return group + "." + name;
    }

    Date getPreviousFireTime(List<? extends Trigger> triggers) {
        Date previousFireTime = null;
        for (Trigger trigger : triggers) {
            Date triggerPreviousFireTime = trigger.getPreviousFireTime();
            if (previousFireTime == null || triggerPreviousFireTime != null && previousFireTime.before(triggerPreviousFireTime)) {
                previousFireTime = triggerPreviousFireTime;
            }
        }
        return previousFireTime;
    }

    Date getNextFireTime(List<? extends Trigger> triggers) {
        Date nextFireTime = null;
        for (Trigger trigger : triggers) {
            Date triggerNextFireTime = trigger.getNextFireTime();
            if (nextFireTime == null || triggerNextFireTime != null && nextFireTime.after(triggerNextFireTime)) {
                nextFireTime = triggerNextFireTime;
            }
        }
        return nextFireTime;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public String getDescription() {
        return description;
    }

    public String getJobClassName() {
        return jobClassName;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public boolean isExecuting() {
        return elapsedTime >= 0;
    }

    public Date getNextFireTime() {
        return nextFireTime;
    }

    public Date getPreviousFireTime() {
        return previousFireTime;
    }

    public Date getLastExceptionTime() {
        return jobStats == null ? null : jobStats.getLastExceptionTime();
    }

    public long getRepeatInterval() {
        return repeatInterval;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean hasError() {
        return jobStats != null && jobStats.getStacktrace() != null;
    }

    public JobStats getJobStats() {
        return jobStats;
    }

    public void setJobStats(JobStats stats) {
        this.jobStats = stats;
    }

    public long getExecutionCount() {
        return jobStats == null ? 0 : jobStats.getExecutionCount();
    }

    public long getExceptionCount() {
        return jobStats == null ? 0 : jobStats.getExceptionCount();
    }

    public float getExceptionPercentage() {
        return jobStats == null ? 0 : jobStats.getExceptionPercentage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[name=" + getName() + ", group=" + getGroup() + ']';
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(JobInfo job2) {
        return getName().compareToIgnoreCase(job2.getName());
    }

    public static List<JobInfo> buildJobInfoList(Scheduler scheduler) {
        List<JobInfo> result = new ArrayList<>();
        try {
            Map<String, JobExecutionContext> currentlyExecutingJobsByFullName = new LinkedHashMap<>();
            for (JobExecutionContext currentlyExecutingJob : scheduler.getCurrentlyExecutingJobs()) {
                JobDetail jobDetail = currentlyExecutingJob.getJobDetail();
                String jobFullName = getFullName(jobDetail);
                currentlyExecutingJobsByFullName.put(jobFullName, currentlyExecutingJob);
            }
            for (JobDetail jobDetail : getAllJobsOfScheduler(scheduler)) {
                String jobFullName = getFullName(jobDetail);
                JobExecutionContext jobExecutionContext = currentlyExecutingJobsByFullName.get(jobFullName);
                result.add(new JobInfo(jobDetail, jobExecutionContext, scheduler));
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    private static List<JobDetail> getAllJobsOfScheduler(Scheduler scheduler) throws SchedulerException {
        List<JobDetail> result = new ArrayList<>();
        for (String jobGroupName : scheduler.getJobGroupNames()) {
            GroupMatcher<JobKey> groupMatcher = GroupMatcher.groupEquals(jobGroupName);
            for (JobKey jobKey : scheduler.getJobKeys(groupMatcher)) {
                JobDetail jobDetail;
                try {
                    jobDetail = scheduler.getJobDetail(jobKey);
                    if (jobDetail != null) {
                        result.add(jobDetail);
                    }
                } catch (Exception e) {
                    LoggerFactory.getLogger(JobInfo.class).debug(e.toString(), e);
                }
            }
        }
        return result;
    }

    static String getFullName(JobDetail jobDetail) {
        return jobDetail.getKey().getGroup() + '.' + jobDetail.getKey().getName();
    }

}
