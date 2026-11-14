# Skylin

Application framework. The Maven artifact is the Java backend, package `com.distantmelody.framework.skylin`. PHP and CSS are in this repository and are not inside the jar.

- Java sources: `src/main/java`
- PHP: `php/`
- CSS: `php/skylin/skylin.css` and `php/skylin/dependencies/pickaday/pikaday.css`

Files under `php/skylin/dependencies` are third-party libraries and keep their own licenses.

```xml
<dependency>
  <groupId>com.distantmelody</groupId>
  <artifactId>skylin</artifactId>
  <version>0.1.0</version>
</dependency>
```

`skylin` depends on `melody-http`, which depends on `melody-json`. Both stay on the compile classpath of a project that depends only on `skylin`, so that project can import `J` and `ServiceUtil` without declaring them.

```
mvn test
```

Apache License 2.0. See [LICENSE](LICENSE).

Publishing `0.1.0` to Maven Central uses `mvn -Prelease deploy` after `com.distantmelody:melody-json:0.1.0` and `com.distantmelody:melody-http:0.1.0` are already on Central, the `com.distantmelody` namespace is verified, a Central Portal token is in `settings.xml` under server id `central`, and GPG signing works. That deploy has not been run.
