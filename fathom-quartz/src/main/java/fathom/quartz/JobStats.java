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
import java.util.Date;

/**
 * This code was extracted from JavaMelody, heavily refactored, and adapted to Fathom.
 *
 * @author Emeric Vernat
 * @author James Moger
 */
public class JobStats implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String name;
    private long executionCount;
    private long totalExecutionTime;
    private long totalExecutionTimeSquareSum;
    private long maximumExecutionTime;
    private long totalCpuTime;
    private long exceptionCount;
    private Date lastExceptionTime;
    private String stacktrace;

    JobStats(String name) {
        super();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public long getExecutionCount() {
        return executionCount;
    }

    public long getExceptionCount() {
        return exceptionCount;
    }

    public long getTotalExecutionTime() {
        return totalExecutionTime;
    }

    public int getMeanExecutionTime() {
        if (executionCount > 0) {
            return (int) (totalExecutionTime / executionCount);
        }
        return -1;
    }

    public int getStandardDeviation() {
        if (executionCount > 0) {
            return (int) Math.sqrt((totalExecutionTimeSquareSum - (double) totalExecutionTime * totalExecutionTime / executionCount) / (executionCount - 1));
        }
        return -1;
    }

    public long getMaximumExecutionTime() {
        return maximumExecutionTime;
    }

    public long getTotalCpuTime() {
        return totalCpuTime;
    }

    public int getMeanCpuTime() {
        if (executionCount > 0) {
            return (int) (totalCpuTime / executionCount);
        }
        return -1;
    }

    public float getExceptionPercentage() {
        if (executionCount > 0) {
            return Math.round(Math.min(100f * exceptionCount / executionCount, 100f));
        }
        return 0;
    }

    public Date getLastExceptionTime() {
        return lastExceptionTime;
    }

    public String getStacktrace() {
        return stacktrace;
    }

    void addExecution(long executionTime, long cpuTime, String exceptionStacktrace) {
        executionCount++;
        totalExecutionTime += executionTime;
        totalExecutionTimeSquareSum += executionTime * executionTime;
        if (executionTime > maximumExecutionTime) {
            maximumExecutionTime = executionTime;
        }
        totalCpuTime += cpuTime;
        if (exceptionStacktrace != null) {
            exceptionCount++;
            lastExceptionTime = new Date();
            stacktrace = exceptionStacktrace;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[name=" + getName() + ", executionCount=" + getExecutionCount() + ']';
    }
}
