## Fathom

Fathom is an opinionated, modular, & injectable foundation for building microservices on a JVM.

<pre>
    (_)        ______      __  __
   __|__      / ____/___ _/ /_/ /_  ____  ____ ___
     |       / /_  / __ `/ __/ __ \/ __ \/ __ `__ \
 \__/ \__/  / __/ / /_/ / /_/ / / / /_/ / / / / / /
  °-. .-°  /_/    \__,_/\__/_/ /_/\____/_/ /_/ /_/
     '
</pre>

### Microservice Foundation

Fathom provides a **tightly integrated base** to quickly bootstrap your microservice project using best-of-breed components.

### Opinionated

* [Undertow] is *the* development &amp; deployment engine
* [Guice] is *the* dependency injection mechanism
* [Guava] is *the* standard library
* [Config] is *the* configuration file parser
* [Args4j] is *the* command-line parsing framework
* [Logback] is *the* logging framework
* [SLF4J] is *the* logging facade
* **Java 8** is the baseline JVM

The footprint of the **Fathom-Core** foundation is ~7.5MB.

### Modular & Injectable

Fathom features a formal [Module](modules.md) specification, [Service](services.md) infrastructure, and dependency injection all combined with [Java ServiceLoader](http://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html) to give _you_ ultimate modularity.

### License

Distributed under the Apache Software License 2.0.

[Undertow]: http://undertow.io
[Guice]: https://github.com/google/guice
[Guava]: https://github.com/google/guava
[Config]: https://github.com/typesafehub/config
[Logback]: http://logback.qos.ch
[SLF4J]: http://www.slf4j.org
[Args4j]: http://args4j.kohsuke.org
