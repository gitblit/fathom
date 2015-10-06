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

import java.io.Serializable;

/**
 * This code was extracted from JavaMelody, heavily refactored, and adapted to Fathom.
 *
 * @author Emeric Vernat
 * @author James Moger
 */
class JobStatsContext implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String jobFullName;
    private final long threadId;
    private final long startTime;
    private final long startCpuTime;

    JobStatsContext(String jobFullName, long threadId, long startTime, long startCpuTime) {
        super();
        assert jobFullName != null;

        this.jobFullName = jobFullName;
        this.threadId = threadId;
        this.startTime = startTime;
        this.startCpuTime = startCpuTime;
    }

    String getJobFullName() {
        return jobFullName;
    }

    long getThreadId() {
        return threadId;
    }

    int getDuration(long timeOfSnapshot) {
        return (int) Math.max(timeOfSnapshot - startTime, 0);
    }

    int getCpuTime() {
        if (startCpuTime < 0) {
            return -1;
        }
        final int cpuTime = (int) (ThreadInfo.getThreadCpuTime(getThreadId()) - startCpuTime) / 1000000;
        return Math.max(cpuTime, 0);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[jobFullName=" + jobFullName + ", threadId=" + getThreadId() + ", startTime=" + startTime + ']';
    }
}
