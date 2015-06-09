## About

**Fathom-REST-Security** integrates [Fathom-REST](rest.md) with [Fathom-Security](security.md) to provide your Routes and Controllers with flexible authentication and authorization.

This is the recommended security module for [Fathom-REST](rest.md).

## Installation

Add the **Fathom-REST-Security** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-rest-security</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

## Configuration

See [Fathom-Security](security/#configuration).

## Authenication

### Basic Authentication

[Basic Authentication] is suitable for simple webpages and RESTful APIs.

#### Usage

```java
public class Routes extends RoutesModule {

  @Inject
  SecurityManager securityManager;

  @Override
  protected void setup() {
    // Require BASIC authentication for all requests to /employees/
    ALL("/employees/.*", new BasicAuthenticationHandler(securityManager, getSettings()));

    // Prefer but do not require BASIC authentication for all requests to /contractors/
    boolean createSessions = true;
    boolean isPassive = true; // allow request to continue if basic-auth header is absent
    String realm = getSettings().getApplicationName();
    ALL("/contractors/.*", new BasicAuthenticationHandler(securityManager, createSessions, isPassive, realm));
  }
}
```

### Form Authentication

[Form Authentication] is suitable for complex webpages rendered with a browser.  Form authentication requires maintaining a [Session].

#### Usage

```java
public class Routes extends RoutesModule {

  @Inject
  SecurityManager securityManager;

  @Override
  protected void setup() {
    // Define a handler for the login process.
    // A template named 'login' will be rendered on unauthenticated GET requests.
    // The SecurityManager will authenticate unauthenticated POST requests and a
    // new Session will be created on successful authentication.
    FormAuthenticationHandler formAuthHandler = new FormAuthenticationHandler(securityManager);
    GET("/login", formAuthHandler);
    POST("/login", formAuthHandler);

    // Define the logout URL and handler.
    // Any request to this URL will invalidate the Session.
    ALL("/logout", new LogoutHandler());

    // Specify the URLs to guard with form authentication.
    // Unauthenticated requests will be redirected to "/login".
    FormAuthenticationGuard guard = new FormAuthenticationGuard("/login");
    GET("/employees/.*", guard);
    POST("/employees/.*", guard);
  }
}
```

## Authorization

### Form Submission Authorization or Cross-Site Request Forgery Protection

[Cross-Site Request Forgery], or CSRF, is a technique to coerce a user to submit a form which executes an unauthorized action on a server using the user's cookies or current session.  From the perspective of the server, the submitted form originated from the user and was an intended action.  From the perspective of the user, they did not authorize the action even though they did submit the forged form.

To protect the user from this kind of forgery we will insert a temporary *token* into the generated forms and require the same *token* to be in the submitted POST request.  This *token* is only valid for the duration of the user's [Session] and not otherwise accessible from malicious forms.

Guarding against this kind of attack is a two-step operation.  First we must add a guard which will validate that submitted forms contain the correct *token* and then we must ensure we are including the *token* in the generated forms to be submitted.

In this example we will guard all the `/employees/` URLs.

```java
CSRFHandler csrfHandler = new CSRFHandler();
ALL("/employees/.*)", csrfHandler).named("CSRF handler");
```

And we must also update our page generation to include a hidden form field named `_csrf_token`.  The *csrfToken* value is available to all template engines.

```html
<form method="post" action="/employees/rename/5">
  <input type="hidden" name="_csrf_token" value="${csrfToken}">
  <input placeholder="Employee name" name="employeeName">
  <input type="submit" value="Rename">
</form>
```

### Controller Method Account Argument Extractor

If you want easy access to the *account* associated with a request you may specify `@Auth Account account` as a method parameter and **Fathom-REST** will inject the *account* on execution.  If the request has no associated *account* then the *Guest account* is supplied to avoid NullPointerExceptions.

```java
@ControllerPath("/employees")
public class EmployeesController extends Controller {

  @GET("/{id: [0-9]+}")
  @Produces({Produces.JSON, Produces.XML})
  @Metered
  public void viewEmployee(int id, @Auth Account account) {
    // authorize the request
    account.checkPermission("employee:view:" + id);

    // The method parameter name "id" matches the url parameter name "id"
    Employee employee = employeeDao.get(id);
    if (employee == null) {
      getResponse().notFound().send("Failed to find employee {}!", id);
    } else {
      getResponse().ok().send(employee);
    }
  }
}
```

### Controller Method Authorization

Instead of manually injecting and checking the *account* for a specific permission, you may use an annotation to specify the *role* or *permission* to require.  If the request has no associated *account* then the *Guest account* is supplied to avoid NullPointerExceptions.

If the *account* is unauthorized an *AuthorizationException* will be thrown which will be intercepted and an Unauthorized (403) error code will be returned to the client.

| Annotation                         | Use-Case                                                       |
|------------------------------------|----------------------------------------------------------------|
| `@RequireGuest`                    | Action requires a Guest account                                |
| `@RequireAuthenticated`            | Action requires an Authenticated account (not a Guest account) |
| `@RequireAdministrator`            | Action requires administrator permissions                      |
| `@RequireRole("role")`             | Action requires the specified role                             |
| `@RequirePermission("permission")` | Action requires the specified permission                       |

In the following examples we are enforcing `employee:view` and `employee:delete` *permissions* on the controller methods.

```java
@ControllerPath("/employees")
public class EmployeesController extends Controller {

  @GET("/{id: [0-9]+}")
  @RequirePermission("employee:view")
  @Produces({Produces.JSON, Produces.XML})
  @Metered  
  public void getEmployee(int id, @Auth Account account) {
    // The method parameter name "id" matches the url parameter name "id"
    Employee employee = employeeDao.get(id);
    if (employee == null) {
      getResponse().notFound().send("Failed to find employee {}!", id);
    } else {
      getResponse().ok().send(employee);
    }
  }

  @DELETE("/employees/{id: [0-9]+}")
  @RequirePermission("employee:delete")
  public void deleteEmployee(int id) {
    boolean success = employeeDao.remove(id);
    if (success) {
      getResponse().ok();
    } else {
      getResponse().notFound();
    }
  }
}
```

[Basic Authentication]: https://en.wikipedia.org/wiki/Basic_access_authentication
[Form Authentication]: https://en.wikipedia.org/wiki/Form-based_authentication
[Cross-Site Request Forgery]: https://en.wikipedia.org/wiki/Cross-site_request_forgery
[Session]: http://www.pippo.ro/doc/session.html
