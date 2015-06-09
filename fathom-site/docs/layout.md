## Layout

Fathom applications use a directory structure popularized by Play; this directory structure is a combination of convention & configuration.

By default, your *application package* is Java's *default package*.  This may seem strange at first.  In **Fathom-Core** only the `conf` package is based on configuration.  All other packages (e.g. *dao* & *models* in the example below) are convention and have no real meaning.

**Note:**<br/>
Some `Module` instances also depend on your *application package*.

```
YourApp
├── pom.xml
└── src
    └── main
        └── java
            ├── Launcher.java
            ├── conf
            │   ├── Components.java
            │   ├── Fathom.java
            │   ├── Servlets.java
            │   ├── default.conf
            │   ├── logback.xml
            │   ├── logback-dev.xml
            │   ├── master.conf
            │   └── slave.conf
            ├── dao
            │   ├── EmployeeDao.java
            │   └── ItemDao.java
            └── models
                ├── Employee.java
                └── Item.java
```

### Configuration of Application Package

It's easy to change the package for your application BUT you must be aware of how that may affect each *module*.  The affect of *application.package*, if any, is documented in the **Layout** section of each *module* documentation.

```hocon
application {
  package = "com.gitblit"
}
```

**Note:**<br/>
The *application.package* setting affects Java classes but not configuration files.  These will still be loaded from the `conf` directory of your classpath.
