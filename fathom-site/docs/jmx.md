## About

**Fathom-JMX** provides a simple way to configure a Java Management Extensions ([JMX]) server for your application.

## Installation

Add the **Fathom-JMX** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-jmx</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

## Configuration

By default, this module will startup a JMX server on port 7091.

```hocon
jmx {
  # The port for serving the JMX registry and handling data connections
  port = 7091
}
```

## Usage

Once you have configured your preferred JMX port and have started your application, you should be able to establish a JMX connection to `127.0.0.1:$JMXPORT` using Mission Control, VisualVM, or JConsole.

You can confirm that your JMX server is running with netstat:

    netstat -t -a

This should indicate all TCP sockets that are listening on your machine.

!!! Note
    The integrated JMX server binds to the loopback address `127.0.0.1` and does not use authentication.  Your JMX server will be safe from outside intrusion, but is otherwise unprotected from users who already have access to your server.

### Remote monitoring on Windows

Since you have Windows you may also have RDP, TeamViewer, or VNC access.  If so, the simplest approach is to install the JDK locally on your server and use Mission Control, VisualVM, or JConsole through your RDP, TeamViewer, or VNC connection.

### Remote monitoring through SSH

If you have a working SSH connection to your remote server, you may use SSH port-forwarding to remotely monitor your Fathom application.

From your local machine, this command establishes an SSH connection with a port-forwarded tunnel for the default JMX port of the remote machine.

    ssh -L 7091:127.0.0.1:7091 username@remoteserver.com

Then, from your preferred JMX client application on your local machine, you simply open a JMX connection on `127.0.0.1:7091`.  Your JMX connection will be available as long as the SSH tunnel is kept alive.

### Remote monitoring through Putty

This is the same exact concept as described in the previous section but instead of specifying command-line arguments, you must configure your SSH session for *Tunnels*.

[JMX]: https://en.wikipedia.org/wiki/Java_Management_Extensions
