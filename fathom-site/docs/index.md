## Fathom

Fathom is an opinionated, modular, & injectable foundation for building microservices on a JVM.

<pre>
    (_)        ______      __  __
   __|__      / ____/___ _/ /_/ /_  ____  ____ ___
     |       / /_  / __ `/ __/ __ \/ __ \/ __ `__ \  Microservice Foundation
 \__/ \__/  / __/ / /_/ / /_/ / / / /_/ / / / / / /  http://fathom.gitblit.com
  °-. .-°  /_/    \__,_/\__/_/ /_/\____/_/ /_/ /_/
     '
</pre>

### Microservice Foundation

Fathom provides a base to quickly bootstrap your microservice project using best-of-breed components.

### Opinionated

* [Undertow][1] is *the* development &amp; deployment engine
* [Guice][2] is *the* dependency injection mechanism
* [Guava][3] is *the* standard library
* [Config][4] is *the* configuration file parser
* [Args4j][5] is *the* command-line parsing framework
* [Logback][6] is *the* logging framework
* [Commons-Daemon][7] is *the* service control integration
* *Java 8* is the baseline JVM

### Modular & Injectable

Fathom features a formal `Module` spec, `Service` classes, and dependency injection combined with the [Service Locator
design](http://martinfowler.com/articles/injection.html#UsingAServiceLocator) that gives _you_ ultimate modularity.

Modules & services may be optionally disabled based on runtime mode and/or config settings.

### License

Distributed under the Apache Software License 2.0.

[1]: http://undertow.io
[2]: https://github.com/google/guice
[3]: https://code.google.com/p/guava-libraries
[4]: https://github.com/typesafehub/config
[5]: http://logback.qos.ch
[6]: http://args4j.kohsuke.org
[7]: http://commons.apache.org/proper/commons-daemon
