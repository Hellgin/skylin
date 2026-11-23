# Architecture

Skylin is a server-side UI framework. A screen is a tree of PHP components that renders HTML on the server. The data
and business logic live in a Java process (the Skylin JVM). PHP calls into the JVM over a local socket for every value
it shows.

```
Browser --HTTP--> web server + PHP 8 --Unix socket--> Skylin JVM (Java 21) --JDBC--> database
   |              (skylin-web: components,              AM / View / ViewRow
   |               index.php, rest.php)                 SecurityProvider
   +--WebSocket (proxied to 127.0.0.1:1001)-----------> SocketServer
```

## The parts

| Part | Where | What it is |
|---|---|---|
| `com.distantmelody:skylin` (jar) | `java/src/main/java` | The backend, package `com.distantmelody.framework.skylin`: `Server`, the bridge listener, `AM`, `View`, `ViewRow`, `EditView`, `lov.Lov`, security, services |
| `com.distantmelody:skylin-web` (zip) | `php/` | The frontend: `index.php`, `rest.php`, `restless.php` and the `skylin/` directory (components, bridge client, CSS, JavaScript). Unpacked at the document root |
| Your application | your project | Screen files under `<document root>/applications/` and Java classes (AMs, views, rows) that the JVM loads |

## A request, end to end

1. The browser opens `index.php?<app path>` (for example `/?customers/search/search`). `skylin/InitRequest.php` sets up
   error reporting, the UTF-8 header and the session handler, which keeps the PHP session through the JVM.
2. `index.php` gets or creates the root `Application` in `$_SESSION` and a child application for the path. The first time
   that application renders, it includes `applications/<app path>.php`. That file builds the component tree with `$this`
   as the application.
3. Each component renders HTML. Values come from Java objects through the `java` proxy class
   (`skylin/bridge/java.php`): a call such as `$row->getValue('name')` is one round trip to the JVM.
4. A click or a changed field is a POST to the component's own PHP file (`skylin/Button.php` with `click=<id>`,
   `skylin/TextField.php` with `change=<id>`, and so on, sent by `skylin/skylin-precontent.js`). The file looks up the
   application and the component by id, runs the handler, and answers with a message list.
5. The browser applies the messages to the page.

The answer to a POST is a list of messages. The framing is `count,len1,len2,...-` followed by the messages. The lengths
are UTF-16 code units, because the browser cuts the messages with `substring` (`Response::browserLength`). The message
types include:

| Message | Effect in the browser |
|---|---|
| `setdiv` | Replace the element with that id by new HTML (what `refresh()` sends) |
| `setattr`, `setprop` | Set an attribute or a DOM property |
| `displaymsg` | Show a message box: `info`, `warn`, `error` or `fatal` (what `AM.info(...)` and friends send) |
| `reload` | Reload the page (sent when the application id in a request no longer exists) |
| `focus` | Focus an element and scroll it into view |
| `openwindow`, `download`, `seturl`, `settitle`, `scrolltobottom` | Window and page helpers |

## Frontend (PHP)

### The component tree

* A screen is an `Application`, which extends `Group`. Components are added with `add()`. The tree is stored in
  `$_SESSION` and survives between requests, so a handler runs on the same objects the page was built from.
* A component renders in a **context**: an array that holds `application`, `row` (the current data row), `sec` (the
  security context) and every property set with `setProp`. A child inherits the context of its parent. An `Iter` or
  `Table` sets `row` for each row it renders.
* Inside a component, `$this->a()` is the application, `$this->r()` the context row (a `DefaultRow` that answers null
  when there is none), `$this->c('name')` a context value, `$this->s()` the `SecurityContext`, and
  `$this->com('id')` a component of the same application by id.
* The DOM id of a component is `x<applicationId>x<componentId>x<rowLinkId>`. The row link id is a chain of link ids
  joined with `x` when rows are nested. Because `x` is the separator, `setId()` throws on an id that contains `x` or `X`
  (and on `$`).
* `refresh()` renders the component again and sends it as `setdiv`. `refreshInRow($row)` does the same for the copy of
  the component drawn for one row.

