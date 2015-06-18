## About

**Fathom-REST-Swagger** integrates [Fathom-REST](rest.md) with [Swagger] to provide you with an easy to use RESTful API specification generator and browser.

!!! Note
    The Swagger specification is quite detailed.  **Fathom-REST-Swagger** makes a best-effort to provide an excellent Swagger experience, but it does not support all aspects of the specification.

## Installation

Add the **Fathom-REST-Swagger** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-rest-swagger</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

## Layout

```
YourApp
└── src
    └── main
        ├── java
        │   └── controllers
        │       ├── EmployeeController.java
        │       └── ItemController.java
        └── resources
            └── swagger
                ├── info.md
                └── controllers
                    ├── EmployeeController
                    │   ├── getEmployee.md
                    │   └── deleteEmployee.md
                    └── ItemController
                        ├── getItem.md
                        └── deleteItem.md
```

## Configuration

**Fathom-REST-Swagger** is configured in your resource config file `conf/default.conf`.

```hocon
swagger {

  # The host (name or ip) serving the API.  This MUST be the host only and does
  # not include the scheme nor subpath.  It MAY include a port. If the host is
  # not specified, the host serving the documentation is to be used
  # (including the port).
  host = ""

  # The base path on which the API is served, which is relative to the host.
  # All specified routes will be relative to this path.
  basePath = "/api"

  # The transfer protocol of the API.
  schemes = [ "http", "https" ]

  # Swagger API Specification
  info {
    # Title of your API
    title = "Example API"

    # Markdown API description resource to load and insert into the generated specification
    description = "classpath:swagger/info.md"

    # API version to display in your generated specifications
    # The default is your {application.version}.
    version = "1.0.0"

    # API Contact Information
    contact {
      name = "API Support"
      url = "http://www.swagger.io/support"
      email = "support@swagger.io"
    }

    # API License Information
    license {
      name = "The Apache Software License, Version 2.0"
      url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
    }
  }

  # External documentation link
  externalDocs {
    url = "http://swagger.io"
    description = "More about Swagger"
  }

  # Swagger UI and Specification Serving
  ui {
    # Path for serving Swagger UI and Swagger specifications
    # This path is relative to your application, not swagger.basePath
    #  - Swagger UI served on /{swagger.ui.path}
    #  - JSON specification served on /{swagger.ui.path}/swagger.json
    #  - YAML specification served on /{swagger.ui.path}/swagger.yaml
    path = "/api"

    # Text to display in the banner of the Swagger UI
    bannerText = "swagger"

    # Display the API key text field in Swagger UI
    showApiKey = false
  }

}
```

## Usage

