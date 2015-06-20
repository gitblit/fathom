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

package fathom.security;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import fathom.Constants;
import fathom.Service;
import fathom.authc.AuthenticationToken;
import fathom.authz.Role;
import fathom.conf.Settings;
import fathom.exception.FathomException;
import fathom.realm.Account;
import fathom.realm.CachingRealm;
import fathom.realm.Realm;
import fathom.utils.ClassUtil;
import fathom.utils.RequireUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * SecurityManager manages the Realms and handles authentication.
 *
 * @author James Moger
 */
@Singleton
public class SecurityManager implements Service {

    private static final Logger log = LoggerFactory.getLogger(SecurityManager.class);

    @Inject
    private Injector injector;

    @Inject
    private Settings settings;

    private Collection<Realm> allRealms;

    private Cache<AuthenticationToken, Account> accountCache;

    @Override
    public int getPreferredStartOrder() {
        return 50;
    }

    @Override
    public void start() {
        allRealms = Collections.emptyList();

        // configure the SecurityManager
        URL configFileUrl = settings.getFileUrl("security.configurationFile", "classpath:conf/realms.conf");

        if (configFileUrl == null) {
            throw new FathomException("Failed to find Security Realms file '{}'",
                    settings.getString("security.configurationFile", "classpath:conf/realms.conf"));
        }

        Config config;
        try (InputStreamReader reader = new InputStreamReader(configFileUrl.openStream())) {
            config = ConfigFactory.parseReader(reader);
            log.info("Configured Security Realms from '{}'", configFileUrl);
        } catch (IOException e) {
            throw new FathomException(e, "Failed to parse Security Realms file '{}'", configFileUrl);
        }

        allRealms = parseDefinedRealms(config);

        // configure an expiring account cache
        int cacheTtl = 0;
        if (config.hasPath("cacheTtl")) {
            cacheTtl = config.getInt("cacheTtl");
        }

        int cacheMax = 100;
        if (config.hasPath("cacheMax")) {
            cacheMax = config.getInt("cacheMax");
        }

        if (cacheTtl > 0 && cacheMax > 0) {
            accountCache = CacheBuilder
                    .newBuilder()
                    .expireAfterAccess(cacheTtl, TimeUnit.MINUTES)
                    .maximumSize(cacheMax)
                    .build();
        }

        String border = Strings.padEnd("", Constants.MIN_BORDER_LENGTH, '-');
        log.info(border);
        log.info("Starting realms");
        log.info(border);
        for (Realm realm : allRealms) {
            log.debug("{} '{}'", realm.getClass().getName(), realm.getRealmName());
        }
        for (Realm realm : allRealms) {
            try {
                log.info("Starting realm '{}'", realm.getRealmName());
                realm.start();
            } catch (Exception e) {
                log.error("Failed to start realm '{}'", realm.getRealmName(), e);
            }
        }
    }

    @Override
    public void stop() {
        clearCache();

        for (Realm realm : allRealms) {
            try {
                log.debug("Stopping realm '{}'", realm.getRealmName());
                realm.stop();
            } catch (Exception e) {
                log.error("Failed to stop realm '{}'", realm.getRealmName(), e);
            }
        }
    }

    /**
     * Tries to authenticate an AuthenticationToken.
     *
     * @param authenticationToken
     * @return an Account instance if authentication is successful
     */
    public Account authenticate(AuthenticationToken authenticationToken) {

        if (accountCache != null) {
            Account account = accountCache.getIfPresent(authenticationToken);
            if (account != null) {
                return account;
            }
        }

        Account authenticatedAccount = null;
        for (Realm realm : allRealms) {
            if (realm.canAuthenticate(authenticationToken)) {
                Account account = realm.authenticate(authenticationToken);
                if (account != null && !account.isDisabled()) {
                    // create a sanitized copy of this account
                    authenticatedAccount = new Account(account.getName(), account.getCredentials().sanitize());
                    break;
                }
            }
        }

        // no authentication
        if (authenticatedAccount == null) {
            return null;
        }

        // aggregate metadata, roles, & permissions
        final Account aggregateAccount = authenticatedAccount;
        allRealms.stream()
                .filter(realm -> realm.hasAccount(aggregateAccount.getUsername()))
                .map(realm -> realm.getAccount(aggregateAccount.getUsername()))
                .filter(account -> account.isEnabled())
                .forEach(account -> {
                    if (Strings.isNullOrEmpty(aggregateAccount.getName())) {
                        aggregateAccount.setName(account.getName());
                    }
                    aggregateAccount.addEmailAddresses(account.getEmailAddresses());
                    aggregateAccount.addTokens(account.getTokens());
                    aggregateAccount.getAuthorizations()
                            .addRoles(account.getAuthorizations().getRoles())
                            .addPermissions(account.getAuthorizations().getPermissions());
                });

        if (accountCache != null) {
            // cache this assembled account
            accountCache.put(authenticationToken, aggregateAccount);
        }

        return aggregateAccount;
    }

    /**
     * Clears the SecurityManager account cache and any CachingRealm's cache.
     * MemoryRealms are not affected by this call.
     */
    public void clearCache() {
        if (accountCache != null) {
            accountCache.invalidateAll();
        }
        for (Realm realm : allRealms) {
            if (realm instanceof CachingRealm) {
                CachingRealm cachingRealm = (CachingRealm) realm;
                cachingRealm.clearCache();
            }
        }
    }

    /**
     * Parse the Realms from the Config object.
     *
     * @param config
     * @return an ordered collection of Realms
     */
    protected Collection<Realm> parseDefinedRealms(Config config) {

        List<Realm> realms = new ArrayList<>();

        // Parse the Realms
        if (config.hasPath("realms")) {
            log.trace("Parsing Realm definitions");
            for (Config realmConfig : config.getConfigList("realms")) {
                // define the realm name and type
                String realmType = Strings.emptyToNull(realmConfig.getString("type"));
                Preconditions.checkNotNull(realmType, "Realm 'type' is null!");

                if (ClassUtil.doesClassExist(realmType)) {
                    Class<? extends Realm> realmClass = ClassUtil.getClass(realmType);
                    if (RequireUtil.allowClass(settings, realmClass)) {
                        Realm realm = injector.getInstance(realmClass);
                        realm.setup(realmConfig);
                        realms.add(realm);
                        log.debug("Created '{}' named '{}'", realmType, realm.getRealmName());
                    }
                } else {
                    throw new FathomException("Unknown realm type '{}'!", realmType);
                }

            }
        }

        return Collections.unmodifiableList(realms);
    }
}
