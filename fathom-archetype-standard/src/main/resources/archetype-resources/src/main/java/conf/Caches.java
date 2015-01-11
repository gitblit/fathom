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
import javax.cache.expiry.Duration;
import javax.cache.expiry.ModifiedExpiryPolicy;

/**
 * This class is used to conveniently build your JCaches.
 */
public class Caches extends CachesModule {

    public static final String ITEMS_CACHE = "itemsCache";

    @Override
    protected void setup(Settings settings, CacheManager cacheManager) {

        // example items cache
        cacheManager.createCache(ITEMS_CACHE, new MutableConfiguration()
                .setExpiryPolicyFactory(ModifiedExpiryPolicy.factoryOf(Duration.ONE_MINUTE))
                .setStatisticsEnabled(true)
                .setStoreByValue(true));
    }
}
