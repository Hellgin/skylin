# Skylin

Application framework. The Maven jar is the Java backend, package `com.distantmelody.framework.skylin`. PHP, CSS, and JavaScript are not inside that jar. They are published as `skylin-1.0.1-web.zip` (classifier `web`). Unpacking that zip at the document root puts `index.php`, `rest.php`, and `restless.php` there, and the `skylin/` directory beside them.

- Java sources: `src/main/java`
- Web sources: `php/`
- CSS: `php/skylin/skylin.css` and `php/skylin/dependencies/pickaday/pikaday.css`

Files under `php/skylin/dependencies` are third-party libraries and keep their own licenses. The web zip leaves out `php/tests`.

```xml
<dependency>
  <groupId>com.distantmelody</groupId>
  <artifactId>skylin</artifactId>
  <version>1.0.1</version>
</dependency>
```

```xml
<dependency>
  <groupId>com.distantmelody</groupId>
  <artifactId>skylin</artifactId>
  <version>1.0.1</version>
  <type>zip</type>
  <classifier>web</classifier>
</dependency>
```

`skylin` depends on `melody-http`, which depends on `melody-json`. Both stay on the compile classpath of a project that depends only on `skylin`, so that project can import `J` and `ServiceUtil` without declaring them.

```
mvn test
```

Apache License 2.0. See [LICENSE](LICENSE).
