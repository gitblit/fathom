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
!!! Note
    This *module* depends on the value of the `application.package` setting.  If you have specified an application package then your Caches class must be `${package}/conf/Caches.java`.

## Configuration

You don't have to configure your JCache provider to use it.  Each provider ships with default settings which allows you to get up-and-running very quickly.

If you need to specify a provider-specific configuration you may do so.

```hocon
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
### Multiple Providers on the classpath

In the event that you have multiple providers on the classpath at the same time, Fathom will pick the first one loaded by the JVM **unless** you specify a preference.

```hocon
# Set the preferred JCache provider.
# If unspecified, the first provider discovered will be used.
# You may specify a full provider classname or you may specify
# a nickname for an already registered JCache implementation
# such as: ehcache, infinispan, hazelcast
jcache.preferredProvider = ""
```
One scenario where you might have multiple providers is using one for `DEV` mode and a different one for `PROD` mode.  This would be the caching analog of using an SQL db like SQLite or H2 for development and PostgreSQL for production.

## Usage

### Defining your Caches

Create a `conf/Caches.java` class.

```java
package conf;

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

In the following non-functional example we are annotating our employee DAO singleton to specify that all methods in this DAO access the *EMPLOYEE_CACHE* defined in our `conf/Caches.java` class.

We are also annotating the `get`, `getAll`, `delete`, and `save` methods.  Calling these methods will transparently manage the cache.

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

!!! Warning
    It's important to note that the JCache method interceptors, like all Guice method interceptors, are bound by the same limitations - including method scope.

    - Classes must be public or package-private.
    - Classes must be non-final
    - Methods must be public, package-private or protected
    - Methods must be non-final
    - Instances must be created by Guice by an @Inject-annotated or no-argument constructor It is not possible to use method interception on instances that aren't constructed by Guice.

### Viewing and Managing your Caches

Fathom does not provide a mechansim to view & manage your caches, however, several JCache providers register [MBeans] which allow cache viewing and management via [JConsole] or [VisualVM].

[Hazelcast]: http://hazelcast.org
[Infinispan]: http://infinispan.org
[Ehcache]: http://ehcache.org

[Guice]: https://github.com/google/guice
[com.hazelcast:hazelcast]: http://search.maven.org/#search|ga|1|g:"com.hazelcast"%20AND%20a:"hazelcast"
[org.infinispan:infinispan-jcache]: http://search.maven.org/#search|ga|1|g:"org.infinispan"%20AND%20a:"infinispan-jcache"
[org.ehcache:jcache]: http://search.maven.org/#search|ga|1|g:"org.ehcache"%20AND%20a:"jcache"
[MBeans]: https://en.wikipedia.org/wiki/Java_Management_Extensions
[VisualVM]: https://visualvm.java.net/
[JConsole]: http://openjdk.java.net/tools/svc/jconsole
