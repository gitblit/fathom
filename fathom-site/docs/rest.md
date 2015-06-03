## About

**Fathom-REST** is an opinionated, injectable, & thin scaffolding for the [Pippo Micro Webframework](http://pippo.ro).

Pippo is a modest, hackable framework for writing RESTful web applications.  It provides easy-to-use filtering, routing, template engine integrations, localization (i18n), [Webjars] support, and [pretty-time] datetime rendering.

## Installation

Add the **Fathom-REST** artifact.

```XML
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-rest</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

## Layout

```
YourApp
└── src
    ├── main
    │   ├── java
    │   │   ├── conf
    │   │   │   ├── Routes.java
    │   │   │   ├── messages.properties
    │   │   │   ├── messages_en.properties
    │   │   │   └── messages_ro.properties
    │   │   ├── controllers
    │   │   │   ├── EmployeeController.java
    │   │   │   └── ItemController.java
    │   │   └── templates
    │   │       ├── base.ftl
    │   │       ├── employee.ftl
    │   │       ├── employees.ftl
    │   │       ├── index.ftl
    │   │       └── login.ftl
    │   └── resources
    │       └── public
    │           └── css
    │               └── custom.css
    └── test
        ├── java
        │   ├── conf
        │   │   └── RoutesTest.java
        │   └── controllers
        │       └── ApiControllerTest.java
        └── resources
```

## Configuration

```Java
package conf;

public class Routes extends RoutesModule {
  GET("/" (ctx) -> {
    ctx.send("Hello, World!");
  });
}
```

## Template Engines

| Engine         | Artifact                     |
|----------------|------------------------------|
| [Freemarker]   | [ro.pippo:pippo-freemarker]  |
| [Jade]         | [ro.pippo:pippo-jade]        |
| [Pebble]       | [ro.pippo:pippo-pebble]      |
| [Trimou]       | [ro.pippo:pippo-trimou]      |
| [Groovy]       | [ro.pippo:pippo-groovy]      |

## Content-Type Engines

| Content-Type | Engine           | Artifact                     |
|--------------|------------------|------------------------------|
| XML          | JAXB             | *default, built-in*          |
| XML          | [XStream]        | [ro.pippo:pippo-xstream]     |
| JSON         | [GSON]           | [ro.pippo:pippo-gson]        |
| JSON         | [FastJSON]       | [ro.pippo:pippo-fastjson]    |
| JSON         | [Jackson]        | [ro.pippo:pippo-jackson]     |
| YAML         | [SnakeYAML]      | [ro.pippo:pippo-snakeyaml]   |


[Webjars]: http://www.webjars.org
[pretty-time]: http://www.ocpsoft.org/prettytime
[Freemarker]: http://freemarker.org
[Jade]: https://github.com/neuland/jade4j
[Pebble]: http://www.mitchellbosecke.com/pebble/home
[Trimou]: http://trimou.org
[Groovy]: https://github.com/decebals/pippo/tree/master/pippo-groovy

[XStream]: https://github.com/x-stream/xstream
[GSON]: https://github.com/google/gson
[FastJSON]: https://github.com/alibaba/fastjson
[Jackson]: https://github.com/FasterXML/jackson
[SnakeYAML]: https://bitbucket.org/asomov/snakeyaml

[ro.pippo:pippo-freemarker]: http://search.maven.org/#search|ga|1|g:"ro.pippo"%20AND%20a:"pippo-freemarker"
[ro.pippo:pippo-jade]: http://search.maven.org/#search|ga|1|g:"ro.pippo"%20AND%20a:"pippo-jade"
[ro.pippo:pippo-pebble]: http://search.maven.org/#search|ga|1|g:"ro.pippo"%20AND%20a:"pippo-pebble"
[ro.pippo:pippo-trimou]: http://search.maven.org/#search|ga|1|g:"ro.pippo"%20AND%20a:"pippo-trimou"
[ro.pippo:pippo-groovy]: http://search.maven.org/#search|ga|1|g:"ro.pippo"%20AND%20a:"pippo-groovy"

[ro.pippo:pippo-xstream]: http://search.maven.org/#search|ga|1|g:"ro.pippo"%20AND%20a:"pippo-xstream"
[ro.pippo:pippo-gson]: http://search.maven.org/#search|ga|1|g:"ro.pippo"%20AND%20a:"pippo-gson"
[ro.pippo:pippo-fastjson]: http://search.maven.org/#search|ga|1|g:"ro.pippo"%20AND%20a:"pippo-fastjson"
[ro.pippo:pippo-jackson]: http://search.maven.org/#search|ga|1|g:"ro.pippo"%20AND%20a:"pippo-jackson"
[ro.pippo:pippo-snakeyaml]: http://search.maven.org/#search|ga|1|g:"ro.pippo"%20AND%20a:"pippo-snakeyaml"
