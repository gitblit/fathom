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
import fathom.conf.Settings;
import fathom.exception.FathomException;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This code was extracted from JavaMelody, heavily refactored, and adapted to Fathom.
 *
 * @author Emeric Vernat
 * @author James Moger
 */
@Singleton
public class JobsMonitor implements JobListener {

    private final Scheduler scheduler;
    private final Map<String, JobStats> stats;
    private final LinkedList<JobError> errors;
    private final ThreadLocal<JobStatsContext> statsContextThreadLocal;
    private final int maxExceptionCount;

    @Inject
    public JobsMonitor(Settings settings, Scheduler scheduler) {
        this.scheduler = scheduler;
        this.statsContextThreadLocal = new ThreadLocal<>();
        this.stats = new ConcurrentHashMap<>();
        this.errors = new LinkedList<>();
        this.maxExceptionCount = settings.getInteger("quartz.bufferLastNExceptions", 50);
    }

    public void pauseJob(JobInfo jobInfo) {
        try {
            scheduler.pauseJob(JobKey.jobKey(jobInfo.getName(), jobInfo.getGroup()));
        } catch (SchedulerException e) {
            throw new FathomException("Failed to pause job {}", jobInfo.getFullName(), e);
        }
    }

    public void resumeJob(JobInfo jobInfo) {
        try {
            scheduler.resumeJob(JobKey.jobKey(jobInfo.getName(), jobInfo.getGroup()));
        } catch (SchedulerException e) {
            throw new FathomException("Failed to resume job {}", jobInfo.getFullName(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        final JobDetail jobDetail = context.getJobDetail();
        final String jobFullName = JobInfo.getFullName(jobDetail);

        JobStatsContext requestContext = new JobStatsContext(jobFullName,
                Thread.currentThread().getId(),
                System.currentTimeMillis(),
                ThreadInfo.getCurrentThreadCpuTime());

        statsContextThreadLocal.set(requestContext);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        statsContextThreadLocal.remove();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        final String message;
        final String stacktrace;
        if (jobException == null) {
            message = null;
            stacktrace = null;
        } else {
            message = jobException.getMessage();
            StringWriter stackTraceWriter = new StringWriter(200);
            jobException.printStackTrace(new PrintWriter(stackTraceWriter));
            stacktrace = stackTraceWriter.toString();
        }

        JobStatsContext statsContext = statsContextThreadLocal.get();
        statsContextThreadLocal.remove();

        if (statsContext != null) {
            addExecution(statsContext, message, stacktrace);
        }
    }

    private void addExecution(JobStatsContext statsContext, String message, String stacktrace) {
        String jobFullName = statsContext.getJobFullName();
        long duration = statsContext.getDuration(System.currentTimeMillis());
        long cpuTime = statsContext.getCpuTime();

        stats.putIfAbsent(jobFullName, new JobStats(jobFullName));
        JobStats jobStats = stats.get(jobFullName);
        synchronized (jobStats) {
            jobStats.addExecution(duration, cpuTime, stacktrace);
        }

        if (stacktrace != null) {
            synchronized (errors) {
                errors.addLast(new JobError(jobFullName, message, stacktrace));
                if (errors.size() > maxExceptionCount) {
                    errors.removeFirst();
                }
            }
        }
    }

    public List<JobError> getErrors() {
        if (errors == null) {
            return Collections.emptyList();
        }
        synchronized (errors) {
            return new ArrayList<>(errors);
        }
    }

    public List<JobInfo> getJobs() {
        List<JobInfo> jobs = JobInfo.buildJobInfoList(scheduler);
        jobs.forEach(jobInfo -> jobInfo.setJobStats(stats.get(jobInfo.getFullName())));
        return jobs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return getClass().getName();
    }
}
