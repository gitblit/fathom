## About

**Fathom-Mailer** provides your application with an outgoing email capability.

**Fathom-Mailer** is based on [Sisu-Mailer](https://github.com/sonatype/sisu-mailer), depends on [Fathom-REST](rest.md), and is integrated
with your chosen [Pippo Template Engine](rest/#template-engines).

## Installation

Add the **Fathom-Mailer** artifact.

```XML
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-mailer</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

## Configuration

Configure your mail server.

```hocon
mail {
  server = smtp.gmail.com
  port = 465
  useSsl = true
  useTls = true
  username = username
  password = password
  debug = false
  systemName = Fathom
  systemAddress = fathom@gitblit.com
  bounceAddress = fathom@gitblit.com
}
```

## Usage

Inject `fathom.mailer.Mailer` when you need email sending.

```java
@ControllerPath("/example")
public class MyController extends Controller {

  @Inject
  Mailer mailer;

}
```
