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

package fathom.realm.jdbc;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fathom.authc.StandardCredentials;
import fathom.authz.Role;
import fathom.exception.FathomException;
import fathom.realm.Account;
import fathom.realm.CachingRealm;
import fathom.utils.ClassUtil;
import fathom.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


/**
 * Realm that allows you to authenticate against a JDBC datasource.
 *
 * @author James Moger
 */
public class JdbcRealm extends CachingRealm {

    private static final Logger log = LoggerFactory.getLogger(JdbcRealm.class);

    private final static String COMMA_SEMI_COLON_DELIMITER = ",|;";

    private final static String SEMI_COLON_DELIMITER = ";";

    protected String jdbcUrl;

    protected String jdbcUsername;

    protected String jdbcPassword;

    protected DataSource dataSource;

    protected String accountQuery;

    protected String passwordMapping;

    protected String nameMapping;

    protected String emailMapping;

    protected String roleMapping;

    protected String permissionMapping;

    protected String accountRolesQuery;

    protected String accountPermissionsQuery;

    protected String definedRolesQuery;

    protected String startScript;

    protected String stopScript;

    protected Config hikariCPConfig;

    @Inject
    MetricRegistry metricRegistry;

    @Override
    public void setup(Config config) {
        super.setup(config);

        if (config.hasPath("url")) {
            jdbcUrl = Strings.emptyToNull(config.getString("url"));
        }
        Preconditions.checkNotNull(jdbcUrl, "You must specify 'url'");

        if (config.hasPath("username")) {
            jdbcUsername = Strings.emptyToNull(config.getString("username"));
        }

        if (config.hasPath("password")) {
            jdbcPassword = Strings.emptyToNull(config.getString("password"));
        }

        if (config.hasPath("accountQuery")) {
            accountQuery = Strings.emptyToNull(config.getString("accountQuery"));
        }
        Preconditions.checkNotNull(accountQuery, "You must specify 'accountQuery'");

        passwordMapping = "password";
        if (config.hasPath("passwordMapping")) {
            passwordMapping = Strings.emptyToNull(config.getString("passwordMapping"));
        }
        Preconditions.checkNotNull(passwordMapping, "You must specify 'passwordMapping'");

        if (config.hasPath("nameMapping")) {
            nameMapping = Strings.emptyToNull(config.getString("nameMapping"));
        }

        if (config.hasPath("emailMapping")) {
            emailMapping = Strings.emptyToNull(config.getString("emailMapping"));
        }

        if (config.hasPath("roleMapping")) {
            roleMapping = Strings.emptyToNull(config.getString("roleMapping"));
        }

        if (config.hasPath("permissionMapping")) {
            permissionMapping = Strings.emptyToNull(config.getString("permissionMapping"));
        }

        if (config.hasPath("accountRolesQuery")) {
            accountRolesQuery = Strings.emptyToNull(config.getString("accountRolesQuery"));
        }

        if (config.hasPath("accountPermissionsQuery")) {
            accountPermissionsQuery = Strings.emptyToNull(config.getString("accountPermissionsQuery"));
        }

        if (config.hasPath("definedRolesQuery")) {
            definedRolesQuery = Strings.emptyToNull(config.getString("definedRolesQuery"));
        }

        if (config.hasPath("startScript")) {
            startScript = Strings.emptyToNull(config.getString("startScript"));
        }

        if (config.hasPath("stopScript")) {
            stopScript = Strings.emptyToNull(config.getString("stopScript"));
        }

        if (config.hasPath("hikariCP")) {
            hikariCPConfig = config.getConfig("hikariCP");
        }

    }

    @Override
    public void start() {

        log.debug("Realm '{}' configuration:", getRealmName());
        Util.logSetting(log, "url", jdbcUrl);
        Util.logSetting(log, "username", jdbcUsername);
        Util.logSetting(log, "password", jdbcPassword);
        Util.logSetting(log, "accountQuery", accountQuery);
        Util.logSetting(log, "passwordMapping", passwordMapping);
        Util.logSetting(log, "nameMapping", nameMapping);
        Util.logSetting(log, "emailMapping", emailMapping);
        Util.logSetting(log, "roleMapping", roleMapping);
        Util.logSetting(log, "permissionMapping", permissionMapping);
        Util.logSetting(log, "accountRolesQuery", accountRolesQuery);
        Util.logSetting(log, "accountPermissionsQuery", accountPermissionsQuery);
        Util.logSetting(log, "definedRolesQuery", definedRolesQuery);
        Util.logSetting(log, "startScript", startScript);
        Util.logSetting(log, "stopScript", stopScript);
        super.logCacheSettings(log);

        if (dataSource == null) {
            Properties properties = new Properties();
            if (hikariCPConfig != null) {
                Map<String, Object> values = hikariCPConfig.root().unwrapped();
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    properties.setProperty(entry.getKey(), entry.getValue().toString());
                }
            }

            HikariConfig config = new HikariConfig(properties);
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(jdbcUsername);
            config.setPassword(jdbcPassword);
            config.setPoolName(getRealmName() + "-Pool");
            config.setMetricRegistry(metricRegistry);

            HikariDataSource ds = new HikariDataSource(config);
            setDataSource(ds);
        }

