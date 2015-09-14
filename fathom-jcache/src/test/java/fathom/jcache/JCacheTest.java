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

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import conf.Caches;
import fathom.Services;
import fathom.conf.Settings;
import fathom.utils.ClassUtil;
import org.apache.onami.test.OnamiRunner;
import org.apache.onami.test.annotation.GuiceProvidedModules;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;

import static org.junit.Assert.assertEquals;

/**
 * @author James Moger
 */
@RunWith(OnamiRunner.class)
public class JCacheTest {

    @Inject
    private static Services services;

    @Inject
    private static Injector injector;

    @GuiceProvidedModules
    public static Module createTestModule() {
        return new AbstractModule() {

            @Override
            protected void configure() {
                Settings settings = new Settings();
                Services services = new Services(settings);

                bind(Settings.class).toInstance(settings);
                bind(Services.class).toInstance(services);

                JCacheModule jCacheModule = new JCacheModule();
                ClassUtil.setField(jCacheModule, "services", services);
                ClassUtil.setField(jCacheModule, "settings", settings);
                install(jCacheModule);

            }

        };
    }

    @BeforeClass
    public static void before() {
        services.start(injector);
    }

    @Test
    public void testStringLongCache() {

        final StringLongCache stringLongCache = injector.getInstance(StringLongCache.class);
        final String key = "testKey";
        long defaultValue = 20L;

        stringLongCache.clear();
        assertEquals(defaultValue, stringLongCache.get(key, defaultValue));
        assertEquals(defaultValue, stringLongCache.get(key, 0L));

        stringLongCache.update(key, 1L);
        assertEquals(1L, stringLongCache.get(key, defaultValue));

        stringLongCache.update(key, 2L);
        assertEquals(2L, stringLongCache.get(key, defaultValue));

        /* private methods can not be intercepted, this is a no-op */
        stringLongCache.set(key, 25L);
        assertEquals(2L, stringLongCache.get(key, defaultValue));

        stringLongCache.clear(key);
        assertEquals(defaultValue, stringLongCache.get(key, defaultValue));
        assertEquals(defaultValue, stringLongCache.get(key, 0L));

        stringLongCache.update(key, 3L);
        assertEquals(3L, stringLongCache.get(key, defaultValue));

        stringLongCache.update(key, 4L);
        assertEquals(4L, stringLongCache.get(key, defaultValue));

        stringLongCache.clear();
        assertEquals(defaultValue, stringLongCache.get(key, defaultValue));
        assertEquals(defaultValue, stringLongCache.get(key, 0L));
    }

    @Test
    public void testStringStringCache() {

        final StringStringCache stringStringCache = injector.getInstance(StringStringCache.class);
        final String key = "testKey";
        final String defaultValue = "pineapples";

        stringStringCache.clear();
        assertEquals(defaultValue, stringStringCache.get(key, defaultValue));
        assertEquals(defaultValue, stringStringCache.get(key, "blueberries"));

        stringStringCache.update(key, "apples");
        assertEquals("apples", stringStringCache.get(key, defaultValue));

        stringStringCache.update(key, "bananas");
        assertEquals("bananas", stringStringCache.get(key, defaultValue));

        /* private methods can not be intercepted, this is a no-op */
        stringStringCache.set(key, "oranges");
        assertEquals("bananas", stringStringCache.get(key, defaultValue));

        stringStringCache.clear(key);
        assertEquals(defaultValue, stringStringCache.get(key, defaultValue));
        assertEquals(defaultValue, stringStringCache.get(key, "blueberries"));

        stringStringCache.update(key, "pears");
        assertEquals("pears", stringStringCache.get(key, defaultValue));

        stringStringCache.update(key, "strawberries");
        assertEquals("strawberries", stringStringCache.get(key, defaultValue));

        stringStringCache.clear();
        assertEquals(defaultValue, stringStringCache.get(key, defaultValue));
        assertEquals(defaultValue, stringStringCache.get(key, "blueberries"));
    }

    @Test
    public void testLongStringCache() {

        final LongStringCache longStringCache = injector.getInstance(LongStringCache.class);
        final long key = 12345L;
        final String defaultValue = "pineapples";

        longStringCache.clear();
        assertEquals(defaultValue, longStringCache.get(key, defaultValue));
        assertEquals(defaultValue, longStringCache.get(key, "blueberries"));

        longStringCache.update(key, "apples");
        assertEquals("apples", longStringCache.get(key, defaultValue));

        longStringCache.update(key, "bananas");
        assertEquals("bananas", longStringCache.get(key, defaultValue));

        /* private methods can not be intercepted, this is a no-op */
        longStringCache.set(key, "oranges");
        assertEquals("bananas", longStringCache.get(key, defaultValue));

        longStringCache.clear(key);
        assertEquals(defaultValue, longStringCache.get(key, defaultValue));
        assertEquals(defaultValue, longStringCache.get(key, "blueberries"));

        longStringCache.update(key, "pears");
        assertEquals("pears", longStringCache.get(key, defaultValue));

        longStringCache.update(key, "strawberries");
        assertEquals("strawberries", longStringCache.get(key, defaultValue));

        longStringCache.clear();
        assertEquals(defaultValue, longStringCache.get(key, defaultValue));
        assertEquals(defaultValue, longStringCache.get(key, "blueberries"));
    }

    @CacheDefaults(cacheName = Caches.STRING_LONG_CACHE)
    public static class StringLongCache {

        /* private methods can not be intercepted, this is a no-op */
        @CachePut
        private void set(@CacheKey String key, @CacheValue long val) {
        }

        @CachePut
        public void update(@CacheKey String key, @CacheValue long val) {
        }

        @CacheResult
        public long get(@CacheKey String key, long defaultValue) {
            return defaultValue;
        }

        @CacheRemove
        public void clear(@CacheKey String key) {
        }

        @CacheRemoveAll
        public void clear() {
        }
    }

    @CacheDefaults(cacheName = Caches.STRING_STRING_CACHE)
    public static class StringStringCache {

        /* private methods can not be intercepted, this is a no-op */
        @CachePut
        private void set(@CacheKey String key, @CacheValue String val) {
        }

        @CachePut
        public void update(@CacheKey String key, @CacheValue String val) {
        }

        @CacheResult
        public String get(@CacheKey String key, String defaultValue) {
            return defaultValue;
        }

        @CacheRemove
        public void clear(@CacheKey String key) {
        }

        @CacheRemoveAll
        public void clear() {
        }
    }

    @CacheDefaults(cacheName = Caches.LONG_STRING_CACHE)
    public static class LongStringCache {

        /* private methods can not be intercepted, this is a no-op */
        @CachePut
        private void set(@CacheKey long key, @CacheValue String val) {
        }

        @CachePut
        public void update(@CacheKey long key, @CacheValue String val) {
        }

        @CacheResult
        public String get(@CacheKey long key, String defaultValue) {
            return defaultValue;
        }

        @CacheRemove
        public void clear(@CacheKey long key) {
        }

        @CacheRemoveAll
        public void clear() {
        }
    }
}