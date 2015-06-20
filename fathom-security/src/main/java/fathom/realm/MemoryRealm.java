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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import fathom.authc.AuthenticationToken;
import fathom.authc.StandardCredentials;
import fathom.authc.TokenCredentials;
import fathom.authz.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A MemoryRealm caches all accounts in a ConcurrentHashMap.
 *
 * @author James Moger
 */
public class MemoryRealm extends StandardCredentialsRealm {

    private static final Logger log = LoggerFactory.getLogger(MemoryRealm.class);

    private final Map<String, Account> accounts;
    private final Map<String, Account> tokens;
    private final Map<String, Role> definedRoles;
    private String realmName;

    public MemoryRealm() {
        super();
        this.realmName = getClass().getSimpleName();
        this.accounts = new ConcurrentHashMap<>();
        this.tokens = new ConcurrentHashMap<>();
        this.definedRoles = new ConcurrentHashMap<>();
    }

    @Override
    public boolean canAuthenticate(AuthenticationToken authenticationToken) {
        return authenticationToken instanceof StandardCredentials || authenticationToken instanceof TokenCredentials;
    }

    @Override
    public Account authenticate(AuthenticationToken authenticationToken) {
        if (authenticationToken instanceof StandardCredentials) {
            return super.authenticate(authenticationToken);
        } else if (authenticationToken instanceof TokenCredentials) {
            return authenticateToken((TokenCredentials) authenticationToken);
        }
        return null;
    }

    @Override
    public synchronized void setup(Config config) {
        if (config.hasPath("name")) {
            realmName = config.getString("name");
        }

        definedRoles.clear();
        definedRoles.putAll(parseDefinedRoles(config));

        accounts.clear();
        tokens.clear();
        if (config.hasPath("accounts")) {
            for (Config accountConfig : config.getConfigList("accounts")) {
                Account account = parseAccount(accountConfig);
                addAccount(account);
                log.trace("Added '{}' account to '{}'", account, getRealmName());
            }
        }

    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    protected Account parseAccount(Config accountConfig) {
        // all accounts require a username
        String username = Strings.emptyToNull(accountConfig.getString("username"));
        Preconditions.checkNotNull(username, "The 'username' setting may not be null nor empty!");

        String name = null;
        if (accountConfig.hasPath("name")) {
            name = accountConfig.getString("name");
        }

        String password = null;
        if (accountConfig.hasPath("password")) {
            password = accountConfig.getString("password");
        }

        StandardCredentials credentials = new StandardCredentials(username, password);
        Account account = new Account(name, credentials);

        if (accountConfig.hasPath("emailAddresses")) {
            account.addEmailAddresses(accountConfig.getStringList("emailAddresses"));
        }

        if (accountConfig.hasPath("tokens")) {
            account.addTokens(accountConfig.getStringList("tokens"));
            for (String token : account.getTokens()) {
                if (tokens.containsKey(token)) {
                    String otherAccount = tokens.get(token).getUsername();
                    log.error("Token collision: {} has the same token as {}", account.getUsername(), otherAccount);
                } else {
                    tokens.put(token, account);
                }
            }
        }

        if (accountConfig.hasPath("disabled") && accountConfig.getBoolean("disabled")) {
            account.setDisabled();
        }

        // add account roles
        if (accountConfig.hasPath("roles")) {
            for (String role : accountConfig.getStringList("roles")) {
                if (definedRoles.containsKey(role)) {
                    Role definedRole = definedRoles.get(role);
                    account.getAuthorizations().addRole(definedRole);
                } else {
                    account.getAuthorizations().addRole(role);
                }
            }
        }

        // add discrete account permissions
        if (accountConfig.hasPath("permissions")) {
            for (String permission : accountConfig.getStringList("permissions")) {
                account.getAuthorizations().addPermission(permission);
            }
        }

        return account;
    }

    /**
     * Parse the Roles specified in the Config object.
     *
     * @param config
     * @return a map of Name-Role
     */
    protected Map<String, Role> parseDefinedRoles(Config config) {
        // Parse the defined Roles
        Map<String, Role> roleMap = new HashMap<>();
        if (!config.hasPath("roles")) {
            log.trace("'{}' has no roles", config);
        } else if (config.hasPath("roles")) {
            log.trace("Parsing Role definitions");
            Config roleConfig = config.getConfig("roles");
            for (Map.Entry<String, ConfigValue> entry : roleConfig.entrySet()) {
                String name = entry.getKey();
                List<String> permissions = roleConfig.getStringList(name);
                Role role = new Role(name, permissions.toArray(new String[permissions.size()]));
                roleMap.put(role.getName(), role);
            }
        }

        return Collections.unmodifiableMap(roleMap);
    }

    @Override
    public String getRealmName() {
        return realmName;
    }

    @Override
    public boolean hasAccount(String username) {
        return accounts.containsKey(username);
    }

    @Override
    public Account getAccount(String username) {
        Account account = accounts.get(username);
        return account;
    }

    @Override
    public Account authenticate(String username, String password) {
        Account account = authenticate(new StandardCredentials(username, password));
        return account;
    }

    public Account authenticateToken(TokenCredentials credentials) {
        return tokens.get(credentials.getToken());
    }

    public Account addAccount(String username, String password) {
        return addAccount(null, username, password);
    }

    public Account addAccount(String name, String username, String password) {
        StandardCredentials credentials = new StandardCredentials(username, password);
        Account account = new Account(name, credentials);
        return addAccount(account);
    }

    public Account addAccount(Account account) {
        accounts.putIfAbsent(account.getUsername(), account);
        return account;
    }

    public Account removeAccount(String username) {
        Account account = accounts.remove(username);
        return account;
    }

}
