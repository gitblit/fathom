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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * This code was extracted from JavaMelody, heavily refactored, and adapted to Fathom.
 *
 * @author Emeric Vernat
 * @author James Moger
 */
public class ThreadInfo {

    private static final ThreadMXBean THREAD_BEAN = ManagementFactory.getThreadMXBean();
    private static final boolean CPU_TIME_ENABLED = THREAD_BEAN.isThreadCpuTimeSupported() && THREAD_BEAN.isThreadCpuTimeEnabled();

    public static long getCurrentThreadCpuTime() {
        return getThreadCpuTime(Thread.currentThread().getId());
    }

    public static long getThreadCpuTime(long threadId) {
        long cpuTime;
        if (CPU_TIME_ENABLED) {
            cpuTime = THREAD_BEAN.getThreadCpuTime(threadId);
        } else {
            cpuTime = 0;
        }
        return cpuTime;
    }
}
