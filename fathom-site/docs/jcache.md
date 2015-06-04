## About

**Fathom-JCache** provides your application with seamless caching integration.

## Installation

Add the **Fathom-JCache** artifact...

```xml
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

```hocon
# Set the preferred JCache provider.
# If unspecified, the first provider discovered will be used.
# You may specify a full provider classname or you may specify
# a nickname for an already registered JCache implementation
# such as: ehcache, infinispan, hazelcast
jcache.preferredProvider = ""

# Ehcache configuration file
# see http://ehcache.org/documentation
# specify 'jcache' to default to the Jcache default configuration
# ehcache.configurationFile = "classpath:conf/ehcache.xml"
ehcache.configurationFile = "jcache"

# Infinispan configuration file
# see http://infinispan.org/docs/7.0.x/user_guide/user_guide.html
# specify 'jcache' to default to the Jcache default configuration
# infinispan.configurationFile = "classpath:conf/infinispan.xml"
infinispan.configurationFile = "jcache"

# Hazelcast configuration file
# see http://hazelcast.org/documentation
# specify 'jcache' to default to the Jcache default configuration
# hazelcast.configurationFile = "classpath:conf/hazelcast.xml"
hazelcast.configurationFile = "jcache"
```

## Usage

### Defining your Caches

Create a `conf/Caches.java` class.

```java
package conf;

/**
 * This class is used to conveniently define your JCaches.
 */
public class Caches extends CachesModule {

    public static final String EMPLOYEE_CACHE = "employeeCache";

    @Override
    protected void setup(Settings settings, CacheManager cacheManager) {

        // example employee cache
        cacheManager.createCache(EMPLOYEE_CACHE, new MutableConfiguration()
                .setExpiryPolicyFactory(ModifiedExpiryPolicy.factoryOf(Duration.ONE_MINUTE))
                .setStatisticsEnabled(true)
                .setStoreByValue(true));
    }
}
```

### Using your Caches

The most elegant way to use Fathom's JCache integration is with annotations.

In the following non-functional example we are annotating our employee DAO singleton to specify that all methods in this DAO access the *EMPLOYEE_CACHE* defined in our `conf/Caches` class.

We are also annotating the `get`, `getAll`, `delete`, and `save` methods.  Calling these methods will transparently populate & clear the cache.

This transparent behavior is possible because the **Fathom-JCache** module registers [AOP method interceptors] for the JCache annotations AND [Guice] is being used to instantiate & inject the DAO instance.

```java
@Singleton
@CacheDefaults(cacheName = Caches.EMPLOYEE_CACHE)
public class EmployeeDao {

    private static final Logger log = LoggerFactory.getLogger(EmployeeDao.class);

    public EmployeeDao() {
    }

    @CacheResult
    public Employee get(int id) {
        log.info("Getting employee #{} by id", id);
        Employee employee = employees.get(id);
        return employee;
    }

    @CacheResult
    public List<Employee> getAll() {
        log.info("Getting all employees");
        return new ArrayList<>(employees.values());
    }

    @CacheRemoveAll
    public Employee delete(int id) {
        Employee employee = employees.get(id);
        employees.remove(id);
        return employee;
    }

    @CacheRemoveAll
    public Employee save(Employee employee) {
        return put(employee);
    }
  }
```

[Hazelcast]: http://hazelcast.org
[Infinispan]: http://infinispan.org
[Ehcache]: http://ehcache.org

[Guice]: https://github.com/google/guice
[AOP method interceptors]: https://github.com/google/guice/wiki/AOP
[com.hazelcast:hazelcast]: http://search.maven.org/#search|ga|1|g:"com.hazelcast"%20AND%20a:"hazelcast"
[org.infinispan:infinispan-jcache]: http://search.maven.org/#search|ga|1|g:"org.infinispan"%20AND%20a:"infinispan-jcache"
[org.ehcache:jcache]: http://search.maven.org/#search|ga|1|g:"org.ehcache"%20AND%20a:"jcache"
