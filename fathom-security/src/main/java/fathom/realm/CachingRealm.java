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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.typesafe.config.Config;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * A CachingRealm maintains an expiring Guava Cache of Accounts.
 *
 * @author James Moger
 */
public abstract class CachingRealm extends StandardCredentialsRealm {

    int cacheTtl;
    int cacheMax;
    private String realmName;
    private Cache<String, Account> accountCache;

    @Override
    public String getRealmName() {
        return realmName;
    }

    public void setup(Config config) {

        realmName = getClass().getSimpleName();
        if (config.hasPath("name")) {
            realmName = config.getString("name");
        }

        // configure an expiring account cache
        if (config.hasPath("cacheTtl")) {
            cacheTtl = config.getInt("cacheTtl");
        }

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
    }

    protected void logCacheSettings(Logger log) {
        logSetting(log, "caching", accountCache != null);
        logSetting(log, "cacheTtl (mins)", cacheTtl);
        logSetting(log, "cacheMax (accounts)", cacheMax);
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

}
