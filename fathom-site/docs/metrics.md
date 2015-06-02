## About

**Fathom-Metrics** provides your application with [DropWizard Metrics][] integration for runtime collection and reporting.

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

| Reporter      | Coordinates                                |
|---------------|--------------------------------------------|
| [InfluxDB][]  | com.gitblit.fathom:fathom-metrics-influxdb |
| [Graphite][]  | com.gitblit.fathom:fathom-metrics-graphite |
| [Ganglia][]   | com.gitblit.fathom:fathom-metrics-ganglia  |
| [Librato][]   | com.gitblit.fathom:fathom-metrics-librato  |


## Configuration


[DropWizard Metrics]: https://dropwizard.github.io/metrics
[InfluxDB]: http://influxdb.com/
[Graphite]: https://github.com/graphite-project/graphite-web
[Ganglia]: http://ganglia.sourceforge.net
[Librato]: https://www.librato.com
