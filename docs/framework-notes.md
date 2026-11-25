# Framework notes

Behaviour that is easy to get wrong, known limits, and what changed compared with the original Skylin. Each item was
checked against the code in this repository.

## Behaviour to know

### Handlers and components

* **Handlers are static code strings.** See [building-a-screen.md](building-a-screen.md#handlers) for the rules and
  for what a closure may contain.
* **Text setters build code.** `Button::setText`, `Component::setTitle`, `SearchLov::setTextStyle` and
  `Button::setImage` store `return "<text>";`. `TextField::setEditable`, `TextField::setMinDec`,
  `Component::setRendered`, `FileArea::setEditable`, `File::setUploadRendered`, `File::setDownloadRendered` and
  `Splitter::setClosed` store `return <value>;`. Pass developer constants only, or use the `E` variant.
* **`SearchLov::setInListValidation($v)` does not work.** It combines its strings with `||`, which yields `true`, and
  the next value change fails with a `ParseError`. Use `setInListValidationE('return false;')`.
* **`TextField::setValue` and `setValueE` make the field read-only** (editable expression `return false;`).
  `setValueE` also replaces the type expression. A link that is not editable renders disabled and ignores clicks.
* **Not escaped:** `Date` (value and read-only text), `ChoiceLov` (option text and read-only label), `SearchLov` popup
  cells, `Label`, `Group::setText` headings, `Button` text, `Template` string bindings, and the page title. Show data
  through `TextField`, which escapes by default.
* **Component ids** must not contain `x`, `X` or `$`; `setId` throws.
* **`com('id')` finds a component once it has been rendered** (components register with their application when they
  first render), and only within the same application. `$this->id` is the same lookup.
* **A component id hides a view of the same name.** `$this->name` returns the component with that id when there is one
  and asks the first AM only otherwise. Reach the view through `$this->getAM()->name`.
* **`Group::add(&$com)` takes its argument by reference.** `add(new X())` works and raises a notice that the framework's
  error level hides.
* **`Grid(x, y)`** renders exactly `x * y` cells. Fewer children leave empty cells; extra children are not rendered.
* **An `Iter` or a `Group` with `noTag()`** has no element of its own. `refresh()` on it has nothing to replace;
  refresh its parent.
* **`Template` renders every `setCom` component** each time it renders, whatever the HTML around the binding point
  hides. To hold the whole template back (a dialog before it opens), put `setRenderedE` on the template, not on its
  components.
* **`ModalPanel::setStyle`** goes on the inner div and overrides the `.modalDialog > div` rule of `skylin.css` (400px,
  margin, padding, rounded corners, gradient).
* **`TabStrip` identifies tabs by heading.** Two screens opened with the same heading share one tab: the second replaces
  the first. `setCloseable($heading, $closeable)` looks the tab up by position as if it were the sequence number, so call
  it right after `setTab`, before any tab has been closed. `setTabRenderedE` and `setTabEditableE` find the tab by
  heading.
* **A render of a `FileArea` stops its uploads in progress.** Each render replaces the area's upload token, so files
  still waiting to be sent from before the render are refused (F5, a refresh of the area or of anything around it).
  Refresh the component that lists the files, not the area.
* **A `FileArea` that is not editable or not rendered withdraws its token when it renders.** One inside a container
  that is hidden is not rendered at all, so it keeps the token of its last render until it renders again.
* **A tab hidden with `setTabRenderedE`** is left out when the whole strip renders, and `changeTab` ignores it. A tab
  added with `setTab` is drawn at once without that check, and a hidden tab that is the current one leaves no tab
  selected.
* **`getTopApplication()` compares applications with `==`**, which compares the objects property by property through the
  whole tree. Pass the application you need in a context property instead.
* **Page title, icon, stylesheet and head** (`setTitle`, `setIcon`, `setCss`, `setHead` on an application) are taken from
  the first application that renders on the page, normally the screen the URL opens.
* **A plain page reload keeps the screen in the session.** `index.php?<path>!` builds it again from the file.
* **`Template`** has no `add()`. The HTML file is read when the template is created, binding points are replaced with
  `str_replace`, and nothing is escaped. The template adds no id to its root element.
* **`ModalPanel`** renders closed after a refresh, unless `open()` follows the refresh.
* **Button permission checks run on the server.** `Button::click` does nothing while `setEditableE` is false or the
  button is not rendered.
* **`Label` takes its caption in the constructor** (`new Label('text')`). It has no `setValueE`. A method it does not
  have is forwarded to the application, so calling `setValueE` on a label does not store text. A note whose text
  changes is a `TextField`.
* **`setProp` keeps the value it is given.** `setProp('row', $row)` still holds that object after the view is cleared
  and filled again. `setPropE` stores an expression and evaluates it each time the component renders, so a single
  current row is `setPropE('row', ...)` returning `getFirst()`. Share that row across siblings with a `Group` that has
  `noTag()`.
* **A click finds its row by the link id in the element id**, through the view of the row the component last rendered
  (`getRowByLinkId`). `clear()` does not reset link ids, so an element drawn for a removed row finds no row and the
  handler sees the `DefaultRow`. Draw the container with the `setPropE('row', ...)` again after replacing the row.
* **`refresh()` reuses the context of the last render.** Only a component with its own `setPropE` reads the row
  again. A field refreshed on its own draws the row it was last drawn with. `refreshInRow($row)` needs the row the
  element was drawn for, because the id is built from it.
* **The frontend relies on undefined keys and properties reading as null.** `InitRequest.php` sets
  `error_reporting(E_ERROR | E_PARSE)` and `display_errors=0`, so a warning from a screen and one from the framework both
  stay out of the page. A blank area usually means a fatal error. It is written to the PHP error log and to SLF4J.

### Views and data

* **Bind handling is textual.** `setQuery` finds `:name` outside single quotes, ends the name at a space, `)`, `,`, `|`,
  a tab or a line break, and replaces it with `?` using `String.replaceFirst`. A colon outside single quotes always
  starts a bind, including PostgreSQL's `::` cast; use `cast(x as type)`. Bind names become camelCase.
* **`setBindVar` throws** when the view has no bind of that name.
* **`setBindVarArray(name, oracleType, value, connection)`** binds an Oracle collection. It unwraps the pool connection
  with `getInnermostDelegate`, then builds `oracle.sql.ArrayDescriptor` and `oracle.sql.ARRAY` by reflection and binds
  that array. The framework does not compile against the Oracle driver. A connection or driver without those classes
  reports the failure with `am.error` and still calls `setBindVar`, usually with a null array.
* **A query writes columns straight into `row.values`.** It does not call `setValue` or `setValueInternal`. An override
  of `setValue` runs when code calls `setValue`, including a user edit through `setValueFromFrontEnd`, and does not run
  when the row is loaded.
* **`EditViewRow.setValueInternal` returns immediately** when the new value equals the current one, so the column is
  not stored again and not marked modified. `setValue` calls that and then continues, so code after `super.setValue`
  still runs for an equal value. Read the previous value before `super.setValue`.
* **`getViewObject` works after `setDataSource`.** That call indexes the views (`initInternal`) and then runs `init()`.
  The AM constructor runs before it, so `getViewObject` in the constructor fails.
* **A row class filled by a query** must be concrete and have a public constructor that takes the `View`. A nested
  class used from another package must be public.
* **`SqlUtil.safeClose` rolls the connection back.** A write on a connection from `getNewConnection` is kept only when
  the code commits before that close.
* **Column generation probes the query** with `select * from (<query>) col_gen_sub where 1 = 2`, `limit ?` as `limit 1`,
  `offset ?` as `offset 0`, and every other `?` as `null`, including a `?` inside a string literal. A failure prints
  "Error generating columns in View <name>" after the SQL error.
* **Column types are normalised.** `Integer`, `Long`, `Short`, `Double`, `Float` and `BigInteger` columns become
  `BigDecimal`; `java.sql.Date`, `LocalDateTime` and `LocalDate` become `Timestamp`; `Blob` and `byte[]` become
  `SkylinBlob`. A column name with anything but letters, digits and `_` stops column generation; give it an alias.
* **`executeQuery()` and errors.** A `SQLException` becomes a fatal message box. Any other exception is printed and
  swallowed, and the view ends up empty.
* **`executeQuery()` with no query** returns before it clears the rows, so rows built in code stay.
* **`setEditableColumns` with a name the view does not have** shows a fatal message instead of throwing.
* **Sorting** compares `BigDecimal` columns as numbers and every other column by its text (`10` before `2`).
* **Linked views run again when a linked value changes.** The comparison is by reference, so a value replaced by an
  equal copy runs the query again too.
* **Move rows with `ViewRow.transfer`.** It fixes the link id and the cached ids; moving a row by hand does not.
* **Bridge types.** `Long`, `Timestamp` and other objects reach PHP as proxies (`strlen` on one is not its text). A
  `BigDecimal` reaches PHP as a 64-bit float.
* **`AM.error(SQLException)`** shows Oracle application errors 20900, 20901 and 20902 as error, warning and info, takes
  the message text out of a PostgreSQL error, and shows any other database's error as a fatal message with the driver's
  text.
* **The datasource type defaults to `oracle`.** Set `"type"` for MySQL and PostgreSQL. An unknown type stops the JVM
  when the pool first connects.

### Requests, sessions and the JVM

* **Requests of one session run one at a time, in no guaranteed order.** The browser sends a field's change (on blur)
  and a click on the button next to it as two requests. When both arrive together the click can run first and see the
  previous value. Scripted tests should wait for the change request to finish before they click.
* **A JVM restart resets every session.** The next page load builds the screens again under a new session id; a POST
  answers with a reload.
* **The session id from a cookie is used only when the JVM knows the session** (`session.use_strict_mode`). Any other
  id, and any id that is not 22 to 128 letters, digits and `-`, is replaced with a new one.
* **Sessions expire after 180 minutes without activity** when PHP's session gc runs, whatever
  `session.gc_maxlifetime` says.
* **Download links** (`SkylinBlob.getUrl`) are single-use and expire after 10 seconds.
* **`AM.eval(code)` outside a bridge request posts to `http://localhost/skylin/websocket/BackendEval.php`**, which
  needs a web server answering there with a free worker. The body starts with a random token the JVM makes at start.
  `BackendEval.php` answers 403 unless the caller's address is loopback and the JVM confirms the token (bridge command
  `g`), and checks both before it opens the session named in the body. A same-request eval sends the code on the
  bridge and does not wait for a result, so a PHP exception inside it never comes back to Java.
