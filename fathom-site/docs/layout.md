## Layout

Fathom applications use a directory structure popularized by Play; this directory structure is a combination of convention & configuration.

By default, your *application package* is Java's *default package*.  This may seem strange at first.  In **Fathom-Core** only the `conf` package is based on configuration.  All other packages (e.g. *dao* & *models* in the example below) are convention and have no real meaning.

**Note:**<br/>
Some `Module` instances may also depend on your *application package*.

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
