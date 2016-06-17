/*
 * Copyright (C) 2016 the original author or authors.
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

package fathom.realm.keycloak;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import fathom.authc.AuthenticationToken;
import fathom.authz.Role;
import fathom.exception.FathomException;
import fathom.realm.Account;
import fathom.realm.Realm;
import fathom.utils.ClassUtil;
import fathom.utils.Util;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.spi.InMemorySessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.spi.UserSessionManagement;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author James Moger
 */
public class KeycloakRealm implements Realm, UserSessionManagement {

    private final Logger log = LoggerFactory.getLogger(KeycloakRealm.class);

    private final Cache<String, Account> accountCache;
    private final Map<String, Role> definedRoles;
    private final SessionIdMapper sessionIdMapper;
    private final NodesRegistrationManagement nodesRegistrationManagement;
    private String realmName;
    private KeycloakConfig keycloakConfig;
    private KeycloakDeployment keycloakDeployment;


    public KeycloakRealm() {
        this.definedRoles = new ConcurrentHashMap<>();
        this.accountCache = CacheBuilder
                .newBuilder()
                .expireAfterAccess(15, TimeUnit.SECONDS)
                .build();
        this.sessionIdMapper = new InMemorySessionIdMapper();
        this.nodesRegistrationManagement = new NodesRegistrationManagement();
    }

    public KeycloakConfig getKeycloakConfig() {
        return keycloakConfig;
    }

    public KeycloakDeployment getKeycloakDeployment() {
        return keycloakDeployment;
    }

    public SessionIdMapper getSessionIdMapper() {
        return sessionIdMapper;
    }

    public void registerKeycloakDeployment(KeycloakDeployment keycloakDeployment) {
        nodesRegistrationManagement.tryRegister(keycloakDeployment);
    }

    @Override
    public void setup(Config config) {
        String configFile = "classpath:conf/keycloak.json";
        if (config.hasPath("file")) {
            configFile = config.getString("file");
        }

        keycloakConfig = parseKeycloakConfig(configFile);
        keycloakDeployment = KeycloakDeploymentBuilder.build(keycloakConfig);

        realmName = keycloakConfig.getRealm() + "/" + keycloakConfig.getResource();
        if (config.hasPath("name")) {
            realmName = config.getString("name");
        }

        definedRoles.clear();
        definedRoles.putAll(parseDefinedRoles(config));
    }

