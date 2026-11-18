# Running Skylin

A running Skylin application is two processes on one machine: a web server with PHP, and the Skylin JVM. They talk over
a Unix socket at a fixed path (see [architecture.md](architecture.md#bridge-transport-and-protocol)).

## Requirements

| What | Notes |
|---|---|
| Java 21 | The jar is built with `--release 21` |
| PHP 8 | With the `sockets` and `mbstring` extensions, and `tokenizer` (usually built in) |
| A web server that runs PHP and can proxy a WebSocket | The examples use Apache 2.4 with `mod_php`, `mod_proxy` and `mod_proxy_wstunnel`. Use a server that handles several requests at once; PHP's built-in development server handles one at a time |
| A JDBC driver | Not bundled. Oracle, MySQL and PostgreSQL URLs are built in |
| OS | Windows 10 or later, Linux, macOS. Windows before 10 1803 uses the TCP fallback |

## Getting the artifacts

```xml
<dependency>
  <groupId>com.distantmelody</groupId>
  <artifactId>skylin</artifactId>
  <version>1.0.4</version>
</dependency>
```

The `skylin` jar depends on `com.distantmelody:skylin-web` (a zip, runtime scope), so the zip arrives with it. Unpack the
zip into the document root, for example with the dependency plugin:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-dependency-plugin</artifactId>
  <executions>
    <execution>
      <id>unpack-skylin-web</id>
      <phase>generate-resources</phase>
      <goals><goal>unpack-dependencies</goal></goals>
      <configuration>
        <includeArtifactIds>skylin-web</includeArtifactIds>
        <outputDirectory>${project.build.directory}/skylin-web</outputDirectory>
      </configuration>
    </execution>
  </executions>
</plugin>
```

Copy `target/skylin-web/*` (`index.php`, `rest.php`, `restless.php`, `skylin/`) to the document root, and add your
`applications/` folder beside them.

## Layout

```
document root (web server)          JVM working directory
  index.php                           FrameworkParams.cnf
  rest.php                            CustomParams.cnf
  restless.php                        datasource.json
  skylin/                             bin/        your classes under APPLICATION_PACKAGES
  applications/                       lib/        skylin jar + dependencies, JDBC driver, your other jars
                                      logs/
```

Minimal `FrameworkParams.cnf`:

```
DEFAULT_APPLICATION:home/home
APPLICATION_PACKAGES:com.example.app.web
SECURITY_PROVIDERS:com.example.app.security.AppSecurityProvider
LOG_PATH:logs
```

`CustomParams.cnf` must exist and may be empty. Every key is in [configuration.md](configuration.md).

## Starting the JVM

From the working directory:

```
java -Djava.net.preferIPv4Stack=true -cp "lib/*" com.distantmelody.framework.skylin.server.Server
```

* The classpath needs the `skylin` jar and its dependencies (melody-http and the melody-json it brings,
  javax.servlet-api, Java-WebSocket, slf4j-api, junixsocket-common and junixsocket-native-common), the JDBC driver, and
  the classes named
  in `SECURITY_PROVIDERS`, `CONFIG_CLASS` and `STARTUP_THREAD`. `mvn dependency:copy-dependencies` collects them.
* slf4j-api comes without a binding. Add one (for example slf4j-simple) to see the framework's log lines.
* junixsocket unpacks its native library to a temporary directory at start. `-Dorg.newsclub.net.unix.library.tmpdir=<dir>`
  chooses the directory.
* At start the JVM waits until port 1001 is free (and port 1400 when it uses TCP), reads the configuration, runs
  `CONFIG_CLASS`, opens the bridge and the WebSocket server, starts `STARTUP_THREAD`, and prints
  `Server is ready. Startup time: ...`.
* Stop it with a normal shutdown (Ctrl+C, SIGTERM): the shutdown hook removes the socket file. A killed JVM leaves the
  file behind; the next start deletes it.

## The web server

Apache example:

```apache
LoadModule proxy_module modules/mod_proxy.so
LoadModule proxy_wstunnel_module modules/mod_proxy_wstunnel.so

DocumentRoot "/srv/app/web"
<Directory "/srv/app/web">
    Require all granted
</Directory>
DirectoryIndex index.php

ProxyRequests Off
ProxyPass /SessionSocket ws://127.0.0.1:1001/SessionSocket
ProxyPassReverse /SessionSocket ws://127.0.0.1:1001/SessionSocket
```

* The browser opens the WebSocket at the page's path followed by `SessionSocket`, on port 80 or 443. Serve the pages on
  the default port, link to the directory (`/?customers/search/search`, not `/index.php?...`), and proxy that path. With
  `SKYLIN_ROOT:/myapp` the path is `/myapp/SessionSocket`.
* When the page runs on another port, set `WEB_SOCKET_ALT_PORT:true` and let the browser reach port 1001 directly.
* With `BACKEND_EVAL:true`, the JVM calls `http://localhost/skylin/websocket/BackendEval.php`, so the web server must
  answer on `localhost` port 80 with the Skylin files at its root.
* `session.save_path` in `php.ini` must be writable by the JVM, which writes the session files.

## Permissions of the socket

* The JVM creates the socket directory when it is missing (`/sky` on Linux, `/tmp/sky` on macOS, `C:\sky` on Windows).
  Creating `/sky` needs the right to write in `/`, so on Linux create it once and give it to the JVM user.
* PHP (the web server user) must be able to connect to the socket file. The simplest setup runs both processes as the
  same user. Otherwise give both users access to the directory and the socket file. Allow no one else: whoever can
  connect to the socket can call into the JVM.

## Ports

| Port | Process | Interface | Purpose |
|---|---|---|---|
| 80 / 443 | web server | your choice | Pages, and the WebSocket path it proxies |
| 1001 | JVM | all interfaces | WebSocket server. Reach it through the proxy, or directly with `WEB_SOCKET_ALT_PORT` |
| 1400 | JVM | 127.0.0.1 | Bridge, only when AF_UNIX is not supported |

## Changing code while it runs

* **A screen file**: save it and open a new instance of the screen. `index.php?<path>!` rebuilds the screen the URL
  opens; a screen opened in a tab or pushed is rebuilt when it is opened again. Screens that are already open keep the
  code they were built with.
* **A class under `APPLICATION_PACKAGES`**: copy the new `.class` files into `bin/` (same package folders, a newer
  modification time) and open a new instance. The next `addAM` sees a newer file and loads the classes again for that
  instance; open instances keep their classes.
* **Anything else** (a jar on the classpath, the security provider, a `CONFIG_CLASS`, a `.cnf` value): restart the JVM.
  Every browser session starts again after a restart.

## Logs

* With `LOG_PATH`, the JVM writes `backend_out <date time>.txt` and `backend_errors <date time>.txt`, one pair per start.
* PHP errors are not shown in the page (`display_errors=0`); they go to PHP's `error_log`.
* "Error generating columns in View X" in `backend_errors` means the view's query failed when its columns were probed.
  The database's error message is a few lines above it.

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| "Application module ... did not load" | The class is not under `bin/` with its package path, its package is not in `APPLICATION_PACKAGES`, or its constructor threw (see `backend_errors`) |
| "Cannot find application <path>" | No file `applications/<path>.php` under the document root plus `SKYLIN_ROOT` |
| A blank area where a component should be | A PHP error during render; read the PHP error log |
| Every click reloads the page | The application id of the request no longer exists, usually because the JVM restarted |
| PHP cannot connect to the bridge | The JVM is not running, the socket directory or file is not accessible to the web server user, or the two sides disagree about AF_UNIX support |
| The WebSocket does not connect | No proxy for the `SessionSocket` path, the page is not on port 80/443, or the page URL is not a directory |
| "Error getting connections from the database used by <name>" | The pool cannot connect; it retries once a minute |
| `could not find method '<name>'` in the JVM output | A PHP call names a method that does not exist with that number of arguments, or a view or column name is misspelled |

## Tests

* `mvn test` in this repository runs the Java bridge tests.
* `php php/tests/expression.php` and `php php/tests/response-framing.php` need only PHP with `mbstring`.
* `php php/tests/bridge-roundtrip.php [session-directory]` needs a running JVM.
