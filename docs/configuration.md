# Configuration

The JVM reads its configuration from its **working directory** when it starts. PHP does not read these files; it asks
the JVM (`$_SESSION['UTIL']->getFrameworkParam('KEY')`). A changed value needs a JVM restart.

| File | Required | Read by |
|---|---|---|
| `FrameworkParams.cnf` | yes | `Server.main`; framework settings, listed below |
| `CustomParams.cnf` | yes (may be empty) | `Server.main`; your application's own settings |
| `datasource.json` | unless `DATASOURCE_FILE` is `disabled` | `Server.main`; connection pools |
| `bin/` | when `APPLICATION_PACKAGES` is used | `SkylinClassLoader`; your application classes |

## File format of the `.cnf` files

```
# a comment
DEFAULT_APPLICATION:home/home
APPLICATION_PACKAGES:com.example.app.web
LOG_PATH:logs
```

* One `KEY:value` per line, UTF-8. The line is split at the **first** colon, so a value may contain colons
  (`LOG_PATH:C:\skylin\logs`).
* The value is not trimmed: write `KEY:value`, not `KEY: value`.
* Empty lines and lines whose first non-blank character is `#` are skipped. Every other line needs a colon; a line
  without one stops the JVM at start.
* A key that is not in the file is null. A key with nothing after the colon is an empty string.
* When either file is missing, the JVM prints the exception and exits.

## FrameworkParams.cnf

### Applications and pages

| Key | Value | Effect |
|---|---|---|
| `DEFAULT_APPLICATION` | a screen path, or `host,path,host,path,...` | The screen `index.php` opens when the URL names none. With pairs, the request's host (without a leading `www.`) picks the path; a host that is not listed gets "Cannot find application". Unset: the page shows a placeholder label |
| `SKYLIN_ROOT` | empty, or a path such as `/myapp` | Where the Skylin web files are, relative to the document root. Used for `applications/`, `Template` files and `SkylinBlob` URLs. Leading slash, no trailing slash |
| `ALLOW_NON_DEFAULT_APPLICATION_HOST` | comma list of host names | When set, only these hosts (compared in upper case) may open a screen named in the URL; others get "Not allowed on this host" |
| `NON_DEFAULT_EXCEPTIONS` | comma list of screen paths | Screens any host may open by URL, despite `ALLOW_NON_DEFAULT_APPLICATION_HOST` |
| `EXTRA_JS` | comma list of script URLs | Added as `<script>` tags in the page head, after `skylin/skylin-precontent.js` |
| `WEB_SOCKET_ALT_PORT` | `true` | The browser connects to `ws://<host>:1001` instead of the page's own port and path |

### Java classes

| Key | Value | Effect |
|---|---|---|
| `APPLICATION_PACKAGES` | comma list of package prefixes, no spaces | Classes in these packages are loaded from `bin/` and reloaded when their `.class` files change. A `.` is appended to each entry. Always set it: the built-in default is a legacy value that matches no new application |
| `SECURITY_PROVIDERS` | comma list of class names | Each class implements `com.distantmelody.framework.skylin.SecurityProvider` and is created once at start. Without it, `SecurityContext.login` has no providers to call |
| `CONFIG_CLASS` | comma list of class names | Each is a `Runnable`, created and run on the main thread, in order, right after the datasource file step and before the bridge opens. Typical use: `Server.addDataSource(...)` from your own configuration |
| `STARTUP_THREAD` | comma list of class names | Each is a `Runnable`, started on its own thread after the bridge and the web socket server have started |

Classes named in `SECURITY_PROVIDERS`, `CONFIG_CLASS` and `STARTUP_THREAD` are loaded with `Class.forName` from the
classpath, not from `bin/`.

### Datasources

| Key | Value | Effect |
|---|---|---|
| `DATASOURCE_FILE` | a path, or `disabled` | The datasource file to read. Default `datasource.json`. `disabled` skips it (register pools from a `CONFIG_CLASS` instead) |

### Runtime

| Key | Value | Effect |
|---|---|---|
| `LOG_PATH` | a directory | Legacy. Standard output and standard error are still written to `backend_out <date time>.txt` and `backend_errors <date time>.txt` there, one pair per start. Each complete line is also passed to SLF4J (`skylin.stdout` at info, `skylin.stderr` at error). Framework messages themselves go through SLF4J |
| `UPLOAD_TMP` | a directory | A bridge reply over 1 MB is written to a file there and PHP reads the file. Without it, large replies go inline |
| `SHUTDOWN_DAILY` | any value | The JVM exits at the next midnight (local time), for a supervisor to restart it |
| `APACHE_EXEC` | path to `httpd.exe` | Windows only. The JVM ends any running `httpd.exe`, starts this one, and stops it at shutdown |
| `PHP_LOG_PATH` | path to the PHP error log | With `APACHE_EXEC`: the log the JVM follows and echoes |
| `HOST`, `PORT` | | Read only by `Component::genToken()`, which also needs the APCu functions |

## CustomParams.cnf

Your application's own settings, in the same format. Skylin does not interpret them.

