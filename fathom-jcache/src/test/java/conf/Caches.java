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

package conf;

import fathom.conf.Settings;
import fathom.jcache.CachesModule;

import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.EternalExpiryPolicy;

/**
 * @author James Moger
 */
public class Caches extends CachesModule {

    public static final String STRING_LONG_CACHE = "StringLongCache";

    public static final String STRING_STRING_CACHE = "StringStringCache";

    public static final String LONG_STRING_CACHE = "LongStringCache";

    @Override
    protected void setup(Settings settings, CacheManager cacheManager) {
        cacheManager.createCache(STRING_LONG_CACHE, new MutableConfiguration()
                .setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf())
                .setStatisticsEnabled(true)
                .setStoreByValue(true));

        cacheManager.createCache(STRING_STRING_CACHE, new MutableConfiguration()
                .setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf())
                .setStatisticsEnabled(true)
                .setStoreByValue(true));

        cacheManager.createCache(LONG_STRING_CACHE, new MutableConfiguration()
                .setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf())
                .setStatisticsEnabled(true)
                .setStoreByValue(true));

    }
}
