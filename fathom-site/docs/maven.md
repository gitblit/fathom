## Maven Tricks

[Maven] is the build system used to compile, assemble, and distribute Fathom.

You are not required to use it, but clearly all the examples in this documentation are written from a Maven perspective.

### Parameters Flag

Fathom depends on Java 8.  For most things, this dependency is quite clear.  In other ways it can be subtle as some Fathom modules depend on the `-parameters` flag of the Java 8 *javac* compiler.  This flag embeds the names of method parameters in the generated *.class* files.

By default Java 8 does not compile with this flag set so you must specify compiler arguments for Maven.

```xml
<build>
  <plugins>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
            <source>1.8</source>
            <target>1.8</target>
            <compilerArguments>
                <parameters/>
            </compilerArguments>
        </configuration>
    </plugin>
  </plugins>
</build>
```

### Maven Version Stamping

It's convenient to automatically stamp your Fathom application with the version specified in your Maven `pom.xml`.

To accomplish this add a resource filter...

```xml
<build>
  <resources>
      <resource>
          <directory>src/main/java</directory>
          <filtering>true</filtering>
          <includes>
              <include>**/*</include>
          </includes>
          <excludes>
              <exclude>**/*.java</exclude>
          </excludes>
      </resource>
      <resource>
          <directory>src/main/resources</directory>
          <filtering>true</filtering>
          <includes>
              <include>**/*</include>
          </includes>
      </resource>
  </resources>
</build>
```

... and then use the typical Maven-style property references in your `conf/default.conf` file.

```hocon
application {
  name = ${project.name}
  version = ${project.version}
}
```

[Maven]: http://maven.apache.org
