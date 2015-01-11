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

import fathom.rest.Context;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for authentication handlers.
 *
 * @author James Moger
 */
public abstract class AbstractAuthcHandler {

    private final static Logger log = LoggerFactory.getLogger(AbstractAuthcHandler.class);

    /**
     * Determines if the current Context has already been authenticated.
     *
     * @param context
     * @return true if this Context is authenticated.
     */
    protected final boolean isAuthenticated(Context context) {
        return SecurityUtils.getSubject().isAuthenticated();
    }

    /**
     * Authenticate the supplied credentials and setup the Context.
     *
     * @param context
     * @param username
     * @param password
     * @param rememberMe
     * @return true if the authentication is successful
     */
    protected final boolean authenticate(Context context, String username, String password, boolean rememberMe) {
        UsernamePasswordToken authenticationToken = new UsernamePasswordToken(username, password, rememberMe);
        try {
            SecurityUtils.getSubject().login(authenticationToken);
            log.info("'{}' authenticated", username);
            return true;
        } catch (AuthenticationException e) {
            log.info("'{}' failed to authenticate", username);
            return false;
        }
    }
}
