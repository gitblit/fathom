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

import fathom.rest.controller.Param;
import fathom.rest.Context;
import ro.pippo.core.ParameterValue;

/**
 * @author James Moger
 */
public class ParamExtractor implements NamedExtractor, ConfigurableExtractor<Param> {

    private String name;

    private String pattern;

    @Override
    public void checkTargetType(Class<?> targetType) {
        ParameterValue testValue = new ParameterValue();
        testValue.to(targetType);
    }

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

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public <T> T extract(Context context, Class<T> classOfT) {
        ParameterValue pv = context.getParameter(name);
        T t = pv.to(classOfT, pattern);
        return t;
    }
}
