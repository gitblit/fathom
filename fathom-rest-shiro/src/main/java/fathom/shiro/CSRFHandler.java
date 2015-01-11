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

package fathom.shiro;

import fathom.rest.route.AbstractCSRFHandler;
import fathom.rest.Context;
import org.apache.shiro.SecurityUtils;

/**
 * Generates and validates an CSRF token based on an authenticated Shiro session.
 *
 * @author James Moger
 */
public class CSRFHandler extends AbstractCSRFHandler {

    /**
     * Constructs an CSRF handler with a dynamically generated SecretKey.
     */
    public CSRFHandler() {
        super();
    }

    public CSRFHandler(String secretKey) {
        super(secretKey);
    }

    public CSRFHandler(String secretKey, String algorithm) {
        super(secretKey, algorithm);
    }

    @Override
    protected String getSessionCsrfToken(Context context) {
        return (String) SecurityUtils.getSubject().getSession().getAttribute(TOKEN);
    }

    @Override
    protected void setSessionCsrfToken(Context context, String token) {
        SecurityUtils.getSubject().getSession().setAttribute(TOKEN, token);
    }

    @Override
    protected String getTokenId(Context context) {
        return SecurityUtils.getSubject().getSession().getId().toString();
    }

}

