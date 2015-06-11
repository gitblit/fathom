## Layout

Fathom applications use a directory structure popularized by Play; this directory structure is a combination of convention & configuration.

!!! Note
    By default, your *application package* is Java's *default package*.  In **Fathom-Core** only the `conf` package is based on configuration.  All other packages (e.g. *dao* & *models* in the example below) are convention and have no real meaning.

```
YourApp
├── pom.xml
└── src
    └── main
        ├── java
        │   ├── App.java
        │   ├── conf
        │   │   ├── Components.java
        │   │   ├── Fathom.java
        │   │   └── Servlets.java
        │   ├── dao
        │   │   ├── EmployeeDao.java
        │   │   └── ItemDao.java
        │   └── models
        │       ├── Employee.java
        │       └── Item.java
        └── resources
            └── conf
                ├── default.conf
                ├── logback.xml
                ├── logback-dev.xml
                ├── master.conf
                └── slave.conf

```

### Configuration of Application Package

It's easy to change the package for your application BUT you must be aware of how that may affect each *module* that you use.  The effect of *application.package*, if any, is documented in the **Layout** section of each *module* documentation.

```hocon
application {
  package = "com.gitblit"
}
```

!!! Note
    The *application.package* setting affects Java classes but not configuration files.  These will still be loaded from the `conf` resource directory of your classpath.
