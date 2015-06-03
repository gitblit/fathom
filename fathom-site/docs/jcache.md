## About

**Fathom-JCache** provides your application with seamless caching integration.

## Installation

Add the **Fathom-JCache** artifact...

```XML
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-jcache</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

... and also add your preferred JCache provider:

| Provider      | Artifact                           |
|---------------|------------------------------------|
| [Hazelcast]   | [com.hazelcast:hazelcast]          |
| [Infinispan]  | [org.infinispan:infinispan-jcache] |
| [Ehcache]     | [org.ehcache:jcache]               |

## Layout

```
YourApp
└── src
    └── main
        └── java
            └── conf
                └── Caches.java
```

## Configuration

```java
package conf;

/**
 * This class is used to conveniently build your JCaches.
 */
public class Caches extends CachesModule {

    public static final String ITEMS_CACHE = "itemsCache";

    public static final String EMPLOYEE_CACHE = "employeeCache";

    @Override
    protected void setup(Settings settings, CacheManager cacheManager) {

        // example items cache
        cacheManager.createCache(ITEMS_CACHE, new MutableConfiguration()
                .setExpiryPolicyFactory(ModifiedExpiryPolicy.factoryOf(Duration.ONE_MINUTE))
                .setStatisticsEnabled(true)
                .setStoreByValue(true));

        // example employee cache
        cacheManager.createCache(EMPLOYEE_CACHE, new MutableConfiguration()
                .setExpiryPolicyFactory(ModifiedExpiryPolicy.factoryOf(Duration.ONE_MINUTE))
                .setStatisticsEnabled(true)
                .setStoreByValue(true));
    }
}
```

[Hazelcast]: http://hazelcast.org
[Infinispan]: http://infinispan.org
[Ehcache]: http://ehcache.org

[com.hazelcast:hazelcast]: http://search.maven.org/#search|ga|1|g:"com.hazelcast"%20AND%20a:"hazelcast"
[org.infinispan:infinispan-jcache]: http://search.maven.org/#search|ga|1|g:"org.infinispan"%20AND%20a:"infinispan-jcache"
[org.ehcache:jcache]: http://search.maven.org/#search|ga|1|g:"org.ehcache"%20AND%20a:"jcache"
