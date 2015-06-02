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

package fathom.rest.controller.extractors;

import fathom.rest.Context;
import fathom.rest.controller.Param;
import ro.pippo.core.ParameterValue;

import java.util.Collection;

/**
 * @author James Moger
 */
public class ParamExtractor extends DefaultObjectExtractor
        implements NamedExtractor, PatternExtractor, ConfigurableExtractor<Param> {

    private String name;

    private String pattern;

    @Override
    public Class<Param> getAnnotationClass() {
        return Param.class;
    }

    @Override
    public void configure(Param param) {
        setName(param.value());
        setPattern(param.pattern());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    @Override
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public Object extract(Context context) {
        ParameterValue pv = context.getParameter(name);
        if (collectionType == null) {
            Object o = pv.to(objectType, pattern);
            return o;
        }

        Object o = pv.toCollection(collectionType, objectType, pattern);
        return o;
    }
}
