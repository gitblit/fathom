# Modules

There are two kinds of *modules* in Fathom, `fathom.Module` and `fathom.ServletsModule`.

All Fathom *modules* are indentified by a [Java service loader].

## fathom.Module

Use this *module* type to provide injectable *Services* & components to your Fathom application.

### Configuration

Create a service definition file for your *module*...

**META-INF/services/fathom.Module**
```
com.package.MyModule
```

... and then define it.

```java
public class MyModule extends Module {

  @Override
  protected void setup() {

    // register MyService
    bind(MyService.class);

  }

}
```

## fathom.ServletsModule

Use this *module* type to provide injectable *Services* & components, Filters, and Servlets.

### Configuration

Create a service definition file for your *module*...

**META-INF/services/fathom.ServletsModule**
```
com.package.MyServletsModule
```

... and then define it.

```java
public class MyServletsModule extends ServletsModule {

  @Override
  protected void setup() {

    // serve all requests with MyServlet
    serve("/*").with(MyServlet.class);

    // add a debug filter for dev mode
    if (getSettings().isDev()) {
      filter("/*").through(MyFilter.class);
    }

    // register MyService
    bind(MyService.class);

  }

}
```

## Configuration

### Requiring Settings

Your *module* may need one or more settings to function and you may specify them as annotated requirements.

Each required setting must be present in the runtime profile [configuration](configuration.md) and must have a non-empty value otherwise the *module* will not be loaded.

```java
@RequireSetting("myService.url")
@RequireSetting("myService.username")
@RequireSetting("myService.password")
public class MyModule extends Module {

  @Override
  protected void init() {

    // register MyService
    bind(MyService.class);

  }

}
```

### Requiring Modes

You might only want to load your *module* in a particular runtime *mode*. This is easily accomplished by using one or more of the mode-specific annotations: `@DEV`, `@TEST`, and `@PROD`.

```java
@DEV @TEST
public class DebugModule extends Module {

  @Override
  protected void init() {

    // register DebugService
    bind(DebugService.class);

  }

}
```

## Usage

Add your *module* to the classpath of your Fathom application.

[Java service loader]: http://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html
