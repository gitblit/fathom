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

package fathom.rest.route;

import fathom.rest.Context;
import ro.pippo.core.route.RouteHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Sets the specified headers in the Response and continues the chain.
 *
 * @author James Moger
 */
public class HeaderFilter implements RouteHandler<Context> {

    final Map<String, Object> headers;

    public HeaderFilter() {
        headers = new HashMap<>();
    }

    public void setHeader(String name, Object value) {
        this.headers.put(name, value);
    }

    @Override
    public void handle(Context context) {
        for (Map.Entry<String, Object> header : headers.entrySet()) {
            context.setHeader(header.getKey(), header.getValue());
        }

        context.next();
    }

}

