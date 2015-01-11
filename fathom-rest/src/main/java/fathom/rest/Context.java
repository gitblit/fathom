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

package fathom.rest;

import ro.pippo.core.Application;
import ro.pippo.core.Request;
import ro.pippo.core.Response;
import ro.pippo.core.route.DefaultRouteContext;
import ro.pippo.core.route.RouteMatch;

import java.util.List;

/**
 * @author James Moger
 */
public class Context extends DefaultRouteContext {

    public Context(Application application, Request request, Response response, List<RouteMatch> routeMatches) {
        super(application, request, response, routeMatches);
    }

}
