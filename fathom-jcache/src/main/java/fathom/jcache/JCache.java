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

package fathom.jcache;

import com.google.common.base.Optional;
import fathom.Service;
import fathom.conf.Settings;
import fathom.utils.ClassUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.spi.CachingProvider;

/**
 * The JCache service handles gracefully closing the CachingProvider.
 */
public class JCache implements Service {

    private static final Logger log = LoggerFactory.getLogger(JCache.class);

    private static final String CACHES_CLASS = "conf.Caches";

    private Settings settings;
    private CachingProvider cachingProvider;

    public JCache(Settings settings, CachingProvider cachingProvider) {
        this.settings = settings;
        this.cachingProvider = cachingProvider;
    }

    @Override
    public int getPreferredStartOrder() {
        return 10;
    }

    @Override
    public void start() {
        // add application caches module
        Optional<String> applicationPackage = Optional.fromNullable(settings.getApplicationPackage());
        String fullClassName = ClassUtil.buildClassName(applicationPackage, CACHES_CLASS);
        if (ClassUtil.doesClassExist(fullClassName)) {
            Class<?> moduleClass = ClassUtil.getClass(fullClassName);
            if (CachesModule.class.isAssignableFrom(moduleClass)) {
                CachesModule cachesModule = (CachesModule) ClassUtil.newInstance(moduleClass);
                if (cachesModule != null) {
                    log.info("Setting up JCache caches in '{}'", cachesModule.getClass().getName());
                    cachesModule.setup(settings, cachingProvider.getCacheManager());
                }
            }
        }
    }

    @Override
    public void stop() {
        cachingProvider.close();
        cachingProvider = null;
    }
}
