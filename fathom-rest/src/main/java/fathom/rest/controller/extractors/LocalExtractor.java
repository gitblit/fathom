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

import fathom.rest.controller.Local;
import fathom.rest.Context;

/**
 * @author James Moger
 */
public class LocalExtractor implements NamedExtractor, ConfigurableExtractor<Local> {

    private String name;

    @Override
    public void checkTargetType(Class<?> targetType) {
    }

    public Class<Local> getAnnotationClass() {
        return Local.class;
    }

    @Override
    public void configure(Local local) {
        setName(local.value());
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
    public <T> T extract(Context context, Class<T> classOfT) {
        T t = context.getLocal(name);
        return t;
    }
}
