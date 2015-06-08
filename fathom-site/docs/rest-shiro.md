## About

**Fathom-REST-Shiro** integrates [Fathom-REST](rest.md) with [Apache Shiro] to provide your Routes and Controllers with flexible authentication and authorization.

**Note:**<br/>
The [Apache Shiro] integration is NOT designed to be used with [Fathom-Security](security.md) nor [Fathom-REST-Security](rest-security.md).

## Installation

Add the **Fathom-REST-Shiro** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-rest-shiro</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

## Layout

```
YourApp
└── src
    └── main
        └── java
            └── conf
                └── shiro.ini
```

## Configuration

**Fathom-REST-Shiro** supports [INI file configuration](http://shiro.apache.org/configuration.html) with `conf/shiro.ini`.

## Usage

TODO.

[Apache Shiro]: https://shiro.apache.org/