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

package fathom.realm;

import com.google.common.base.Strings;
import fathom.authc.AuthenticationToken;
import fathom.authc.StandardCredentials;
import fathom.utils.CryptoUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parent class for StandardCredentials realms.
 *
 * @author James Moger
 */
public abstract class StandardCredentialsRealm implements Realm {

    private final static Logger log = LoggerFactory.getLogger(StandardCredentialsRealm.class);

    @Override
    public String toString() {
        return getRealmName();
    }

    @Override
    public boolean canAuthenticate(AuthenticationToken authenticationToken) {
        return authenticationToken instanceof StandardCredentials;
    }

    public abstract Account authenticate(String username, String password);

    @Override
    public Account authenticate(AuthenticationToken authenticationToken) {
        if (authenticationToken instanceof StandardCredentials) {
            StandardCredentials requestCredentials = (StandardCredentials) authenticationToken;
            if (Strings.isNullOrEmpty(requestCredentials.getUsername())
                    || Strings.isNullOrEmpty(requestCredentials.getPassword())) {
                return null;
            }

            return authenticate(requestCredentials);
        }
        return null;
    }

    public Account authenticate(StandardCredentials requestCredentials) {
        if (hasAccount(requestCredentials.getUsername())) {
            Account storedAccount = getAccount(requestCredentials.getUsername());
            StandardCredentials storedCredentials = (StandardCredentials) storedAccount.getCredentials();
            if (Strings.isNullOrEmpty(storedCredentials.getPassword())) {
                log.debug("Account '{}' in '{}' has no password and may not be used for authentication",
                        storedAccount.getUsername(), getRealmName());
                return null;
            }

            if (validatePassword(requestCredentials, storedCredentials)) {
                log.debug("Authentication succeeded for '{}' against '{}'",
                        requestCredentials.getUsername(), getRealmName());
                return storedAccount;
            } else {
                log.debug("Authentication failed for '{}' against '{}'",
                        requestCredentials.getUsername(), getRealmName());
            }
        } else {
            log.debug("Unknown account '{}' in the '{}' realm", requestCredentials.getUsername(), getRealmName());
        }

        return null;
    }

    /**
     * Validate a password.
     * <p>
     * Supported password formats are:
     * <ul>
     * <li>blowfish ({BF} prefix)</li>
     * <li>unsalted sha-256 ({SHA256} prefix)</li>
     * <li>unsalted sha-1 ({SHA1} prefix)</li>
     * <li>unsalted md5 ({MD5} prefix)</li>
     * <li>clear text</li>
     * </ul>
     *
     * @param requestCredentials
     * @param storedCredentials
     * @return true if the request password matches the stored password
     */
    protected boolean validatePassword(StandardCredentials requestCredentials, StandardCredentials storedCredentials) {
        final String storedPassword = storedCredentials.getPassword();
        final String username = requestCredentials.getUsername();
        final String password = requestCredentials.getPassword();
        boolean authenticated = false;

        // test blowfish password
        if (storedPassword.startsWith("{BF}")) {
            if (BCrypt.checkpw(password, storedPassword.substring("{BF}".length()))) {
                log.trace("Blowfish hashed password matched for user '{}'", username);
                authenticated = true;
            }
        }
        // test unsalted SHA-256 password
        else if (storedPassword.startsWith("{SHA256}")) {
            String shaPassword = CryptoUtil.getHashSHA256(password);
            if (storedPassword.substring("{SHA256}".length()).equals(shaPassword)) {
                log.trace("Unsalted SHA-256 hashed password matched for user '{}'", username);
                authenticated = true;
            }
        }
        // test unsalted SHA-1 password
        else if (storedPassword.startsWith("{SHA1}")) {
            String shaPassword = CryptoUtil.getHashSHA1(password);
            if (storedPassword.substring("{SHA1}".length()).equals(shaPassword)) {
                log.trace("Unsalted SHA-1 hashed password matched for user '{}'", username);
                authenticated = true;
            }
        }
        // test unsalted MD5 password
        else if (storedPassword.startsWith("{MD5}")) {
            String md5Password = CryptoUtil.getHashMD5(password);
            if (storedPassword.substring("{MD5}".length()).equals(md5Password)) {
                log.trace("Unsalted MD5 hashed password matched for user '{}'", username);
                authenticated = true;
            }
        }
        // test clear text password
        else if (storedPassword.equals(password)) {
            log.trace("Clear text password matched for user '{}'", username);
            authenticated = true;
        }

        return authenticated;
    }
}
