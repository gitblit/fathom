## About

**Fathom-REST** is an opinionated, injectable, & thin scaffolding for the [Pippo Micro Webframework](http://pippo.ro).

## Installation

Add the **Fathom-REST** artifact.

```XML
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-rest</artifactId>
    <version>${fathom.version}</version>
</dependency>
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

### Template Engines

All template engine choices support localization (i18n), [Webjars](http://www.webjars.org), & [pretty-time](http://www.ocpsoft.org/prettytime) datetime rendering.

| Engine           | Coordinates                  |
|------------------|------------------------------|
| [Freemarker][]   | ro.pippo:pippo-freemarker    |
| [Jade][]         | ro.pippo:pippo-jade          |
| [Pebble][]       | ro.pippo:pippo-pebble        |
| [Trimou][]       | ro.pippo:pippo-trimou        |
| [Groovy][]       | ro.pippo:pippo-groovy        |

### Content-Type Engines

| Content-Type | Engine           | Coordinates                  |
|--------------|------------------|------------------------------|
| XML          | JAXB _(default)_ |                              |
| XML          | [XStream][]      | ro.pippo:pippo-xstream       |
| JSON         | [GSON][]         | ro.pippo:pippo-gson          |
| JSON         | [FastJSON][]     | ro.pippo:pippo-fastjson      |
| JSON         | [Jackson][]      | ro.pippo:pippo-jackson       |
| YAML         | [SnakeYAML][]    | ro.pippo:pippo-snakeyaml     |


[Freemarker]: https://github.com/decebals/pippo/tree/master/pippo-freemarker
[Jade]: https://github.com/decebals/pippo/tree/master/pippo-jade
[Pebble]: https://github.com/decebals/pippo/tree/master/pippo-pebble
[Trimou]: https://github.com/decebals/pippo/tree/master/pippo-trimou
[Groovy]: https://github.com/decebals/pippo/tree/master/pippo-groovy
[XStream]: https://github.com/decebals/pippo/tree/master/pippo-xstream
[GSON]: https://github.com/decebals/pippo/tree/master/pippo-gson
[FastJSON]: https://github.com/decebals/pippo/tree/master/pippo-fastjson
[Jackson]: https://github.com/decebals/pippo/tree/master/pippo-jackson
[SnakeYAML]: https://github.com/decebals/pippo/tree/master/pippo-snakeyaml