* **A failed `eval` is logged twice.** `InitRequest.php` catches an uncaught exception, including one from a handler
  `eval`, writes it to the PHP error log and to `com.distantmelody.framework.skylin.util.Log.error`, and answers HTTP
  500 when the headers are still open. `Application::backendEval` catches its own failure, logs it the same way, and
  does not throw, so the bridge call that asked for the eval can finish.
* **Framework messages go through SLF4J**, one logger per class. PHP calls
  `java('com.distantmelody.framework.skylin.util.Log')->error($message)` (also `info`, `warn` and `debug`).
  `Out::println` is `FrontEndOut` at info. The jar ships `slf4j-api` and no binding; without a binding those lines are
  discarded. `LOG_PATH` still copies standard output and standard error into the dated files, and each complete line is
  also passed to SLF4J (`skylin.stdout` at info, `skylin.stderr` at error). A line written by an SLF4J appender is not
  passed back.
* **The WebSocket server sets reuse address**, and the startup probe binds port 1001 with `SO_REUSEADDR`, so a restart
  can listen while connections from the previous process are still closing. If the port still cannot be bound, startup
  waits and retries every 20 seconds.
* **Classes under `APPLICATION_PACKAGES` must be in `bin/`.** A class that is missing there does not load, even when it
  is on the classpath. Entries of `APPLICATION_PACKAGES` are not trimmed.
