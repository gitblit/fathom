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

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import fathom.rest.controller.Body;
import fathom.rest.controller.Controller;
import fathom.rest.controller.Required;
import io.swagger.models.ExternalDocs;

import java.lang.reflect.Parameter;

/**
 * @author James Moger
 */
public class SwaggerUtil {

    /**
     * Determines if a parameter is Required or not.
     *
     * @param parameter
     * @return true if the parameter is required
     */
    public static boolean isRequired(Parameter parameter) {
        return parameter.isAnnotationPresent(Body.class)
                || parameter.isAnnotationPresent(Required.class);
    }

    /**
     * Returns the description of a Parameter, if present.
     *
     * @param parameter
     * @return a description or null
     */
    public static String getDescription(Parameter parameter) {
        if (parameter.isAnnotationPresent(Desc.class)) {
            Desc annotation = parameter.getAnnotation(Desc.class);
            return annotation.value();
        }
        return null;
    }

    /**
     * Returns the ref of the model.
     * This ref is either explicitly named or it is generated from the model class name.
     *
     * @param modelClass
     * @return the ref of the model
     */
    public static io.swagger.models.Tag getModelRef(Class<?> modelClass) {
        if (modelClass.isAnnotationPresent(fathom.rest.swagger.Tag.class)) {
            fathom.rest.swagger.Tag annotation = modelClass.getAnnotation(fathom.rest.swagger.Tag.class);
            io.swagger.models.Tag tag = new io.swagger.models.Tag();
            tag.setName(Optional.fromNullable(Strings.emptyToNull(annotation.name())).or(modelClass.getSimpleName()));
            tag.setDescription(annotation.description());
            return tag;
        }

        io.swagger.models.Tag tag = new io.swagger.models.Tag();
        tag.setName(modelClass.getName());
        return tag;
    }

    /**
     * Returns the Tag for a controller.
     *
     * @param controllerClass
     * @return a controller tag or null
     */
    public static io.swagger.models.Tag getTag(Class<? extends Controller> controllerClass) {
        if (controllerClass.isAnnotationPresent(fathom.rest.swagger.Tag.class)) {
            fathom.rest.swagger.Tag annotation = controllerClass.getAnnotation(fathom.rest.swagger.Tag.class);
            io.swagger.models.Tag tag = new io.swagger.models.Tag();
            tag.setName(Optional.fromNullable(Strings.emptyToNull(annotation.name())).or(controllerClass.getSimpleName()));
            tag.setDescription(annotation.description());

            if (!Strings.isNullOrEmpty(annotation.externalDocs())) {
                ExternalDocs docs = new ExternalDocs();
                docs.setUrl(annotation.externalDocs());
                tag.setExternalDocs(docs);
            }

            if (!Strings.isNullOrEmpty(tag.getDescription())) {
                return tag;
            }
        }
        return null;
    }

}