* Java: `Util.getCustomParam("KEY")` (`com.distantmelody.framework.skylin.util.Util`).
* Java, through the servlet context: `ContextParams.get("KEY")`, or `getInitParameter("KEY")` on the servlet context a
  service receives.
* PHP: `$_SESSION['UTIL']->getCustomParam('KEY')` or `$_SESSION['CONTEXT_PARAMS']->get('KEY')`.

## datasource.json

```json
{
  "datasource": [
    {
      "name": "main",
      "type": "mysql",
      "server": "127.0.0.1",
      "port": 3306,
      "database": "app",
      "user": "app",
      "password": "<password>",
      "min": 2,
      "max": 10
    }
  ]
}
```

* `name` is what screens pass to `addAM(..., 'main')` and what `AM.getNewConnection()` looks up
  (`java:comp/env/jdbc/main`).
* `type` is `oracle` (the default when it is left out), `mysql` or `postgresql`. Any other value stops the JVM when the
  pool first connects. Set it explicitly.
* The JDBC URL is built from `server`, `port` and `database` in `server/DatabaseConnector.java`:

  | Type | URL |
  |---|---|
  | `oracle` | `jdbc:oracle:thin:@<server>:<port>:<database>` |
  | `mysql` | `jdbc:mysql://<server>:<port>/<database>?...` with, among others, `tinyInt1isBit=false` (a `TINYINT(1)` is a number), `zeroDateTimeBehavior=CONVERT_TO_NULL`, `characterEncoding=UTF-8`, `connectionTimeZone=UTC` and `forceConnectionTimeZoneToSession=true` |
  | `postgresql` | `jdbc:postgresql://<server>:<port>/<database>` |

  Check the full parameter list in `DatabaseConnector` against what your database server requires.
* `min` connections are opened and kept; the pool shrinks back to `max`. Connections are checked one per second. When
  the database cannot be reached, the pool retries once a minute and logs each attempt.
* JDBC drivers are not bundled. Put the driver jar for your database on the JVM's classpath.
* Connections from a pool have auto-commit off. `EditView.save()` commits; code that writes with its own SQL commits
  itself.
* This file holds credentials. Keep it out of version control, or set `DATASOURCE_FILE:disabled` and register the pools
  from a `CONFIG_CLASS`:

  ```java
  public class AppConfig implements Runnable
  {
      @Override
      public void run()
      {
          Server.addDataSource("main", host, 3306, "app", user, password, 2, 10, "mysql");
      }
  }
  ```

## Security providers

```java
public class AppSecurityProvider implements SecurityProvider
{
    @Override
    public boolean authenticate(String username, LoginOption... options) { /* check the password */ }

    @Override
    public List<String> authorize(String username, LoginOption... options) { /* the user's roles */ }

    @Override
    public String getTrueUsername(String username, LoginOption... options) { return username; }
}
```

* `SecurityContext.login(username, password, domain)` asks every provider to authenticate and collects the roles of
  those that succeed. `login(username, LoginOption...)` does the same with `LoginPassword`, `LoginDomain` and
  `LoginRequiredPrivilege` options; `LoginRequiredPrivilege` makes the login fail when the role is missing.
* `login(username, domain)`, and `login(username, LoginOption...)` without a `LoginPassword`, do not check a password:
  they only collect roles. Use them only after your own authentication step.
* `userInRole(role)`, `getRoles()`, `logout()`, `isLoggedIn()`. `getUsername()` answers `TEST` while nobody is logged
  in.
* In PHP the security context is `$this->s()`. An AM gets the same context when it is added (`AM.userInRole`,
  `AM.getUsername`, `AM.role()`).

## The JVM working directory

```
FrameworkParams.cnf
CustomParams.cnf
datasource.json            unless DATASOURCE_FILE says otherwise
bin/                       .class files of APPLICATION_PACKAGES
lib/                       the skylin jar, its dependencies, your jar, a JDBC driver (any layout the -cp covers)
logs/                      if LOG_PATH:logs
```

The bridge socket path does not depend on the working directory; see
[architecture.md](architecture.md#bridge-transport-and-protocol).

## php.ini

* Extensions: `sockets` (the bridge), `mbstring` (response framing), and `tokenizer` (closures in `Expression`, usually
  built in).
* `session.save_path` must be a directory the **JVM** can write: the JVM writes the session files there.
* Skylin turns on `session.use_strict_mode` itself and makes its own 32-hex-digit session ids, so `session.sid_length`
  and `session.sid_bits_per_character` do not apply. A session id from a cookie is used only when it is 22 to 128
  letters, digits and `-` and the JVM knows the session.
* `default_charset = "UTF-8"`.
* Skylin sets `error_reporting(E_ERROR | E_PARSE)` and `display_errors=0` in `InitRequest.php` for its own requests,
  because the frontend relies on undefined keys and properties reading as null. Keep `log_errors` on with an
  `error_log` file. An uncaught exception, including a failed `eval`, is written there and to SLF4J
  (`com.distantmelody.framework.skylin.util.Log`).
