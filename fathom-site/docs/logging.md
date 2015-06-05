## Logging

[Logback] is the logging framework used by Fathom and [SLF4J] is the facade.

You can configure Logback through the `conf/logback.xml` file.

Details on configuring Logback can be found [here](http://logback.qos.ch/documentation.html).

### Mode-Specific Configuration

You can specify a mode-specific Logback configuration in your profile configuration file.

```hocon
# Default Logback configuration
logback.configurationFile = "classpath:conf/logback-dev.xml"

# Production Logback configuration
prod {
  logback.configurationFile = "classpath:conf/logback.xml"
}
```

### Manual Specification

If you need to specify a Logback configuration file that is not built-into your application you may do this with a launch argument:

```
java -Dlogback.configurationFile=mylogback.xml -jar myapp.jar
```

[Logback]: http://logback.qos.ch
[SLF4J]: http://www.slf4j.org
