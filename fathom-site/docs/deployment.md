## Deployment

Fathom does not supply a deployment solution.  It does, however, recommend two available solutions.

- [Stork], for more traditional distribution and foolproof Linux/Windows/OSX service integration
- [Capsule], for more modern distribution

## Stork

[Stork] will generate a universal `.tar.gz` bundle of your application with shell scripts & batch files that will correctly launch your application and optionally install/remove an operating system service for your application.

### Layout
```
YourApp
├── pom.xml
└── src
    └── main
        └── launchers
            └── fathom.yml
```

### Configuration

**pom.xml**
```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.fizzed</groupId>
      <artifactId>fizzed-stork-maven-plugin</artifactId>
      <version>1.2.2</version>
      <executions>
        <execution>
          <id>generate-stork-launchers</id>
          <goals>
            <goal>generate</goal>
          </goals>
        </execution>
        <execution>
          <id>generate-stork-assembly</id>
          <goals>
            <goal>assembly</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

**src/main/launchers/fathom.yml**
```yaml
#
# Stork Packaging
# https://github.com/fizzed/java-stork
#
name: "fathom-fit"
domain: "com.gitblit.fathom"
display_name: "fathom-fit"
short_description: "Fathom Integration Test"
type: DAEMON
main_class: "fathom.Boot"
platforms: [ WINDOWS, LINUX, MAC_OSX ]
min_java_version: "1.8"
min_java_memory: 1024
symlink_java: true
```

## Capsule

[Capsule] describes itself as *dead-simple packaging and deployment for JVM applications*.

Capsule generates either a *fat-jar*, similar to [Maven's Shade plugin](http://maven.apache.org/plugins/maven-shade-plugin/) or [Gradle's Shadow plugin](https://github.com/johnrengelman/shadow), or a *thin-jar* that automatically downloads dependencies on first execution.

In a typical Fathom application it's very easy to generate a 25MB+ distribution.  This makes The *thin-jar* build particularly appealing since it is likely to be under a few MB.

### Configuration

**pom.xml**
```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.github.chrischristo</groupId>
      <artifactId>capsule-maven-plugin</artifactId>
      <version>0.10.3</version>
      <executions>
        <execution>
          <goals>
            <goal>build</goal>
          </goals>
          <configuration>
            <appClass>fathom.Boot</appClass>
            <chmod>true</chmod>
            <trampoline>true</trampoline>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

[Stork]: https://github.com/fizzed/stork
[Capsule]: https://github.com/puniverse/capsule
