# Skylin

Application framework. The Maven jar is the Java backend, package `com.distantmelody.framework.skylin`. PHP, CSS, and JavaScript are not inside that jar. They are `com.distantmelody:skylin-web`, a zip. The `skylin` jar depends on that zip, so a project that depends only on `skylin` downloads both.

Unpack `skylin-web` at the document root. That puts `index.php`, `rest.php`, and `restless.php` there, and the `skylin/` directory beside them.

- Java sources: `java/src/main/java`
- Web sources: `php/`
- CSS: `php/skylin/skylin.css` and `php/skylin/dependencies/pickaday/pikaday.css`

Files under `php/skylin/dependencies` are third-party libraries and keep their own licenses. The web zip leaves out `php/tests`.

## Maven Central

```xml
<dependency>
  <groupId>com.distantmelody</groupId>
  <artifactId>skylin</artifactId>
  <version>1.0.2</version>
</dependency>
```

`skylin` depends on `skylin-web`, and on `melody-http`, which depends on `melody-json`. `melody-http` and `melody-json` stay on the compile classpath of a project that depends only on `skylin`, so that project can import `J` and `ServiceUtil` without declaring them. `skylin-web` is pulled in transitively as `com.distantmelody:skylin-web:1.0.2:zip`, runtime scope. Maven downloads that zip and does not unpack it.

Unpack it during `generate-resources`, then use the output directory as the web server document root. That is where `index.php`, `rest.php`, `restless.php`, and `skylin/` are served from. The Java backend does not take a path to these files. `SKYLIN_ROOT` in `FrameworkParams.cnf` (`key:value` lines, read with `Util.getFrameworkParam`) is a path under that document root. `SkylinBlob` prefixes blob URLs with it. Leave it empty when the unpacked zip is the document root.

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-dependency-plugin</artifactId>
  <version>3.11.0</version>
  <executions>
    <execution>
      <id>unpack-skylin-web</id>
      <phase>generate-resources</phase>
      <goals>
        <goal>unpack</goal>
      </goals>
      <configuration>
        <artifactItems>
          <artifactItem>
            <groupId>com.distantmelody</groupId>
            <artifactId>skylin-web</artifactId>
            <version>1.0.2</version>
            <type>zip</type>
            <outputDirectory>${project.build.directory}/skylin-web</outputDirectory>
          </artifactItem>
        </artifactItems>
      </configuration>
    </execution>
  </executions>
</plugin>
```

```
mvn test
```

Apache License 2.0. See [LICENSE](LICENSE).
