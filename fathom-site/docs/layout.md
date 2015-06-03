## Layout

Fathom applications use a directory structure popularized by Play; this directory structure is a combination of convention & configuration.

```
YourApp
├── pom.xml
└── src
    └── main
        └── java
            ├── YourApp.java
            ├── conf
            │   ├── Components.java
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
