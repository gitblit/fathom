## About

**Fathom-Mailer** provides your application with a singleton instance of a mail service.

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

Inject the `Mailer` where you need the mail service.

```java
@Singleton
public class MyPostOffice {

  @Inject
  Mailer mailer;

  public void sendText() {
    // prepare a mail request
    MailRequest request = mailer.newTextMailRequest("Plain Text Message", "This is a plain text message");

    request.getToAddresses().add(new Address("user1@gitblit.com"));
    request.getToAddresses().add(new Address("user2@gitblit.com"));

    // send the mail asynchronously
    mailer.send(request);
  }

  public void sendHtml() {
    // prepare a mail request
    MailRequest request = mailer.newHtmlMailRequest("Html Test Message", "This is a <b>text/html</b> message");

    request.getToAddresses().add(new Address("user1@gitblit.com"));
    request.getToAddresses().add(new Address("user2@gitblit.com"));

    // send the mail asynchronously
    mailer.send(request);
  }

  public void sendTextTemplate() {
    String subject = "Hi ${username}";
    String templateName = "test_text";

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("username", "Example User");
    parameters.put("systemUsername", "Fathom");

    // prepare a mail request
    MailRequest request = mailer.newTextTemplateMailRequest(subject, templateName, parameters);

    request.getToAddresses().add(new Address("user1@gitblit.com"));
    request.getToAddresses().add(new Address("user2@gitblit.com"));

    // send the mail asynchronously
    mailer.send(request);
  }

  public void sendHtmlTemplate() {
    String subject = "Hi ${username}";
    String templateName = "test_html";

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("username", "Example User");
    parameters.put("systemUsername", "Fathom");
    MailRequest request = mailer.newHtmlTemplateMailRequest(subject, templateName, parameters);

    request.getToAddresses().add(new Address("user1@gitblit.com"));
    request.getToAddresses().add(new Address("user2@gitblit.com"));

    // send the mail asynchronously
    mailer.send(request);
  }

}
```
