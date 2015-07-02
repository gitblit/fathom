package ${package}.conf;

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
