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
package controllers;

import com.google.common.base.Optional;
import fathom.metrics.Timed;
import fathom.rest.controller.Consumes;
import fathom.rest.controller.Path;
import fathom.rest.controller.Controller;
import fathom.rest.controller.GET;

@Path("/annotated")
public class HelloAnnotatedController extends Controller {

    @Timed("hello.annotated")
    @GET("/")
    @Consumes({Consumes.HTML})
    public void hello(String name) {
        getResponse().bind("greeting", "Hello " + Optional.fromNullable(name).or("World")).render("hello");
    }

}
