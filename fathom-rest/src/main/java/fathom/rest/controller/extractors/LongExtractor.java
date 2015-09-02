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
import fathom.rest.controller.Long;
import ro.pippo.core.ParameterValue;

/**
 * @author James Moger
 */
public class LongExtractor implements ArgumentExtractor, NamedExtractor, ConfigurableExtractor<Long> {

    private String name;

    private long defaultValue;

    @Override
    public Class<Long> getAnnotationClass() {
        return Long.class;
    }

    @Override
    public void configure(Long annotation) {
        setName(annotation.value());
        setDefaultValue(annotation.defaultValue());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public long getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(long defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public Object extract(Context context) {
        ParameterValue pv = context.getParameter(name);
        long i = pv.toLong(defaultValue);
        return i;
    }
}
