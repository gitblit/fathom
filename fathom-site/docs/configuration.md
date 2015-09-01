## Configuration

Your Fathom application is configured by a [HOCON] resource file named `conf/default.conf`.

!!! Note
    Specification of an *application.package* does not affect configuration file loading.  Configuration files will always be loaded from the `conf` resource directory of the classpath.

### Profiles

Your application may have several configuration profiles.  Each profile must have a corresponding configuration file.  The default configuration profile specifies the `conf/default.conf` resource configuration file.

You may choose your profile by specifying the `--profile` launch argument.

```
java -jar myapp.jar --profile myProfile
```

This example will load the `conf/myProfile.conf` resource configuration file.

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

In this example, the *port 1564* will be used when the application is run in the DEV mode, *port 2465* when the application is run in the TEST mode, and *port 465* for all other modes.

### Overriding Settings

Fathom will first load your profile configuration from the `conf` resource directory of your classpath.

If there is a *same-named* configuration file in the `java.home` directory (usually the directory from which your application was launched), this file will be *parsed & **merged*** with the classpath-sourced configuration.

This allows you to deploy a complete configuration profile built-into your application and then discretely override individual settings for that profile on an as-needed basis.

## Settings

```hocon
# Application metadata and settings
application {
  name = "My Application"
  version = "1.0.0"
  # used to generate advertised application urls for rendered pages, sent emails, etc
  hostname = ${undertow.host}
}

# Undertow server settings
undertow {

  # Setting a port to 0 disables that transport
  httpPort = 8080
  httpsPort = 0
  ajpPort = 0

  # Define the network interface for serving
  # e.g. 0.0.0.0 will serve on all available network interfaces
  host = "0.0.0.0"

  # The context path of your application.
  # This is useful if you are running your application behind a reverse proxy
  # and you have to create proxy/rewrite rules.
  contextPath = "/"

  # Optionally setup https/SSL
  keystoreFile = ""
  keystorePassword = ""
  truststoreFile = ""
  truststorePassword = ""

  # Threads
  #
  # io threads accept incoming socket connections
  # worker threads handle your logic post-socket-accept

  # Specify the number of io threads for handling socket requests.
  # A value of 0 will use Undertow's default which is:
  #  - 2 io threads for a single-core machine
  #  - 1 io thread/core for a multi-core machine
  ioThreads = 0

  # Specify the number of worker threads for handling your logic.
  # A value of 0 will use Undertow's default which is:
  #  - 8*ioThreads
  workerThreads = 0

  # Buffers
  #
  # Undertow adaptively sizes it's buffers based on the heap allocated to the JVM.
  #
  # If the heap < 64M:
  #  - bufferSize = 512
  #  - buffersPerRegion = 10
  #    this allocates 10*512 bytes
  #
  # If the heap < 128M:
  #  - bufferSize = 1K
  #  - buffersPerRegion = 10
  #    this allocates 10*1k
  #
  # Else
  #  - bufferSize = 16k
  #  - buffersPerRegion = 20
  #    this allocates 20*16k

  # Set the buffer size.
  # A value of 0 uses Undertow's adaptive default.
  #  e.g. 16k, 32k
  bufferSize = 16k

  # Set the number of buffers.
  # A value of 0 uses Undertow's adaptive default.
  buffersPerRegion = 20

}
```

[HOCON]: https://github.com/typesafehub/config/blob/master/README.md