* **`SecurityContext.login(user, domain)` and `login(user, options)` without `LoginPassword` do not check a password.**
  They collect roles only.
* **`SecurityContext.getUsername()` answers `TEST`** while nobody is logged in.
* **`SHUTDOWN_DAILY`** makes the JVM exit at midnight; something has to start it again.

## Not tested

* macOS. junixsocket ships a macOS library and the socket path is `/tmp/sky/bridge.sock`, but it has not been run there.
* The TCP fallback for Windows before 10 1803.
* Parts of the PHP frontend have not been reviewed for PHP 8 changes: old `each()` / `create_function` style code, and
  string to number comparisons. The paths that screens use (rendering,
  tables, lists, tabs, templates, login) work on PHP 8.3.
* The two socket path functions (`BridgeListener.bridgeSocketPath` in Java and `skylinBridgeSocketPath` in PHP) are kept
  in sync by hand; no test compares them.

## Changes compared with the original Skylin

### PHP frontend

* PHP 8 port: `InitRequest.php` lowers the error level; `bridge/java.php` declares `class java` before
  `session_start()` (PHP 8 does not hoist the class, and the stored session holds `java` proxies);
  `Table::handlePageLimit` uses explicit casts (`$rowCount.''-1` was a `TypeError` in PHP 8, which broke every
  `Table`); undefined variables in `Date.php` and `ChoiceLov.php` are initialised.
* `TextField` escapes its value by default; `noEscape()` turns that off. A screen written for the original Skylin that
  puts HTML into a `TextField` value now shows the tags as text until it calls `noEscape()`.
* The `E` setters accept closures (`Expression::toExpression`).
* `Application::render()` gives the wrapper div of an application that has another one pushed on top of it the
  application's own classes. `Grid` puts the classes added with `addClass` on its root div.
