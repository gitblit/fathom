## About

**Fathom-Mailer** provides your application with a thread-safe, singleton instance of a mail service.

**Fathom-Mailer** is based on [Sisu-Mailer](https://github.com/sonatype/sisu-mailer), depends on [Fathom-REST](rest.md), and is integrated
with your chosen [Pippo Template Engine](rest/#template-engines).

## Installation

Add the **Fathom-Mailer** artifact.

```xml
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
  # SMTP connection parameters
  server = smtp.gmail.com
  port = 465
  useSsl = true
  useTls = true

  # SMTP authentication credentials
  username = username
  password = password

  # The 'from' name and address for sent emails
  systemName = Fathom
  systemAddress = fathom@gitblit.com

  # The recipient address for bounce emails from your SMTP server
  bounceAddress = fathom@gitblit.com

  # Enable verbose debug logging
  debug = false
}
```

## Usage

Inject the `Mailer` where you need the mail service.

```java
@Singleton
public class MyPostOffice {

  @Inject
  Mailer mailer;

}
```

### Sending a Text Email

```java
MailRequest request = mailer.newTextMailRequest("Plain Text Message", "This is a plain text message");
request.getToAddresses().add(new Address("user1@gitblit.com"));
mailer.send(request);
```

### Sending an HTML Email

```java
MailRequest request = mailer.newHtmlMailRequest("Html Test Message", "This is a <b>text/html</b> message");
request.getToAddresses().add(new Address("user1@gitblit.com"));
mailer.send(request);
```

### Sending a Text Template Email

Templates are loaded from the `templates` base directory on the classpath.  All specified template names are relative to this directory.

```java
String subject = "Hi ${username}";
String templateName = "test_text";

Map<String, Object> parameters = new HashMap<>();
parameters.put("username", "Example User");
parameters.put("systemUsername", "Fathom");

MailRequest request = mailer.newTextTemplateMailRequest(subject, templateName, parameters);
request.getToAddresses().add(new Address("user1@gitblit.com"));
mailer.send(request);
```

### Sending an HTML Template Email

Templates are loaded from the `templates` base directory on the classpath.  All specified template names are relative to this directory.

```java
String subject = "Hi ${username}";
String templateName = "test_html";

Map<String, Object> parameters = new HashMap<>();
parameters.put("username", "Example User");
parameters.put("systemUsername", "Fathom");

MailRequest request = mailer.newHtmlTemplateMailRequest(subject, templateName, parameters);
request.getToAddresses().add(new Address("user1@gitblit.com"));
mailer.send(request);
```