**Fathom-REST-Swagger** registers a [service](services.md) which will automatically generate & serve Swagger 2.0 specifications in JSON and YAML.  The specifications are generated from your registered Fathom [controller routes](/rest/#controllers) at runtime.  Routes that are directly implemented in the `conf/Routes.java` class are not considered for inclusion because they lack too much detail.

!!! Note
    Only **RESTful Fathom Controller Routes** are included in the generated Swagger specifications.

### RESTful Fathom Controller Routes

A [RESTful] web API is an http/https service that processes resource requests (/employee/5) using [http verbs] (GET, POST, PUT, DELETE, etc).

RESTful web APIs typically...

- Produce `application/json`, `application/xml`, `application/x-yaml`
and
- Consume `application/json`, `application/xml`, `application/x-yaml`, `application/x-www-form-urlencoded`, and/or `multipart/form-data`

In order for a Fathom controller route to be registered in your Swagger specification...

1. the route must declare that it `@Produces` one or more aforementioned *content-types*
2. the route must specify one of the following http methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`
3. the route and it's declaring controller are NOT annotated with `@Undocumented`
4. the route's uri pattern must begin with the configured `swagger.basePath`

```java
// Controller API Method, REGISTERED in Swagger specification
@GET("/employee/{id}")
@Produces({Produces.JSON, Produces.XML})
public Employee getEmployee(int id) {
}

// Controller View Method, NOT REGISTERED in Swagger specification
@GET("/employee/{id}.html")
@Produces({Produces.HTML})
public void getEmployee(int id) {
}
```

## Improving the Generated Specification

**Fathom-REST-Swagger** is able to generate a fairly complete Swagger specification from your modestly-documented or well-documented [Fathom-REST](rest.md) controllers.

However, there is always room for improvement.  Your generated specification, while functional, can not fully showcase your API without some hints from you.

### @Tag

You may use the `@Tag` annotation to briefly describe a controller and it's set of methods/operations.  In Swagger, operations are grouped together by their *tag* and those operations share a common base path (*e.g. /api/employee*).

```java
@Path("/api/employee")
@Produces({Produces.JSON, Produces.XML})
@Tag(name="employees", description="Employees API")
public class EmployeeController extends Controller {
}
```

### @Named

You may name your controller routes.  This information is used in the *Summary* field of the Swagger specification and may also be used for normal runtime logging of route dispatching.

```java
@GET("/{id}")
@Named("Get employee by id")
public Employee getEmployee(int id) {
  Employee employee = employeeDao.get(id);
  return employee;
}
```

!!! Note
    The `@Named` annotation is part of [Fathom-REST](rest.md) and is re-used for Swagger.

### @Notes

The `@Notes` annotation adds a brief description to an operation in addition to the `@Named` (*Summary*) information.

You can use `@Notes` to load a classpath resource notes file.  [GFM] syntax may be used.
These two examples are equivalent for a given controller method.

```java
@GET("/{id}")
@Notes
public Employee getEmployee(int id) {
  Employee employee = employeeDao.get(id);
  return employee;
}

@GET("/{id}")
@Notes("classpath:swagger/com/package/EmployeeController/getEmployee.md")
public Employee getEmployee(int id) {
  Employee employee = employeeDao.get(id);
  return employee;
}
```

Or you may directly specify your note text:

```java
@Notes("This method requires a valid employee id")
public Employee getEmployee(int id) {
  Employee employee = employeeDao.get(id);
  return employee;
}
```

### @Desc

You may use the `@Desc` annotation to briefly describe a controller method parameter.

```java
@GET("/{id}")
public Employee getEmployee(@Desc("employee id") int id) {
  Employee employee = employeeDao.get(id);
  return employee;
}
```

### @Form

Specify the `@Form` annotation to indicate that a method argument is sourced from a form.

```java
@POST("/{id}/uploadAvatar")
public void uploadAvatar(
    @Desc("employee id") int id
    @Desc("nickname") @Form String nickname,
    @Desc("avatar to upload") @Form FileItem avatar) {

}
```

### @Return

You may use the `@Return` annotation to briefly describe method-specific responses.

```java
@GET("/{id}")
@Return(status = 200, description = "Employee retrieved", onResult = Employee.class)
@Return(status = 404, description = "Employee not found", onResult = Void.class)
public Employee getEmployee(@Desc("employee id") int id) {
  Employee employee = employeeDao.get(id);
  return employee;
}
```

!!! Note
    The `@Return` annotation is part of [Fathom-REST](rest.md) and is more thoroughly documented in that module.

## CORS or Cross Origin Resource Sharing

CORS is a technique to prevent websites from doing bad things with your personal data. Most browsers + javascript toolkits not only support CORS but enforce it, which has implications for your API server which supports Swagger.

You can read about CORS here: http://www.html5rocks.com/en/tutorials/cors/.

There are two cases where no action is needed for CORS support:

1. swagger-ui is hosted on the same server as the application itself (same host and port).
2. The application is located behind a proxy that enables the requires CORS headers. This may already be covered within your organization.

Otherwise, CORS support needs to be enabled for:

1. Your generated Swagger specifications, swagger.json and swagger.yaml
2. Your API endpoints, if you want the *Try it now* button to work

### Configuring CORS Support

Add a `HeaderFilter` in your `conf/Routes.java` class before you register your API routes.

```java
CORSFilter corsFilter = new CORSFilter();
corsFilter.setAllowOrigin("*");
corsFilter.setAllowMethods("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD");
corsFilter.setAllowHeaders("Content-Type", "api_key", "Authorization", "Csrf-Token");

ALL("/api/?.*", corsFilter).named("CORS Filter");
```

### Testing CORS Support

Once you have setup your CORS filter, you can test that the appropriate headers are being set with curl or your favorite browser extension.

```
$ curl -v -X OPTIONS --header "Access-Control-Request-Method: GET" "http://localhost:8080/api/swagger.json"
* Hostname was NOT found in DNS cache
*   Trying 127.0.0.1...
* Connected to localhost (127.0.0.1) port 8080 (#0)
> OPTIONS /api/swagger.json HTTP/1.1
> User-Agent: curl/7.35.0
> Host: localhost:8080
> Accept: */*
> Access-Control-Request-Method: GET
>
< HTTP/1.1 200 OK
< Connection: keep-alive
< Access-Control-Allow-Origin: *
< Access-Control-Allow-Headers: Content-Type,api_key,Authorization,Csrf-Token
< Content-Type: text/html;charset=UTF-8
< Content-Length: 0
< Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE,HEAD
< Date: Thu, 18 Jun 2015 22:06:47 GMT
<
* Connection #0 to host localhost left intact
```

[Swagger]: http://swagger.io
[RESTful]: https://en.wikipedia.org/wiki/Representational_state_transfer
[http verbs]: https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol#Request_methods
[GFM]: https://help.github.com/articles/github-flavored-markdown/
