## About

**Fathom-Mailer** provides your application with an outgoing email capability.

**Fathom-Mailer** is based on [Sisu-Mailer](https://github.com/sonatype/sisu-mailer) and is integrated
with your chosen Pippo `TemplateEngine`.  This allows you to generate email notifications from templates rather
than wasting your time using `StringBuilder` and friends.

## Installation

Add the **Fathom-Mailer** artifact.

```XML
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-mailer</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

**Fathom-Mailer** depends on [Fathom-REST](rest.md) and a Pippo Template Engine.

## Configuration

Configure your mail server.
```properties
mail.server = smtp.gmail.com
mail.port = 465
mail.useSsl = true
mail.useTls = true
mail.username = username
mail.password = password
mail.debug = false
mail.systemName = Fathom
mail.systemAddress = fathom@gitblit.com
mail.bounceAddress = fathom@gitblit.com
```

## Usage

Inject `fathom.mailer.Mailer` when you need email sending.
