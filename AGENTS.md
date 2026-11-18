# Skylin: guide for agents

Skylin is a server-side UI framework. A screen is a PHP file that builds a tree of components (`Group`, `TextField`,
`Table`, ...). The tree lives in the PHP session and renders HTML on the server. Data and business logic live in a Java
process, the Skylin JVM, as application modules (`AM`), views (`View`) and rows (`ViewRow`). PHP calls the JVM over a
local Unix socket for every value it shows. A click is a POST that runs a handler on the server and returns a list of
DOM updates.

## Read in this order

1. [docs/architecture.md](docs/architecture.md): the parts, a request end to end, the bridge, sessions, class loading.
2. [docs/building-a-screen.md](docs/building-a-screen.md): a tutorial from a static screen to search, edit, drill-down
   and templates, with code.
3. [docs/configuration.md](docs/configuration.md): every key of `FrameworkParams.cnf`, `CustomParams.cnf`,
   `datasource.json`, security providers, `php.ini`.
4. [docs/running.md](docs/running.md): requirements, layout, starting the JVM, the web server, hot reload,
   troubleshooting.
5. [docs/framework-notes.md](docs/framework-notes.md): behaviour that is easy to get wrong, known limits, changes
   compared with the original Skylin.

## Rules that matter most

* **Handlers are static code strings.** `setActionE`, `setRenderedE`, `setValueE` and every other `...E` setter take a
  string or a closure; a closure is converted to the text of its body and may use only `$this`, superglobals and
  variables it assigns itself (no parameters, no `use`, no `fn`, one closure per line). Never build a handler string
  from request input or database values. Read data when the handler runs: `$this->r()` (row), `$this->c('name')`
  (context), `$this->a()` (application), `$this->s()` (security), `$this->com('id')` (component).
* **Text setters wrap their argument in code.** `Button::setText`, `setTitle`, `TextField::setEditable` and similar
  build `return "...";` or `return ...;`. Pass constants only, or use the `E` variant.
* **Bind fields to columns.** `new TextField('col')` reads and writes `col` of the context row, takes its editability
  from `ViewRow.isUpdateable` and its input type from the column type. Set the row with `setProp('row', $row)` on a
  container, or use `Iter` / `Table`, which set it per row.
* **Escape through `TextField`.** It escapes by default. `Date`, `ChoiceLov`, `SearchLov` popup cells, `Label`,
  `Button` text and `Template` strings do not escape.
* **SQL uses named binds** (`:snake_case` in the query, `setBindVar("camelCase", ...)`). Never concatenate input into
  SQL. A colon outside single quotes always starts a bind.
* **Columns are camelCase** (`customer_id` is `customerId`). Number columns are `BigDecimal`, dates are `Timestamp`.
  Leave a number a number; use `new Date('col')` for dates.
* **Validation lives in the row**: `ViewRow.validate(name, value, serverValue)` calls `validationError(sentence)`. After
  a failed save, call PHP `validate()` and refresh: the fields draw their own errors.
* **Component ids** may not contain `x`, `X` or `$`.
* **Refresh what changed.** A handler that changes data calls `->refresh()` (or `refreshInRow($row)`) on the components
  that show it. Nothing redraws by itself.
* **Move rows between views with `ViewRow.transfer`**, never by hand.
* **`TabStrip` identifies tabs by heading.** Put a record id in the heading for one tab per record.
* **Java classes under `APPLICATION_PACKAGES` load from `bin/`** in the JVM working directory and reload for new screen
  instances when their `.class` files change. Everything else needs a JVM restart.
* **One request per session runs at a time**, in no guaranteed order. A JVM restart resets every session.

## Where the reference code is

| Topic | Read |
|---|---|
| Application, child applications, push / pop, `register` | `php/skylin/Application.php` |
| Context, ids, refresh, classes, styles | `php/skylin/Component.php` |
| Closures to handler strings | `php/skylin/Expression.php`, tests in `php/tests/expression.php` |
| Fields, links, text areas, escaping | `php/skylin/TextField.php` |
| Containers and lists | `php/skylin/Group.php`, `Iter.php`, `Grid.php`, `ModalPanel.php`, `TabStrip.php`, `Splitter.php` |
| Tables | `php/skylin/Table.php` |
| Lists of values | `php/skylin/Lov.php`, `ChoiceLov.php`, `SearchLov.php`, `java/.../lov/Lov.java` |
| Templates | `php/skylin/Template.php` |
| Response messages | `php/skylin/Response.php`, `php/skylin/skylin-precontent.js` |
| Page entry, URL forms | `php/skylin/entry/index.php`, `php/skylin/InitRequest.php` |
| Bridge (PHP side) | `php/skylin/bridge/java.php`, `BridgeConnection.php`, `BridgeSessionHandler.php` |
| Views, rows, edit views | `java/src/main/java/com/distantmelody/framework/skylin/View.java`, `ViewRow.java`, `EditView.java`, `EditViewRow.java` |
| Application modules, messages, connections | `java/.../skylin/AM.java`, `AMContainer.java`, `SkylinClassLoader.java` |
| Security | `java/.../skylin/SecurityContext.java`, `SecurityProvider.java`, `security/` |
| Startup and configuration | `java/.../skylin/server/Server.java`, `DatabaseConnector.java` |
| Bridge (Java side) | `java/.../skylin/bridge/BridgeListener.java`, `BridgeConnection.java`, `BridgeSession.java` |
| Services | `php/skylin/entry/rest.php`, `java/.../skylin/services/` |

`java/...` is `java/src/main/java/com/distantmelody/framework`.

## Verifying

* `mvn test` runs the Java bridge tests.
* `php php/tests/expression.php` and `php php/tests/response-framing.php` need only PHP with `mbstring`.
* `php php/tests/bridge-roundtrip.php` needs a running JVM.

## Working on this repository

* Java sources are in `java/src/main/java`, the web files in `php/`. The `skylin-web` zip is `php/` without
  `php/tests`.
* Files under `php/skylin/dependencies` are third-party libraries with their own licenses.
* When framework behaviour changes, update the matching section of the docs above.
