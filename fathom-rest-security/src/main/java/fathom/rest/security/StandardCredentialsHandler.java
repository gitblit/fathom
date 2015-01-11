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

package fathom.rest.security;

import fathom.authc.StandardCredentials;
import fathom.realm.Account;
import fathom.rest.Context;
import fathom.security.SecurityManager;

/**
 * Base class for standard authentication handlers.
 *
 * @author James Moger
 */
public abstract class StandardCredentialsHandler {

    protected final SecurityManager securityManager;

    protected StandardCredentialsHandler(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    /**
     * Determines if the current Context has already been authenticated.
     *
     * @param context
     * @return true if this Context is authenticated.
     */
    protected final boolean isAuthenticated(Context context) {
        Account account = context.getSession(AuthConstants.ACCOUNT_ATTRIBUTE);
        if (account == null) {
            account = context.getLocal(AuthConstants.ACCOUNT_ATTRIBUTE);
        }
        return account != null && account.isAuthenticated();
    }

    /**
     * Authenticate the supplied credentials and setup the Context.
     *
     * @param username
     * @param password
     * @return an account if the authentication is successful
     */
    protected Account authenticate(String username, String password) {
        StandardCredentials authenticationToken = new StandardCredentials(username, password);
        Account account = securityManager.authenticate(authenticationToken);
        return account;
    }
}
