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

package fathom.realm.windows;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.sun.jna.platform.win32.Win32Exception;
import com.typesafe.config.Config;
import fathom.authc.StandardCredentials;
import fathom.realm.Account;
import fathom.realm.CachingRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import waffle.windows.auth.IWindowsAccount;
import waffle.windows.auth.IWindowsAuthProvider;
import waffle.windows.auth.IWindowsComputer;
import waffle.windows.auth.IWindowsIdentity;
import waffle.windows.auth.impl.WindowsAuthProviderImpl;

import java.util.Arrays;
import java.util.List;

/**
 * @author James Moger
 */
public class WindowsRealm extends CachingRealm {

    private final static Logger log = LoggerFactory.getLogger(WindowsRealm.class);
    protected List<String> adminGroups;
    private String defaultDomain;
    private boolean allowGuests;
    private IWindowsAuthProvider waffle;

    @Override
    public void setup(Config config) {
        super.setup(config);

        String os = System.getProperty("os.name").toLowerCase();
        Preconditions.checkState(os.startsWith("windows"), "Windows authentication is not supported on '{}'", os);

        if (config.hasPath("defaultDomain")) {
            defaultDomain = Strings.emptyToNull(config.getString("defaultDomain"));
        }

        if (config.hasPath("allowGuests")) {
            allowGuests = config.getBoolean("allowGuests");
        }

        adminGroups = Arrays.asList("BUILTIN\\Administrators");
        if (config.hasPath("adminGroups")) {
            adminGroups = config.getStringList("adminGroups");
        }

        waffle = new WindowsAuthProviderImpl();
    }

    protected String describeJoinStatus(String value) {
        if ("NetSetupUnknownStatus".equals(value)) {
            return "unknown";
        } else if ("NetSetupUnjoined".equals(value)) {
            return "not joined";
        } else if ("NetSetupWorkgroupName".equals(value)) {
            return "joined to a workgroup";
        } else if ("NetSetupDomainName".equals(value)) {
            return "joined to a domain";
        }
        return value;
    }

    @Override
    public void start() {
        log.debug("Realm '{}' configuration:", getRealmName());
        logSetting(log, "defaultDomain", defaultDomain);
        logSetting(log, "allowGuests", allowGuests);
        logSetting(log, "adminGroups", adminGroups);
        super.logCacheSettings(log);

        IWindowsComputer computer = waffle.getCurrentComputer();
        log.debug("Windows realm information:");
        logSetting(log, "name", computer.getComputerName());
        logSetting(log, "status", describeJoinStatus(computer.getJoinStatus()));
        logSetting(log, "memberOf", computer.getMemberOf());
    }

    @Override
    public void stop() {
    }

    @Override
    public Account authenticate(StandardCredentials requestCredentials) {

        final String username = getSimpleUsername(requestCredentials.getUsername());
        final String password = requestCredentials.getPassword();

        if (hasAccount(username)) {
            // account is cached, authenticate against the cache
            return super.authenticate(new StandardCredentials(username, password));
        }

        return authenticate(username, password);

    }

    @Override
    public Account authenticate(final String username, final String password) {
        IWindowsIdentity identity = null;
        try {
            if (username.indexOf('@') > -1 || username.indexOf('\\') > -1) {
                // manually specified domain
                identity = waffle.logonUser(username, password);
            } else {
                // no domain specified, use default domain
                identity = waffle.logonDomainUser(username, defaultDomain, password);
            }

            log.debug("Authentication succeeded for '{}' against '{}'", username, getRealmName());

            if (identity.isGuest() && !allowGuests) {
                log.warn("Guest account access is disabled");
                return null;
            }

            String name;
            String fqn = identity.getFqn();
            if (fqn.indexOf('\\') > -1) {
                name = fqn.substring(fqn.lastIndexOf('\\') + 1);
            } else {
                name = fqn;
            }

            Account account = new Account(name, new StandardCredentials(username, password));

            for (IWindowsAccount group : identity.getGroups()) {
                account.getAuthorizations().addRole(group.getFqn());
            }

            setAdminAttribute(account);
            cacheAccount(account);

            return account;
        } catch (Win32Exception e) {
            log.debug("Authentication failed for '{}' against '{}'", username, getRealmName());
            log.error(e.getMessage());
        } finally {
            if (identity != null) {
                identity.dispose();
            }
        }
        return null;
    }

    /**
     * Set the admin attribute from group memberships retrieved from Windows.
     *
     * @param account
     */
    private void setAdminAttribute(Account account) {
        if (adminGroups != null) {
            for (String adminGroup : adminGroups) {
                if (adminGroup.startsWith("@") && account.getUsername().equalsIgnoreCase(adminGroup.substring(1))) {
                    // admin user
                    account.getAuthorizations().addPermission("*");
                } else if (account.hasRole(adminGroup)) {
                    // admin role
                    account.getAuthorizations().addPermission("*");
                }
            }
        }
    }

    /**
     * Returns a simple username without any domain prefixes.
     *
     * @param username
     * @return a simple username
     */
    private String getSimpleUsername(String username) {
        String simpleUsername = username;
        if (defaultDomain != null) {
            // sanitize username
            if (username.startsWith(defaultDomain + "\\")) {
                // strip default domain from domain\ username
                simpleUsername = username.substring(defaultDomain.length() + 1);
            } else if (username.endsWith("@" + defaultDomain)) {
                // strip default domain from username@domain
                simpleUsername = username.substring(0, username.lastIndexOf('@'));
            }
        }
        return simpleUsername;
    }
}
