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
 * * This code was extracted from JavaMelody, heavily refactored, and adapted to Fathom.
 *
 * @author Emeric Vernat
 * @author James Moger
 */
public class JobError implements Serializable {

    private static final long serialVersionUID = 1L;
    private final long time;
    private final String name;
    private final String message;
    private final String stacktrace;

    JobError(String name, String message, String stacktrace) {
        super();
        this.time = System.currentTimeMillis();
        this.name = name;
        this.message = message;
        this.stacktrace = stacktrace;
    }

    public long getTime() {
        return time;
    }

    public Date getDate() {
        return new Date(time);
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public String getStacktrace() {
        return stacktrace;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[job=" + getName() + ']';
    }
}
