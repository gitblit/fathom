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

import fathom.rest.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.Request;
import ro.pippo.core.Response;

/**
 * Base class for a Controller.
 *
 * @author James Moger
 */
public class Controller {

    private Context context;

    public final void setContext(Context context) {
        this.context = context;
    }

    public final Context getContext() {
        return context;
    }

    public final Request getRequest() {
        return context.getRequest();
    }

    public final Response getResponse() {
        return context.getResponse();
    }

    public final void redirectTo(String path) {
        getContext().redirect(path);
    }
}
