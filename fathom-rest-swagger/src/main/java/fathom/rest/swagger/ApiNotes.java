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
package fathom.rest.swagger;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for notes on a controller method.
 *
 * Unless otherwise specified, a Markdown resource file named
 * classpath:swagger/com/package/ControllerClass/method.md
 * will be loaded and inserted as the Swagger Operation notes.
 *
 * @author James Moger
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ApiNotes {

    /**
     * Markdown text notes OR a classpath resource file.
     *
     * @ApiNotes("this is my note")
     * @ApiNotes("classpath:swagger/info.md"
     */
    String value() default "";

    /**
     * Notes localization key for messages.properties lookup.
     *
     * If this value is non-empty, a localized variant of the notes will be retrieved
     * from messages.properties with a fallback to value().
     */
    String key() default "";
}
