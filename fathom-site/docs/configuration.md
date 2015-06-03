## Configuration

Your Fathom application is configured by a [HOCON] file named `conf/default.conf`.

Many modules require settings and these will all be stored in your configuration file.

### Profiles

Your application may have several configuration profiles.  Each profile must have a corresponding configuration file.  The `default` configuration profile specifies the `conf/default.conf` configuration file.

You may choose your profile by specifying the `--profile` launch argument.

```
java -jar myapp.jar --profile myProfile
```

This example will load the `conf/myProfile.conf` configuration file.

### Modes

A mode is used to change the behavior of your application and the settings loaded from the profile configuration file.

There are three available modes:

| Name | Description                       |
|------|-----------------------------------|
| PROD | Mode for production deployment    |
| TEST | Mode for unit/integration tests   |
| DEV  | Mode for development or debugging |

The runtime mode can be specified at launch time:

```
java -jar myapp.jar --mode DEV
```

### Modes & Settings

The profile configuration file supports mode-specific settings.

For example, consider the following configuration:

```hocon
mail {
  server = smtp.gmail.com
  port = 465
}

dev.mail {
  port = 1465
}
test.mail.port = 2465
```

In this example, the *port* 1564 will be used when the application is run in the DEV mode, *port* 2465 when the application is run in the TEST mode, and *port* 465 for all other modes.

### Overriding Settings

Fathom will first load your profile configuration from the `conf` directory of your classpath.

If there is a *same-named* configuration file in the `java.home` directory (usually the directory from which your application was launched), this file will be *parsed & **merged*** with the classpath-sourced configuration.

This allows you to deploy a complete configuration profile built-into your application and then discretely override individual settings for that profile on an as-needed basis.

## Settings

[HOCON]: https://github.com/typesafehub/config/blob/master/README.md
