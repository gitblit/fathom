## Logging

[Logback] is the logging framework used by Fathom and [SLF4J] is the interface.

You can configure Logback through the `conf/logback.xml` file.

Details on configuring Logback can be found [here](http://logback.qos.ch/documentation.html).

### Mode-Specific Configuration

If a mode-specific Logback config file is discovered on the classpath, this file will be used instead of `logback.xml`.

Mode-specific config files are specified by appending *-mode* to the basename of the file.  For example, `conf/logback-dev.xml` is the Logback config for the DEV mode.

### Manual Specification

If you need to specify a Logback configuration file that is not built-into your application you may do this with a launch argument:

```
java -Dlogback.configurationFile=mylogback.xml -jar myapp.jar
```

[Logback]: http://logback.qos.ch
[SLF4J]: http://www.slf4j.org
