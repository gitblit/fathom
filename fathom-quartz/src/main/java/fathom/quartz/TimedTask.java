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

import com.google.inject.Singleton;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 *
 */
@Singleton
@Scheduled(jobName = "test", cronExpression = "0/2 * * * * ?")
public class TimedTask
        implements Job {

    private int invocationsA = 0;

    public int getInvocationsTimedTaskA() {
        return this.invocationsA;
    }

    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        this.invocationsA++;
    }

}