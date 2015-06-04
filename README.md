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

Fathom provides a base to quickly bootstrap your microservice project using [Undertow][1], [Guice][2],
[Guava][3], [Config][4], [Args4j][5], [Logback][6], and [Commons-Daemon][7].

> Fathom applications are _developed-on_ and _deployed-with_ [Undertow][1].
> You do not build WARs with Fathom; you build integrated server applications.

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

Fathom features a formal `Module` spec, `Service` classes, and dependency injection combined with the [Service Locator
design](http://martinfowler.com/articles/injection.html#UsingAServiceLocator) that gives _you_ ultimate modularity.

Modules & services may be optionally disabled based on runtime mode and/or config settings.

There are several modules available to build your Fathom foundation:

- [JCache](/fathom-jcache/) module for [Hazelcast](http://hazelcast.org), [Infinispan](http://infinispan.org), or [Ehcache](http://ehcache.org).
- [Metrics](/fathom-metrics/) module for runtime metrics collection
- [EventBus](/fathom-eventbus/) module for decoupled event passing
- [Mailer](/fathom-mailer/) module for email sending
- [Quartz](/fathom-quartz/) module for Quartz scheduling integration
- [REST](/fathom-rest/) module for RESTful routing & controllers based on [Pippo][8]
    - [Security](/fathom-rest-security/) module for RESTful Authentication & Authorization against PAM, Windows, LDAP, JDBC, Redis, etc.
    - [Shiro](/fathom-rest-shiro/) module for RESTful Authenication & Authorization
    - Multiple content-type engine choices
        - JAXB _(default)_
        - [XStream](https://github.com/decebals/pippo/tree/master/pippo-xstream)
        - [GSON](https://github.com/decebals/pippo/tree/master/pippo-gson)
        - [FastJSON](https://github.com/decebals/pippo/tree/master/pippo-fastjson)
        - [Jackson](https://github.com/decebals/pippo/tree/master/pippo-jackson)
        - [SnakeYAML](https://github.com/decebals/pippo/tree/master/pippo-snakeyaml)
    - Multiple template engine choices with [Webjars](http://www.webjars.org) support, localization (i18n), & [pretty-time](http://www.ocpsoft.org/prettytime) integration.
        - [Freemarker](https://github.com/decebals/pippo/tree/master/pippo-freemarker)
        - [Jade](https://github.com/decebals/pippo/tree/master/pippo-jade)
        - [Pebble](https://github.com/decebals/pippo/tree/master/pippo-pebble)
        - [Trimou](https://github.com/decebals/pippo/tree/master/pippo-trimou)
        - [Groovy](https://github.com/decebals/pippo/tree/master/pippo-groovy)

### Testing

* Integration testing based on [Rest-Assured](https://code.google.com/p/rest-assured).

### Deployment

* Configurable runtime profiles
* Three runtime modes: PROD, TEST, & DEV
* Flexible config file with runtime-mode setting variations, property substitutions, & environment variable substitutions
* Integrated support for HTTPS, HTTP, & AJP transports
* Integrated [Commons-Daemon][7] service support for Windows & UNIX
* Distribution bundling with [Stork](https://github.com/fizzed/stork)

### Footprint

The footprint of a _Hello World_ Fathom servlet-based microservice is about *7.5MB*.

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
[8]: http://www.pippo.ro
