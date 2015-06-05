## About

**Fathom-Metrics** provides your application with [DropWizard Metrics] integration for runtime collection and reporting of and object that is instantiated by Guice.

## Installation

Add the **Fathom-Metrics** artifact...

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-metrics</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

... and optionally a **Fathom-Metrics** reporter:

| Reporter    | Module                                                  |
|-------------|---------------------------------------------------------|
| [InfluxDB]  | [com.gitblit.fathom:fathom-metrics-influxdb](#influxdb) |
| [Graphite]  | [com.gitblit.fathom:fathom-metrics-graphite](#graphite) |
| [Ganglia]   | [com.gitblit.fathom:fathom-metrics-ganglia](#ganglia)   |
| [Librato]   | [com.gitblit.fathom:fathom-metrics-librato](#librato)   |


## Configuration

```hocon
metrics {
  # Collect JVM metrics
  jvm = true

  # Report metrics via MBeans for JConsole, VisualVM, or JMX
  mbeans = true
}
```

## Usage

The most elegant way to use Fathom's Metrics integration is with annotations.

| Annotation | Description                                                                                |
|------------|--------------------------------------------------------------------------------------------|
| `@Counted` | A counter increments on method execution and optionally decrements at execution completion.|
| `@Metered` | A meter measures the rate of events over time (e.g., “requests per second”)                |
| `@Timed`   | A timer measures both the rate that a particular piece of code is called and the distribution of its duration. |

In the following example we will annotate several methods for metrics collection and the activity data will be transparently collected.

```java
@Singleton
public class EmployeeDao {

    private static final Logger log = LoggerFactory.getLogger(EmployeeDao.class);

    public EmployeeDao() {
    }

    @Metered
    public Employee get(int id) {
        log.info("Getting employee #{} by id", id);
        Employee employee = employees.get(id);
        return employee;
    }

    @Timed
    public List<Employee> getAll() {
        log.info("Getting all employees");
        return new ArrayList<>(employees.values());
    }

    @Counted
    public Employee delete(int id) {
        Employee employee = employees.get(id);
        employees.remove(id);
        return employee;
    }

    @Metered
    public Employee save(Employee employee) {
        return put(employee);
    }
  }
```

This transparent behavior is possible because the **Fathom-Metrics** module registers [AOP method interceptors] for the collection annotations AND [Guice] is being used to instantiate & inject your objects.

----

## VisualVM or JConsole

As long as `metrics.mbeans = true` in your runtime configuration, your metrics will be visible to [VisualVM] or [JConsole].

**Note:**<br/>
VisualVM does not ship with an MBeans viewer however there is an MBeans plugin that can be installed **from within** VisualVM to enable display of MBeans.

----

## InfluxDB

[InfluxDB] is a self-hosted or cloud-hosted metrics database and dashboard service.

### Installation

Add the **Fathom-Metrics-InfluxDB** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-metrics-influxdb</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

```hocon
metrics {
  influxdb {
    enabled = true
    address = localhost
    port = 8086
    database = mydb
    username = root
    password = root
    period = 60 seconds
  }
}
```

----

## Graphite

[Graphite] is a self-hosted or cloud-hosted metrics database and dashboard service.

### Installation

Add the **Fathom-Metrics-Graphite** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-metrics-graphite</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

```hocon
metrics {
  graphite {
    enabled = true
    address = localhost
    port = 8086
    pickled = false
    period = 60 seconds
  }
}
```
----

## Ganglia

[Ganglia] is a self-hosted metrics database and dashboard service.

### Installation

Add the **Fathom-Metrics-Ganglia** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-metrics-ganglia</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

```hocon
metrics {
  ganglia {
    enabled = true
    address = localhost
    port = 8086
    period = 60 seconds
  }
}
```

----

## Librato

[Librato] is a cloud-based metrics database and dashboard service.

### Installation

Add the **Fathom-Metrics-Librato** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-metrics-librato</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

```hocon
metrics {
  librato {
    enabled = true
    username = person@example.com
    apikey = 12345cafebabe
    period = 60 seconds
  }
}
```

[DropWizard Metrics]: https://dropwizard.github.io/metrics
[InfluxDB]: http://influxdb.com/
[Graphite]: https://github.com/graphite-project/graphite-web
[Ganglia]: http://ganglia.sourceforge.net
[Librato]: https://www.librato.com

[Guice]: https://github.com/google/guice
[AOP method interceptors]: https://github.com/google/guice/wiki/AOP

[VisualVM]: https://visualvm.java.net/
[JConsole]: http://openjdk.java.net/tools/svc/jconsole
