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
package fathom.rest.controller;

import fathom.rest.controller.extractors.AuthExtractor;
import fathom.rest.controller.extractors.ExtractWith;
import fathom.rest.security.AuthConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that identifies that an Account object should be extracted from the session.
 * If the session does not exist or the request is unauthenticated then the Guest Account
 * will be extracted.
 *
 * @author James Moger
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@ExtractWith(AuthExtractor.class)
public @interface Auth {
    String value() default AuthConstants.ACCOUNT_ATTRIBUTE;
}
