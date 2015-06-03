## About

**Fathom-Metrics** provides your application with [DropWizard Metrics] integration for runtime collection and reporting.

## Installation

Add the **Fathom-Metrics** artifact...

```XML
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

## InfluxDB

### Installation

Add the **Fathom-Metrics-InfluxDB** artifact.

```XML
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-metrics-influxdb</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

```hocon
```

----

## Graphite

### Installation

Add the **Fathom-Metrics-Graphite** artifact.

```XML
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-metrics-graphite</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

```hocon
```

----

## Ganglia

### Installation

Add the **Fathom-Metrics-Ganglia** artifact.

```XML
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-metrics-ganglia</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

```hocon
```

----

## Librato

### Installation

Add the **Fathom-Metrics-Librato** artifact.

```XML
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-metrics-librato</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

```hocon
```

[DropWizard Metrics]: https://dropwizard.github.io/metrics
[InfluxDB]: http://influxdb.com/
[Graphite]: https://github.com/graphite-project/graphite-web
[Ganglia]: http://ganglia.sourceforge.net
[Librato]: https://www.librato.com