### Handlers are code strings

Handlers (`setActionE`, `setRenderedE`, `setValueE`, `setRowsE`, ...) are stored as PHP code strings and run with `eval`,
with `$this` being the component.

* **Why strings.** The tree is serialized into the session, and PHP closures cannot be serialized
  (`serialize()` throws "Serialization of 'Closure' is not allowed").
* **A closure is accepted and turned into a string.** Every setter whose name ends in `E` takes a string or a closure.
  `Expression::toExpression()` (`skylin/Expression.php`) reads the closure's body from the source file with
  `PhpToken` and stores that text. It refuses a closure that would mean something else once it is a string: one with
  parameters, `use (...)`, static variables, `fn` arrow functions, nested closures or functions, magic constants such as
  `__DIR__`, variable variables, a variable read before the closure assigns it, two closures on one line, and code that
  has no source file (eval'd code, first-class callables). Allowed: `$this`, superglobals, and variables the closure
  assigns itself. `php/tests/expression.php` has the rules as tests.
* **Handler strings are static.** Build them only from developer-defined text. Data reaches a handler when it runs,
  through `$this->r()`, `$this->c(...)` and the security context. See
  [building-a-screen.md](building-a-screen.md#handlers).

### Applications inside applications

* `$this->newChildApplication('customers/detail/detail')` creates a child application. The screen file is included
  when the child first renders, so properties set right after creating it (`$child->setProp('customerId', $id)`) are
  visible to the file as `$this->c('customerId')`.
* A child is placed like any component: added to a `Group`, or given to `TabStrip::setTab($heading, $child)`.
* `$child->push($this->a())` puts a child **on top of** the application that called it. The application below is
  not drawn while something is on top of it and keeps its state. `pop()` on the top application draws the one below
  again and destroys itself.
* Several instances of the same screen can be open at once. Each instance keeps the screen code and the Java classes it
  was created with (see "Class loading and hot reload" below).

## Backend (Java)

* **`AM` (application module).** One per screen (or per part of a screen). It owns views, created in its constructor
  with `new SomeView(this)`. PHP attaches it with `$this->addAM('am', 'com.example.app.web.customers.search.am.SearchAM', 'main')`.
  The third argument is a datasource name; `setDataSource` then generates the column metadata of every view
  (`genColumns`) and calls each view's `init()`, then the AM's `init()`.
* **`View`.** A list of rows. Either a SQL query with named binds (`setQuery`) or columns declared in code
  (`setColumnNames`, `setColumnTypes`). The name of a view is its class's simple name.
* **`ViewRow`.** One row. `getValue(col)` and `setValue(col, value)`. Subclasses add behaviour and validation
  (`validate`).
* **`EditView` / `EditViewRow`.** A view that records changed rows and has view, add and edit modes.
* **`lov.Lov`.** A view that drives a drop-down (`ChoiceLov`) or a popup list (`SearchLov`).
* **`SecurityContext` and `SecurityProvider`.** Login and roles. Providers are listed in `SECURITY_PROVIDERS`.
* **Datasources.** Pools registered at startup from `datasource.json` or from code (`Server.addDataSource`), looked up
  through JNDI as `java:comp/env/jdbc/<name>`.

### Class loading and hot reload

* Classes whose names start with an entry of `APPLICATION_PACKAGES` are loaded by `SkylinClassLoader` from
  `bin/<package path>/<Class>.class` under the JVM working directory, not from the classpath.
* `AMContainer.load(className)` keeps the loader that defined an AM. When a screen adds an AM, the container checks the
  `.class` files that loader read. If one is newer, a new loader defines the classes again for the new instance.
  Instances created earlier keep the classes they were created with.
* Services called through `rest.php` / `restless.php` follow the same rule (`ServiceContainer`, `ServiceClassLoader`).
* Classes named in `SECURITY_PROVIDERS`, `STARTUP_THREAD` and `CONFIG_CLASS` are loaded with `Class.forName` from the
  classpath. A change to them needs a JVM restart.
* A screen file is included once per application instance, the first time it renders. Editing a screen file affects
  the next instance, not the ones already open.

## Calls across the bridge

PHP holds Java objects as `java` proxies. A method call on a proxy sends the object id, the method name and the
arguments to the JVM, which runs the method and returns the result.

**Java to PHP:**

| Java value | PHP value |
|---|---|
| `String` | string |
| `Integer` | int |
| `Double`, `Float`, `BigDecimal` | float (`floatval`, 64-bit) |
| `Boolean` | bool |
| array or `Iterable` | PHP array of converted values |
| `null` | null |
| anything else (`Long`, `Timestamp`, a view, a row, ...) | a `java` proxy |

**PHP to Java:**

| PHP value | Java value |
|---|---|
| string | `String` |
| int | `Integer` |
| float | `BigDecimal` |
| bool | `Boolean` |
| array | `Object[]`; an array of only ints becomes `int[]`, of only bools `boolean[]` |
| `java` proxy | the object |
| null, `DefaultRow` | `null` |

**Method lookup** (`BridgeSession.callMethod`): a public method with that name and argument count whose parameter types
accept the arguments (with boxing between `int`/`Integer`, `boolean`/`Boolean`, `double`, and `int` to `long`). When
several match, the last one found wins. Varargs are not supported. When nothing matches, the JVM calls the object's
`handleVirtualMethodCall`. When that does not answer either, it logs `could not find method '<name>'` and returns null.

**Short forms.** `$proxy->name` (property read) is a call to the method `name` with no arguments, and falls back to
`handleVirtualMethodCall` the same way:

* `AM.handleVirtualMethodCall` returns the view with that name, so `$this->a()->getAM()->CustomerList` is the view.
* `View.handleVirtualMethodCall` returns a column of the current row, or of row `n` when one argument is passed.
* `ViewRow` returns a column of that row.
* `role()->SOME_ROLE` (`QuickRoleLookup`) answers `userInRole("SOME_ROLE")`.

On the PHP side, `Component::__get($name)` and `Application::__get($name)` forward to the application's first AM, so
`$this->CustomerList` in a handler is the view `CustomerList` of the first AM. `Application::__call` first looks for a
function registered with `register()`, then forwards to the first AM.

## Bridge transport and protocol

* **Unix domain socket on every OS**, including Windows 10 and later. Java side: junixsocket (`BridgeListener`). PHP side:
  `socket_create(AF_UNIX)` from the `sockets` extension (`skylin/bridge/BridgeConnection.php`).
* **The path is fixed, with no configuration.** It is derived from the OS in `BridgeListener.bridgeSocketPath` (Java) and
  `skylinBridgeSocketPath()` (PHP):

  | OS | Socket file |
  |---|---|
  | Windows | `C:\sky\bridge.sock` |
  | macOS | `/tmp/sky/bridge.sock` |
  | Linux and anything else | `/sky/bridge.sock` |

  The JVM creates the directory if it is missing, deletes a stale socket file at start, and removes the file at shutdown
  (a killed JVM leaves it behind, which is why it is deleted at start). The path must fit AF_UNIX (107 bytes).
* **TCP fallback.** Only when AF_UNIX is not supported: `127.0.0.1:1400` with `TCP_NODELAY`. Each side probes for AF_UNIX
  support on its own (`AFUNIXSocket.isSupported()` in Java, creating an AF_UNIX socket in PHP).
* **Request (PHP to JVM):** 7 ASCII decimal digits `N`, then `N` bytes: `<command>[,<arguments>]`. Lengths inside the
  arguments are byte lengths of UTF-8 text. A request over 1 MB is written to a temporary file and sent as `e,<path>`.
* **Reply (JVM to PHP):** 7 ASCII hex digits `M`, then `M` bytes: a type character and the reply. Type `3` is the reply
  itself. Type `4` is the path of a file that holds a reply over 1 MB (only when `UPLOAD_TMP` is set).
* **Callbacks.** While a call runs, the JVM can ask PHP to run code for an application (`1<appId>,<code>`, used by
  `AM.eval` when `BACKEND_EVAL` is on). PHP runs it on a new bridge connection and the call continues. The JVM does not
  wait for the result of that eval. A failure in it is written to the PHP error log and to SLF4J, and the Java call
  does not see the exception.
* **Bytes, not characters.** Java reads a frame as `byte[]` (`ByteReader`) and decodes UTF-8 once at each leaf value.
  Replies are encoded once (`ByteWriter`). Session data is kept as bytes.
* An idle connection is closed after 10 seconds.

## Sessions

* `BridgeSessionHandler` (PHP, `skylin/bridge/BridgeSessionHandler.php`) is PHP's session handler. It sends session
  open, read, write, close, destroy and gc to the JVM (bridge command `b`).
* The JVM writes the serialized session to `sess1_<session id>` in PHP's `session.save_path`. The JVM user therefore
  needs write access to that directory.
* One request per session runs at a time: open waits for a per-session lock that close releases. Two requests of the
  same session that arrive together run one after the other, in no guaranteed order.
* Java objects (AMs, views, rows) live in JVM memory, in a `BridgeSession` per PHP session id. When the JVM restarts,
  every browser session starts empty: the next page load builds the screens again, and a POST answers with a page
  reload.
* gc drops sessions that have been inactive for 180 minutes, whatever `session.gc_maxlifetime` says.

## WebSocket

* The JVM runs a WebSocket server on port 1001 (`SocketServer`, Java-WebSocket) with address reuse, so a restart can
  listen while connections from the previous process are still closing. The startup probe binds the same way and, if
  the port still cannot be taken, retries every 20 seconds.
* The browser connects to the page's host on port 80 (http) or 443 (https), at the page path followed by
  `SessionSocket`. For a page at `/` that is `/SessionSocket`, which the web server proxies to
  `ws://127.0.0.1:1001/SessionSocket`. With `WEB_SOCKET_ALT_PORT:true` the browser connects to `ws://<host>:1001`
  directly.
* Uses: server push from an AM (`AM.eval` outside a request, `customMessage`), a ping that keeps the session active, and
  `FORCE_REFRESH`, which reloads the page. Screens that do not push work without it.

## Services: `rest.php` and `restless.php`

* `rest.php?<fully.qualified.ServletClass>` calls a class that extends `javax.servlet.http.HttpServlet` in the JVM
  (bridge command `9`). The HTTP method, the `Authorization` header and the request body reach the servlet; the body is
  its input stream. Text after a second `?` in the URL is passed as the input when the request has no body (that is how
  `SessionBlob` receives its id). Response headers and body come back to the caller. There is no PHP session.
* `restless.php` does the same, and also attaches the caller's Skylin session (from the `PHPSESSID` cookie) as the
  request attribute `BRIDGE_SESSION`.
* Built-in services (`com.distantmelody.framework.skylin.services.builtin`): `SessionBlob` (downloads, used by
  `SkylinBlob.getUrl()`; a link is single-use and expires after 10 seconds), `FileUpload` and `MessageAM`.

## Text encoding

* UTF-8 end to end: `InitRequest.php` sends `Content-Type: text/html; charset=UTF-8`, the page has
  `<meta charset="utf-8">`, the bridge carries UTF-8 with byte lengths, and Java decodes with
  `StandardCharsets.UTF_8` (never the platform default).
* The browser response counts UTF-16 code units. `Response.php` makes the text valid UTF-8 first (`mb_scrub`), so the
  `mbstring` extension is required.
* What a database can store is up to the database and its column character sets.

## Tests

* `java/src/test` (run by `mvn test`): the bridge wire format with 1 to 4 byte characters, split writes, two frames in one
  write, frames over 1 MB, binary session data, over TCP and over a Unix socket.
* `php/tests/expression.php` and `php/tests/response-framing.php` need only PHP (with `mbstring` and `tokenizer`):
  `php php/tests/expression.php`.
* `php/tests/bridge-roundtrip.php` needs a running JVM and goes through the real PHP bridge code.
