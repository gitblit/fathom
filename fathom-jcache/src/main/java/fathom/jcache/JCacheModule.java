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

import com.google.common.base.Strings;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import fathom.Module;
import fathom.conf.Settings;
import org.aopalliance.intercept.MethodInvocation;
import org.jsr107.ri.annotations.CacheContextSource;
import org.jsr107.ri.annotations.DefaultCacheKeyGenerator;
import org.jsr107.ri.annotations.DefaultCacheResolverFactory;
import org.jsr107.ri.annotations.guice.CacheLookupUtil;
import org.jsr107.ri.annotations.guice.CachePutInterceptor;
import org.jsr107.ri.annotations.guice.CacheRemoveAllInterceptor;
import org.jsr107.ri.annotations.guice.CacheRemoveEntryInterceptor;
import org.jsr107.ri.annotations.guice.CacheResultInterceptor;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;
import javax.cache.spi.CachingProvider;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fathom module which configures a JCache provider.
 *
 * @author James Moger
 */
@MetaInfServices
public final class JCacheModule extends Module {

    private static final Logger log = LoggerFactory.getLogger(JCacheModule.class);

    @Override
    protected void setup() {

        // Setup Hazelcast logging
        System.setProperty("hazelcast.logging.type", "slf4j");

        // setup the known cache providers
        Map<String, String> knownProviders = new HashMap<String, String>() {{
            put("org.infinispan.jcache.JCachingProvider", "infinispan");
            put("org.ehcache.jcache.JCacheCachingProvider", "ehcache");
            put("com.hazelcast.cache.HazelcastCachingProvider", "hazelcast");
        }};

        // collect the the available providers and identify the preferred provider
        String preferredName = Strings.emptyToNull(getSettings().getString(Settings.Setting.jcache_preferredProvider, null));
        CachingProvider preferredProvider = null;

        List<CachingProvider> providers = new ArrayList<>();
        for (CachingProvider provider : Caching.getCachingProviders()) {
            providers.add(provider);
            String providerClassName = provider.getClass().getName();
            if ((knownProviders.containsKey(providerClassName) && knownProviders.get(providerClassName).equals(preferredName))
                    || providerClassName.equals(preferredName)) {
                preferredProvider = provider;
            }
        }

        if (providers.isEmpty()) {

            log.debug("There are no JCache providers on the classpath.");
            // XXX JCache provider based on Guava would be really cool
            // @see https://github.com/ben-manes/caffeine/issues/6

        } else {

            if (preferredProvider == null) {
                preferredProvider = providers.get(0);
            }

            // register the cache service which handles graceful cleanup
            register(new JCache(getSettings(), preferredProvider));

            // optionally configure the JCache provider
            final String providerClassName = preferredProvider.getClass().getName();
            String keyName = knownProviders.get(providerClassName);

            if (!Strings.isNullOrEmpty(keyName)) {

                String configFile = Strings.emptyToNull(getSettings().getString(keyName + ".configurationFile", ""));
                if (Strings.isNullOrEmpty(configFile) || configFile.equalsIgnoreCase("jcache")) {
                    log.debug("Configuring JCache provider '{}' using internal provider defaults", keyName);
                } else {
                    try {
                        URL configFileUrl = getSettings().getFileUrl(keyName + ".configurationFile", "");
                        log.debug("Configuring JCache provider '{}' from '{}'", keyName, configFileUrl);
                        preferredProvider.getCacheManager(configFileUrl.toURI(), preferredProvider.getDefaultClassLoader());
                    } catch (URISyntaxException e) {
                        log.error("Failed to configure " + keyName, e);
                    }
                }
            }

            // Bind the preferred provider
            bind(CacheManager.class).toInstance(preferredProvider.getCacheManager());
            bind(CacheKeyGenerator.class).to(DefaultCacheKeyGenerator.class);
            bind(CacheResolverFactory.class).toInstance(new DefaultCacheResolverFactory(preferredProvider.getCacheManager()));
            bind(new TypeLiteral<CacheContextSource<MethodInvocation>>() {
            }).to(CacheLookupUtil.class);

            // install the Guice annotation interceptors
            CachePutInterceptor cachePutInterceptor = new CachePutInterceptor();
            requestInjection(cachePutInterceptor);
            bindInterceptor(Matchers.annotatedWith(CachePut.class), Matchers.any(), cachePutInterceptor);
            bindInterceptor(Matchers.any(), Matchers.annotatedWith(CachePut.class), cachePutInterceptor);

            CacheResultInterceptor cacheResultInterceptor = new CacheResultInterceptor();
            requestInjection(cacheResultInterceptor);
            bindInterceptor(Matchers.annotatedWith(CacheResult.class), Matchers.any(), cacheResultInterceptor);
            bindInterceptor(Matchers.any(), Matchers.annotatedWith(CacheResult.class), cacheResultInterceptor);

            CacheRemoveEntryInterceptor cacheRemoveEntryInterceptor = new CacheRemoveEntryInterceptor();
            requestInjection(cacheRemoveEntryInterceptor);
            bindInterceptor(Matchers.annotatedWith(CacheRemove.class), Matchers.any(), cacheRemoveEntryInterceptor);
            bindInterceptor(Matchers.any(), Matchers.annotatedWith(CacheRemove.class), cacheRemoveEntryInterceptor);

            CacheRemoveAllInterceptor cacheRemoveAllInterceptor = new CacheRemoveAllInterceptor();
            requestInjection(cacheRemoveAllInterceptor);
            bindInterceptor(Matchers.annotatedWith(CacheRemoveAll.class), Matchers.any(), cacheRemoveAllInterceptor);
            bindInterceptor(Matchers.any(), Matchers.annotatedWith(CacheRemoveAll.class), cacheRemoveAllInterceptor);
        }

    }

}