* The browser response counts its lengths in UTF-16 code units, so text outside ASCII is cut in the right places.
* Brought over from the pitsrc copy of Skylin: `FileArea` (drop files on an area; it posts to `restless.php`, where the
  pitsrc version used `rest.php` with the session id in the header), `$this->id` finding a component, `finalInit()` for
  every AM after the screen file, `Component::setRendered`, `TabStrip::setTabRenderedE`, and `Splitter::setClosed`,
  `Splitter::setClosedE` and `Table::setHeight` returning the component for chaining. The `FileArea` port escapes the
  file names it writes, accepts a closure in `setViewE` and `afterUploadE`, ignores an upload notification for a
  component that is not a rendered `FileArea`, handles `dragover` so the browser fires `drop`, reads folders of any
  size, and uploads one file at a time with the file name URL-encoded. It sends the view's upload token instead of the
  column names (`View.allowFileUpload`), so `FileUpload` writes only to views and columns a `FileArea` opened. The
  token is replaced on every render of the area and withdrawn when the area renders not editable (`setEditableE`, new)
  or not rendered.

### Java backend

* Framework messages go through SLF4J. `LOG_PATH` still copies standard output and standard error into the dated files, and each of those lines is also passed to SLF4J. An uncaught PHP exception is written to the PHP error log and to `com.distantmelody.framework.skylin.util.Log`.
* MySQL support in `DatabaseConnector` (see [configuration.md](configuration.md#datasourcejson)).
* Oracle `ROWID` handling was removed from `EditView` and `EditViewRowDeleted`. `View.setBindVarArray` builds an
  `oracle.sql.ARRAY` by reflection, so the framework does not compile against the Oracle driver. Calling it without
  that driver fails. Oracle remains a datasource type.
* `Short`, `Float` and `BigInteger` columns become `BigDecimal` too (the original widened `Integer`, `Double` and
  `Long`), and `LocalDateTime` / `LocalDate` columns are mapped to `Timestamp`, which is what the MySQL driver returns for
  `DATETIME` / `DATE`.
* Column generation probes `limit ?` / `offset ?` as `limit 1` / `offset 0` (it used `limit null`), so a list view can
  use `limit :max_rows`.
* The bridge uses a Unix socket on Windows 10 and later too, with junixsocket 2.11.1 (`junixsocket-common` plus
  `junixsocket-native-common`, which carries the Windows library). It parses bytes and decodes UTF-8 once per value.
* `Server` starts Apache itself only when `APACHE_EXEC` is set (the original always did on Windows).
* Datasources can be registered from code (`Server.addDataSource`, `CONFIG_CLASS`), and `DATASOURCE_FILE:disabled`
  skips the file.
* `BackendEval.php` runs code only for a loopback caller with the JVM's token. `AM.websocketRefreshComponent` and
  `skylin/websocket/RefreshComponent.php` are gone: refresh with `eval("$this->com('id')->refresh();")`.
* `rest.php` and `restless.php` create only `HttpServlet` subclasses. The original created any class named in the URL
  and cast it afterwards, so the constructor of a class that is not a service ran.
* `MessageAM` ignores `sessionId` in the body and uses only the session of the request (the `PHPSESSID` cookie through
  `restless.php`).
* `FileUpload` takes the session from the request too, so it is called through `restless.php`, not `rest.php`. Its
  `Authorization` header is `<view id>_<token>_<file name>` instead of
  `<view id>_<name column>_<blob column>_<session id>_<file name>`: no session id, the columns come from the grant the
  token names (`View.allowFileUpload`), and the file name is URL-encoded. A view takes uploads only after a `FileArea`
  has rendered for it.
* Session ids are checked. The original used the `PHPSESSID` cookie as it came, in the session file name
  (`sess1_<id>`) and in bridge commands, so an id such as `../../file` reached files outside the save path, and an id
  with a comma changed the command. PHP and the JVM now accept only 22 to 128 letters, digits and `-` (open sends an
  empty id instead, and the other session commands and the session bind refuse it), and the JVM refuses a session file
  outside the save path. `BridgeSessionHandler` turns on `session.use_strict_mode`, implements `validateId` (bridge
  command `b,7`, new), `create_sid` (32 hex digits) and `updateTimestamp`, and uses the PHP 8 method signatures. An id
  the JVM does not know is replaced, so a JVM restart gives every browser a new session id (the original kept the id
  with an empty session). `restless.php` attaches no session for a cookie that is not a valid id, and `java.php` stops
  the request with a 500 when the JVM refuses the session id.