        if (!Strings.isNullOrEmpty(startScript)) {
            executeScript(startScript);
        }
    }

    @Override
    public void stop() {
        if (dataSource != null) {

            if (!Strings.isNullOrEmpty(stopScript)) {
                executeScript(stopScript);
            }

            if (dataSource instanceof HikariDataSource) {
                HikariDataSource ds = (HikariDataSource) dataSource;
                ds.close();
            }
        }
    }

    /**
     * Returns the datasource that is being used to retrieve connections for this realm.
     *
     * @return dataSource the SQL data source.
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Sets the datasource that should be used to retrieve connections used by this realm.
     *
     * @param dataSource the SQL data source.
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Account authenticate(StandardCredentials requestCredentials) {

        final String username = requestCredentials.getUsername();
        final String password = requestCredentials.getPassword();

        if (hasAccount(username)) {
            // account is cached, authenticate against the cache
            return super.authenticate(new StandardCredentials(username, password));
        }

        return authenticate(username, password);
    }

    @Override
    public Account authenticate(final String username, final String password) {

        try (Connection conn = dataSource.getConnection()) {
            Account account = getAccount(conn, username);
            if (account == null) {
                log.debug("No account found for '{}' in '{}'", username, getRealmName());
                return null;
            }

            StandardCredentials storedCredentials = (StandardCredentials) account.getCredentials();
            if (Strings.isNullOrEmpty(storedCredentials.getPassword())) {
                log.debug("Account '{}' in '{}' has no password and may not be used for authentication",
                        account.getUsername(), getRealmName());
                return null;
            }

            StandardCredentials requestCredentials = new StandardCredentials(username, password);
            if (validatePassword(requestCredentials, storedCredentials)) {
                log.debug("Authentication succeeded for '{}' against '{}'", username, getRealmName());

                setAuthorizationsByQuery(conn, account);
                cacheAccount(account);

                return account;
            } else {
                log.debug("Authentication failed for '{}' against '{}'", username, getRealmName());
            }

        } catch (SQLException e) {
            log.error("There was an SQL error while authenticating '{}'", username, e);
        }

        return null;
    }

    protected Account getAccount(Connection conn, String username) throws SQLException {
        Account account = null;
        try (PreparedStatement ps = conn.prepareStatement(accountQuery)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Check to ensure only one row is processed
                    if (account != null) {
                        throw new FathomException("More than one row found for '{}'. Usernames must be unique.", username);
                    }

                    String password = Strings.emptyToNull(rs.getString(passwordMapping));
                    Preconditions.checkNotNull(password, "Password for '{}' is null or empty!", username);

                    // set a name from the accounts query
                    String name = null;
                    if (!Strings.isNullOrEmpty(nameMapping)) {
                        name = Strings.emptyToNull(rs.getString(nameMapping));
                    }

                    // create an account
                    account = new Account(name, new StandardCredentials(username, password));

                    // add email addresses from the accounts query
                    if (!Strings.isNullOrEmpty(emailMapping)) {
                        String value = rs.getString(emailMapping);
                        Set<String> addresses = toSet(value, COMMA_SEMI_COLON_DELIMITER);
                        account.addEmailAddresses(addresses);
                    }

                    // add permissions from the accounts query
                    if (!Strings.isNullOrEmpty(permissionMapping)) {
                        String value = rs.getString(permissionMapping);
                        Set<String> permissions = toSet(value, SEMI_COLON_DELIMITER);
                        for (String permission : permissions) {
                            account.getAuthorizations().addPermission(permission);
                        }
                    }

                    // add roles from the accounts query
                    if (!Strings.isNullOrEmpty(roleMapping)) {
                        Map<String, Role> definedRoles = getDefinedRoles(conn);

                        String value = rs.getString(roleMapping);
                        Set<String> roles = toSet(value, COMMA_SEMI_COLON_DELIMITER);
                        for (String role : roles) {
                            if (definedRoles.containsKey(role)) {
                                Role definedRole = definedRoles.get(role);
                                account.getAuthorizations().addRole(definedRole);
                            } else {
                                account.getAuthorizations().addRole(role);
                            }
                        }
                    }

                    cacheAccount(account);
                }
            }
        }

        return account;
    }

    private void setAuthorizationsByQuery(Connection conn, Account account) throws SQLException {
        // Retrieve roles and permissions from database
        Map<String, Role> declaredRoles = getDefinedRoles(conn);

        Set<String> roles = getRolesByQuery(conn, account.getUsername());
        for (String role : roles) {
            if (declaredRoles.containsKey(role)) {
                Role declaredRole = declaredRoles.get(role);
                account.getAuthorizations().addRole(declaredRole);
            } else {
                account.getAuthorizations().addRole(role);
            }
        }

        Set<String> permissions = getPermissionsByQuery(conn, account.getUsername());
        for (String permission : permissions) {
            account.getAuthorizations().addPermission(permission);
        }
    }

    protected Map<String, Role> getDefinedRoles(Connection conn) throws SQLException {
        if (Strings.isNullOrEmpty(definedRolesQuery)) {
            log.trace("'{}' not set", "definedRolesQuery");
            return Collections.emptyMap();
        }

        Map<String, Role> declaredRoles = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(definedRolesQuery)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Add the role to the set of roles
                    String name = rs.getString(1);
                    String value = rs.getString(2);
                    Set<String> permissions = toSet(value, SEMI_COLON_DELIMITER);

                    if (Strings.isNullOrEmpty(name) || permissions.isEmpty()) {
                        log.warn("Skipping defined role '{}':'{}' because of null/empty values", name, value);
                    } else {
                        Role role = new Role(name);
                        for (String permission : permissions) {
                            role.addPermission(permission);
                        }
                        declaredRoles.put(name, role);
                        log.debug("Added defined role '{}':'{}'", name, value);
                    }
                }
            }
        }

        return declaredRoles;
    }

    protected Set<String> getRolesByQuery(Connection conn, String username) throws SQLException {
        if (Strings.isNullOrEmpty(accountRolesQuery)) {
            log.trace("'{}' not set", "accountRolesQuery");
            return Collections.emptySet();
        }

        Set<String> roles = new LinkedHashSet<String>();
        try (PreparedStatement ps = conn.prepareStatement(accountRolesQuery)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Add the role to the set of roles
                    String value = rs.getString(1);
                    roles.addAll(toSet(value, COMMA_SEMI_COLON_DELIMITER));
                }
            }
        }

        return roles;
    }

    protected Set<String> getPermissionsByQuery(Connection conn, String username) throws SQLException {
        if (Strings.isNullOrEmpty(accountPermissionsQuery)) {
            log.trace("'{}' not set", "accountPermissionsQuery");
            return Collections.emptySet();
        }

        Set<String> permissions = new LinkedHashSet<String>();
        try (PreparedStatement ps = conn.prepareStatement(accountPermissionsQuery)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Add the permission to the set of permissions
                    String value = rs.getString(1);
                    permissions.addAll(toSet(value, SEMI_COLON_DELIMITER));
                }
            }
        }

        return permissions;
    }

    /**
     * Execute a script located either in the classpath or on the filesystem.
     *
     * @param scriptPath
     */
    protected void executeScript(String scriptPath) {
        URL scriptUrl = null;
        try {
            if (scriptPath.startsWith("classpath:")) {
                String script = scriptPath.substring("classpath:".length());
                scriptUrl = ClassUtil.getResource(script);
                if (scriptUrl == null) {
                    log.warn("Script '{}' not found!", scriptPath);
                }
            } else {
                File file = new File(scriptPath);
                if (file.exists()) {
                    scriptUrl = file.toURI().toURL();
                } else {
                    log.warn("Script '{}' not found!", scriptPath);
                }
            }

            // execute the script
            if (scriptUrl != null) {
                executeScript(scriptUrl);
            }

        } catch (Exception e) {
            log.error("Failed to parse '{}' as a url", scriptPath);
        }
    }

    /**
     * Execute a script.
     *
     * @param scriptUrl
     */
    protected void executeScript(URL scriptUrl) {
        log.debug("Executing script '{}'", scriptUrl);
        try (Connection conn = dataSource.getConnection()) {
            try (Statement stat = conn.createStatement()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(scriptUrl.openStream(), StandardCharsets.UTF_8))) {
                    reader.lines()
                            .filter(line -> !Strings.isNullOrEmpty(line) && line.charAt(0) != '#' && !line.startsWith("//"))
                            .forEach(line -> {
                                try {
                                    log.trace("execute: {}", line);
                                    stat.execute(line.trim());
                                } catch (SQLException e) {
                                    log.error("Failed to execute '{}'", line, e);
                                }
                            });
                }
            }
        } catch (SQLException | IOException e) {
            log.error("Failed to execute script '{}'", scriptUrl, e);
        }
    }

    /**
     * Creates an ordered set from a comma or semi-colon delimited string.
     * Empty values are discarded.
     *
     * @param value
     * @return a set of strings
     */
    private Set<String> toSet(String value, String delimiter) {
        if (Strings.isNullOrEmpty(value)) {
            return Collections.emptySet();
        } else {
            Set<String> stringSet = new LinkedHashSet<>();
            String[] values = value.split(delimiter);
            for (String stringValue : values) {
                if (!Strings.isNullOrEmpty(stringValue)) {
                    stringSet.add(stringValue.trim());
                }
            }

            return stringSet;
        }
    }
}
