## About

**Fathom-REST-Swagger** integrates [Fathom-REST](rest.md) with [Swagger] to provide you with an easy to use RESTful API specification generator and browser.

!!! Warning
    The [Swagger] specification is quite detailed.  **Fathom-REST-Swagger** makes a best-effort to provide an excellent [Swagger] experience, but it does not support all aspects of the Swagger specification.

## Installation

Add the **Fathom-REST-Swagger** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-rest-swagger</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

## Configuration

**Fathom-REST-Swagger** is configured in your resource config file `conf/default.conf`.

```hocon
swagger {

  # Swagger API Specification
  api {
    # Title of your API
    title = "Example API"

    # Markdown API description resource to load and insert into the generated specification
    description = "classpath:swagger/api.md"

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

  # Swagger UI and Specification Serving
  ui {
    # Path for serving Swagger UI and Swagger specifications
    #  - Swagger UI served on /{swagger.ui.url}
    #  - JSON specification served on /{swagger.ui.url}/swagger.json
    #  - YAML specification served on /{swagger.ui.url}/swagger.yaml
    url = "/api"

    # Text to display in the banner of the Swagger UI
    bannerText = "swagger"

    # Display the API key text field in Swagger UI
    showApiKey = false
  }

}
```

## Usage

**Fathom-REST-Swagger** registers a [service](services.md) which will automatically generate & serve Swagger 2.0 specifications in JSON and YAML.  The specifications are generated from the registered Fathom [controller routes](/rest/#controllers).  Non-Controller routes that are directly implemented in the `conf/Routes.java` class are not candidates for inclusion because they lack too much detail.

!!! Note
    **Only RESTful Fathom controller routes may be used to generate Swagger specifications.**

### RESTful Fathom Controller Routes

[Swagger] is a specification for describing a RESTful API.  Generally, a RESTful API is a collection of http/https urls paired with http methods which generate `application/json`, `application/xml`, and/or `application/x-yaml`.

In order for your Fathom controller route to be registered in your Swagger specification it...

1. must declare that it `@Produces` one or more RESTful *content-types*
2. must specify one of the following http methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`
3. must NOT be annotated with `@Undocumented`

```java
// Controller API Method, registered in Swagger specification
@GET("/employee/{id}")
@Produces({Produces.JSON, Produces.XML})
public void getEmployee(int id) {
  Employee employee = employeeDao.get(id);
  if (employee != null) {
    getResponse().ok().send(employee);
  } else {
    getResponse().notFound();
  }
}

// Controller View Method, NOT registered in Swagger specification
@GET("/employees/{id}")
@Produces({Produces.HTML})
public void getEmployee(int id) {
  Employee employee = employeeDao.get(id);
  if (employee != null) {
    getResponse().bind("employee", employee).render("employee");
  } else {
    getResponse().notFound();
  }
}
```

### Improving the Generated Specification

Your generated Swagger specification, while functional, can not fully describe your API without some hints from you.

#### @Tag

You may use the `@Tag` annotation to briefly describe a controller.

```java
@Path("/api/employee")
@Produces({Produces.JSON, Produces.XML})
@Tag(name="employees", description="Employees API")
public class EmployeeController extends Controller {
}
```

#### @Summary

You may use the `@Summary` annotation to briefly describe a controller method.

```java
@GET("/{id}")
@Summary("Get an employee")
public void getEmployee(int id) {
  Employee employee = employeeDao.get(id);
  if (employee != null) {
    getResponse().ok().send(employee);
  } else {
    getResponse().notFound();
  }
}
```

#### @Notes

The `@Notes` annotation is a flag to load a classpath Markdown resource of notes for the controller method.

By default, the resource file `classpath:swagger/com/package/controller/method.md` will be loaded and inserted into your specification.

```java
@GET("/{id}")
@Summary("Get an employee")
@Notes
public void getEmployee(int id) {
  Employee employee = employeeDao.get(id);
  if (employee != null) {
    getResponse().ok().send(employee);
  } else {
    getResponse().notFound();
  }
}
```

#### @Desc

You may use the `@Desc` annotation to briefly describe a controller method parameter.

```java
@GET("/{id}")
@Summary("Get an employee")
@Notes
public void getEmployee(@Desc("employee id") int id) {
  Employee employee = employeeDao.get(id);
  if (employee != null) {
    getResponse().ok().send(employee);
  } else {
    getResponse().notFound();
  }
}
```

[Swagger]: http://swagger.io