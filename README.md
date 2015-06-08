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

Fathom provides a **tightly integrated base** to quickly bootstrap your microservice project using best-of-breed components.

[Full documentation available here](http://fathom.gitblit.com).

### Opinionated

* [Undertow][1] is *the* development &amp; deployment engine
* [Guice][2] is *the* dependency injection mechanism
* [Guava][3] is *the* standard library
* [Config][4] is *the* configuration file parser
* [Args4j][5] is *the* command-line parsing framework
* [Logback][6] is *the* logging framework
* [SLF4J][7] is *the* logging interface
* **Java 8** is the baseline JVM

### Modular

Fathom features a formal `Module` spec, `Service` classes, and dependency injection combined with [Java service loaders](http://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html) that gives _you_ ultimate modularity.

### Similar alternatives

* [Restlet](http://restlet.com)
* [Ratpack](http://ratpack.io)
* [SparkJava](http://sparkjava.com)
* [Spring Boot](http://projects.spring.io/spring-boot)
* [Ninja](http://www.ninjaframework.org)
* [Play](https://playframework.com)
* [DropWizard](http://dropwizard.github.io/dropwizard)

### License

Distributed under the Apache Software License 2.0.

[1]: http://undertow.io
[2]: https://github.com/google/guice
[3]: https://code.google.com/p/guava-libraries
[4]: https://github.com/typesafehub/config
[5]: http://logback.qos.ch
[6]: http://args4j.kohsuke.org
[7]: http://www.slf4j.org
