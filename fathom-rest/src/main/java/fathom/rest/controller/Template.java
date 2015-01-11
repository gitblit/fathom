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

import com.google.common.base.Strings;
import fathom.rest.Context;
import ro.pippo.core.HttpConstants;
import ro.pippo.core.util.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a Template response to a controller request.
 *
 * @author James Moger
 */
public class Template implements ControllerResult {

    private String templateName;

    private String contentType;

    private int statusCode;

    private Map<String, Object> bindings;

    protected Template(String templateName) {
        this.templateName = templateName;
        this.statusCode = 200;
        this.bindings = new HashMap<>();
    }

    /**
     * Returns a template.
     *
     * @param templateName
     * @return
     */
    public static Template named(String templateName) {
        return new Template(templateName);
    }

    /**
     * Returns a 'text/html' template.
     *
     * @param templateName
     * @return a Template with an text/html content type
     */
    public static Template view(String templateName) {
        Template template = named(templateName);
        template.contentType(HttpConstants.ContentType.TEXT_HTML);
        return template;
    }

    public Template html() {
        return contentType(HttpConstants.ContentType.TEXT_HTML);
    }

    public Template json() {
        return contentType(HttpConstants.ContentType.APPLICATION_JSON);
    }

    public Template xml() {
        return contentType(HttpConstants.ContentType.APPLICATION_XML);
    }

    public Template text() {
        return contentType(HttpConstants.ContentType.TEXT_PLAIN);
    }

    public Template contentType(String contentType) {
        this.contentType = contentType;

        return this;
    }

    public Template status(int statusCode) {
        this.statusCode = statusCode;

        return this;
    }

    public Template set(String name, Object value) {
        this.bindings.put(name, value);

        return this;
    }

    public Template set(String name, String value, Object... args) {
        this.bindings.put(name, StringUtils.format(value, args));

        return this;
    }

    public Template setAll(Map<String, Object> bindings) {
        this.bindings.putAll(bindings);

        return this;
    }

    @Override
    public void process(Context context, Method controllerMethod) {
        if (!Strings.isNullOrEmpty(contentType)) {
            context.getResponse().contentType(contentType);
        }
        context.status(statusCode);
        context.setLocals(bindings);
        if (Strings.isNullOrEmpty(templateName)) {
            templateName = controllerMethod.getDeclaringClass().getSimpleName() + "/" + controllerMethod.getName();
        }
        context.render(templateName);
    }
}
