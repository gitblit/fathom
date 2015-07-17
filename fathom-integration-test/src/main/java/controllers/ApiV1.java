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

import fathom.rest.controller.ContentTypeBySuffix;
import fathom.rest.controller.Path;
import fathom.rest.controller.Produces;

/**
 * @author James Moger
 */
@Path("/v1")
@Produces({Produces.JSON, Produces.XML})
@ContentTypeBySuffix
public abstract class ApiV1 extends Api {
}
