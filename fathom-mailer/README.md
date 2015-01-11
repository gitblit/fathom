Fathom Mailer
=====================

Module for sending emails.

Setup
-----

1) Add the dependency to your pom.xml:
```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-mailer</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

2) Configure your mail server.
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

3) Inject `fathom.mailer.Mailer` when you need email sending.

Usage
-----

Fathom Mailer is based on [Sisu-Mailer](https://github.com/sonatype/sisu-mailer) and is integrated
with your chosen `TemplateEngine`.  This allows you to generate email notifications from templates rather
than wasting your time using `StringBuilder` and friends.