    private KeycloakConfig parseKeycloakConfig(String configFile) {
        String content = ClassUtil.loadStringResource(configFile);
        if (content == null) {
            log.warn("Failed to find Keycloak config '{}'", configFile);
        } else {
            try {
                return JsonSerialization.readValue(content, KeycloakConfig.class);
            } catch (IOException e) {
                throw new FathomException("Failed to deserialize {}", configFile, e);
            }
        }
        return new KeycloakConfig();
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
    public void start() {
        if (Strings.isNullOrEmpty(keycloakConfig.getRealm()) || Strings.isNullOrEmpty(keycloakConfig.getRealmKey())) {
            log.warn("The 'keycloak' settings in your config is incomplete. Keycloak will deny all requests.");
        } else {
            log.debug("Keycloak '{}' configuration:", getRealmName());
            Util.logSetting(log, "realm", keycloakConfig.getRealm());
            Util.logSetting(log, "resource", keycloakConfig.getResource());
            Util.logSetting(log, "use-resource-role-mappings", keycloakConfig.isUseResourceRoleMappings());
            Util.logSetting(log, "realm-public-key", keycloakConfig.getRealmKey().substring(0, 8) + "..." + keycloakConfig.getRealmKey().substring(keycloakConfig.getRealmKey().length() - 8));
            Util.logSetting(log, "server-auth-url", keycloakConfig.getAuthServerUrl());
            Util.logSetting(log, "enable-basic-auth", keycloakConfig.isEnableBasicAuth());

            Util.logSetting(log, "bearer-only", keycloakConfig.isBearerOnly());
            Util.logSetting(log, "ssl-required", keycloakConfig.getSslRequired());
            Util.logSetting(log, "expose-token", keycloakConfig.isExposeToken());
            Util.logSetting(log, "public-client", keycloakConfig.isPublicClient());
            Util.logSetting(log, "allow-any-hostname", keycloakConfig.isAllowAnyHostname());

            Util.logSetting(log, "cors", keycloakConfig.isCors());
            Util.logSetting(log, "cors-max-age", keycloakConfig.getCorsMaxAge());
            Util.logSetting(log, "cors-methods", keycloakConfig.getCorsAllowedMethods());
            Util.logSetting(log, "cors-allowed-headers", keycloakConfig.getCorsAllowedHeaders());

//            Util.logSetting(log, "principal-attribute", keycloakConfig.getPrincipalAttribute());
//            Util.logSetting(log, "turn-off-change-session-id-on-login", keycloakConfig.getTurnOffChangeSessionIdOnLogin());

            Util.logSetting(log, "token-store", keycloakConfig.getTokenStore());
            Util.logSetting(log, "always-refresh-token", keycloakConfig.isAlwaysRefreshToken());

            Util.logSetting(log, "connection-pool-size", keycloakConfig.getConnectionPoolSize());
            Util.logSetting(log, "register-node-period", keycloakConfig.getRegisterNodePeriod());
            Util.logSetting(log, "register-node-at-startup", keycloakConfig.isRegisterNodeAtStartup());

            Util.logSetting(log, "disable-trust-manager", keycloakConfig.isDisableTrustManager());
            Util.logSetting(log, "truststore", keycloakConfig.getTruststore());
            Util.logSetting(log, "truststore-password", keycloakConfig.getTruststorePassword());
            Util.logSetting(log, "client-keystore", keycloakConfig.getClientKeystore());
            Util.logSetting(log, "client-keystore-password", keycloakConfig.getClientKeystorePassword());
        }
    }

    @Override
    public void stop() {
    }

    @Override
    public String getRealmName() {
        return realmName;
    }

    @Override
    public boolean canAuthenticate(AuthenticationToken authenticationToken) {
        return authenticationToken instanceof KeycloakToken;
    }

    @Override
    public Account authenticate(AuthenticationToken authenticationToken) {
        KeycloakToken keycloakToken = (KeycloakToken) authenticationToken;
        Account account = new Account(keycloakToken.getUsername(), keycloakToken);
        AccessToken accessToken = keycloakToken.getToken();

        if (!Strings.isNullOrEmpty(accessToken.getName())) {
            account.setName(accessToken.getName());
        }

        if (!Strings.isNullOrEmpty(accessToken.getEmail())) {
            account.addEmailAddress(accessToken.getEmail());
        }

        Set<String> roles = new TreeSet<>();
        // add realm roles
        AccessToken.Access realmAccess = accessToken.getRealmAccess();
        if (realmAccess != null) {
            roles.addAll(Optional.fromNullable(realmAccess.getRoles()).or(Collections.emptySet()));
        }

        // add resource roles
        for (Map.Entry<String, AccessToken.Access> entry : accessToken.getResourceAccess().entrySet()) {
            AccessToken.Access resourceAccess = entry.getValue();
            roles.addAll(Optional.fromNullable(resourceAccess.getRoles()).or(Collections.emptySet()));
        }

        // add account roles
        for (String role : roles) {
            if (definedRoles.containsKey(role)) {
                Role definedRole = definedRoles.get(role);
                account.getAuthorizations().addRole(definedRole);
            } else {
                account.getAuthorizations().addRole(role);
            }
        }

        cacheAccount(account);
        return account;
    }

    @Override
    public boolean hasAccount(String username) {
        return accountCache != null && accountCache.getIfPresent(username) != null;
    }

    @Override
    public Account getAccount(String username) {
        if (accountCache != null) {
            Account account = accountCache.getIfPresent(username);
            return account;
        }

        return null;
    }

    protected void cacheAccount(Account account) {
        if (accountCache != null) {
            accountCache.put(account.getUsername(), account);
        }
    }

    /**
     * Clears this Realm's account cache.
     */
    public void clearCache() {
        if (accountCache != null) {
            accountCache.invalidateAll();
        }
    }

    @Override
    public void logoutAll() {
        sessionIdMapper.clear();
    }

    @Override
    public void logoutHttpSessions(List<String> ids) {
        for (String id : ids) {
            log.trace("logout session ID: " + id);
            sessionIdMapper.removeSession(id);
        }
    }
}
