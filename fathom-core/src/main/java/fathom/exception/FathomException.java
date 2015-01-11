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
package fathom.exception;

import com.google.common.base.Throwables;

/**
 * Base class for all Fathom exceptions.
 *
 * @author James Moger
 */
public class FathomException extends RuntimeException {

    public FathomException(String message, Object... parameters) {
        super(format(message, parameters));
    }

    public FathomException(Throwable cause, String message, Object... parameters) {
        super(format(message, parameters), Throwables.getRootCause(cause));
    }

    public FathomException(Throwable cause) {
        super(Throwables.getRootCause(cause));
    }

    protected static String format(String message, Object... parameters) {
        message = message.replaceAll("\\{\\}", "%s");
        return String.format(message, parameters);
    }
}
