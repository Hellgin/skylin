# Skylin

Application framework. The Maven jar is the Java backend, package `com.distantmelody.framework.skylin`. PHP, CSS, and JavaScript are not inside that jar. They are `com.distantmelody:skylin-web`, a zip. The `skylin` jar depends on that zip, so a project that depends only on `skylin` downloads both.

Unpack `skylin-web` at the document root. That puts `index.php`, `rest.php`, and `restless.php` there, and the `skylin/` directory beside them.

- Java sources: `java/src/main/java`
- Web sources: `php/`
- CSS: `php/skylin/skylin.css` and `php/skylin/dependencies/pickaday/pikaday.css`

Files under `php/skylin/dependencies` are third-party libraries and keep their own licenses. The web zip leaves out `php/tests`.

You need to unpack `skylin-web` into your web root.

```
mvn test
```

Apache License 2.0. See [LICENSE](LICENSE).
