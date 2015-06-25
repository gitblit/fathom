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
 * Annotation for a model property.
 *
 * @author James Moger
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ApiProperty {

    /**
     * Name of the property.
     */
    String name() default "";

    /**
     * Brief description of the property.
     */
    String description() default "";

    /**
     * Description localization key for messages.properties lookup.
     *
     * If this value is non-empty, a localized variant of the description will be retrieved
     * from messages.properties with a fallback to value().
     */
    String descriptionKey() default "";

    /**
     * The default value of the property.
     */
    String defaultValue() default "";

    /**
     * A brief example of the property.
     */
    String example() default "";

    /**
     * Identifies properties that may not be updated through a POST, PUT, or PATCH request.
     */
    boolean readOnly() default false;
}
