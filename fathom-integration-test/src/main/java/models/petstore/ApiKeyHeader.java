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

package models.petstore;

import com.google.common.base.Optional;
import fathom.realm.Account;
import fathom.rest.Context;
import fathom.rest.controller.ReturnHeader;
import fathom.rest.controller.extractors.AuthExtractor;
import fathom.rest.security.aop.RequireToken;
import fathom.rest.swagger.ApiModel;
import org.sonatype.micromailer.imp.Strings;
import ro.pippo.core.route.RouteDispatcher;

/**
 * @author James Moger
 */
@ApiModel(name = ApiKeyHeader.NAME, description = "the key to send with api requests")
public class ApiKeyHeader extends ReturnHeader.StringHeader {

    static final String NAME = RequireToken.DEFAULT;

    static final String DEFAULT = "needone";

    @Override
    public String getHeaderName() {
        return NAME;
    }

    @Override
    public String getDefaultValue() {
        Context context = RouteDispatcher.getRouteContext();
        if (context == null) {
            // no context because we are generated a Swagger specification
            return DEFAULT;
        }

        // return the primary account token or DEFAULT
        Account account = new AuthExtractor().extract(context);
        return Optional.fromNullable(account.getToken()).or(DEFAULT);
    }

    @Override
    public boolean validate(String value) {
        return Strings.isNotEmpty(value);
    }
}
