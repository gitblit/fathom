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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that defines a group of API Operations.
 * This annotation is applied to Fathom Controllers.
 *
 * @author James Moger
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApiOperations {

    /**
     * The tag to apply to all Operations.
     * e.g. @ApiOperations(tag="products", description="Operations about products")
     */
    String tag() default "";

    /**
     * Brief description of the group of API Operations.
     */
    String description();

    /**
     * Description localization key for messages.properties lookup.
     *
     * If this value is non-empty, a localized variant of the description will be retrieved
     * from messages.properties with a fallback to description().
     */
    String descriptionKey() default "";

    /**
     * URL for external documentation about this group of API Operations.
     */
    String externalDocs() default "";
}
