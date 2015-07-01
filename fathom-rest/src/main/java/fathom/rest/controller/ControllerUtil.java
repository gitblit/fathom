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
import fathom.rest.controller.extractors.ArgumentExtractor;
import fathom.rest.controller.extractors.ExtractWith;
import fathom.rest.controller.extractors.ParamExtractor;
import ro.pippo.core.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author James Moger
 */
public class ControllerUtil {

    public static List<String> collectProduces(Method method) {
        Set<String> contentTypes = new LinkedHashSet<>();
        if (method.isAnnotationPresent(Produces.class)) {
            // controller method specifies Produces
            Produces produces = method.getAnnotation(Produces.class);
            for (String value : produces.value()) {
                contentTypes.add(value);
            }
        } else if (method.getDeclaringClass().isAnnotationPresent(Produces.class)) {
            // controller class specifies Produces
            Produces produces = method.getDeclaringClass().getAnnotation(Produces.class);
            for (String value : produces.value()) {
                contentTypes.add(value);
            }
        }
        return new ArrayList<>(contentTypes);
    }

    public static Collection<String> collectSuffixes(Method method) {
        Set<String> suffixes = new LinkedHashSet<>();
        for (String produces : collectProduces(method)) {
            int i = produces.lastIndexOf('/') + 1;
            String type = StringUtils.removeStart(produces.substring(i).toLowerCase(), "x-");
            suffixes.add(type);
        }
        return suffixes;
    }

    public static Collection<Return> collectReturns(Method method) {
        Map<Integer, Return> returns = new TreeMap<>();
        if (method.getDeclaringClass().isAnnotationPresent(Returns.class)) {
            for (Return aReturn : method.getDeclaringClass().getAnnotation(Returns.class).value()) {
                returns.put(aReturn.code(), aReturn);
            }
        }
        if (method.getDeclaringClass().isAnnotationPresent(Return.class)) {
            Return aReturn = method.getDeclaringClass().getAnnotation(Return.class);
            returns.put(aReturn.code(), aReturn);
        }
        if (method.isAnnotationPresent(Returns.class)) {
            for (Return aReturn : method.getAnnotation(Returns.class).value()) {
                returns.put(aReturn.code(), aReturn);
            }
        }
        if (method.isAnnotationPresent(Return.class)) {
            Return aReturn = method.getAnnotation(Return.class);
            returns.put(aReturn.code(), aReturn);
        }
        return returns.values();
    }

    /**
     * Returns the name of a parameter.
     *
     * @param parameter
     * @return the name of a parameter.
     */
    public static String getParameterName(Parameter parameter) {
        // identify parameter name and pattern from method signature
        String methodParameterName = parameter.getName();
        if (parameter.isAnnotationPresent(Param.class)) {
            Param param = parameter.getAnnotation(Param.class);
            if (!Strings.isNullOrEmpty(param.value())) {
                methodParameterName = param.value();
            }
        }
        return methodParameterName;
    }

    /**
     * Returns the appropriate ArgumentExtractor to use for the controller method parameter.
     *
     * @param parameter
     * @return an argument extractor
     */
    public static Class<? extends ArgumentExtractor> getArgumentExtractor(Parameter parameter) {
        for (Annotation annotation : parameter.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(ExtractWith.class)) {
                ExtractWith with = annotation.annotationType().getAnnotation(ExtractWith.class);
                Class<? extends ArgumentExtractor> extractorClass = with.value();
                return extractorClass;
            }
        }
        // if unspecified we use the ParamExtractor
        return ParamExtractor.class;
    }
}
