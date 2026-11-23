# Building a screen

A screen is one PHP file that builds a component tree, usually backed by a Java application module (AM) with views.
This guide builds up from a static screen to search, edit, drill-down and client-side behaviour. The examples use a
made-up application in the package `com.example.app`.

How the pieces fit together is in [architecture.md](architecture.md). Setup is in [running.md](running.md).

## Where the files go

```
<document root>/
  index.php, rest.php, restless.php, skylin/        from the skylin-web zip
  applications/
    customers/search/search.php                     screen "customers/search/search"
    customers/detail/detail.php                     screen "customers/detail/detail"
    customers/detail/template/notes.html            a template used by that screen

<JVM working directory>/
  FrameworkParams.cnf, CustomParams.cnf
  bin/com/example/app/web/customers/search/am/SearchAM.class
  bin/com/example/app/web/customers/search/vo/CustomerList.class
```

* The screen path is the file path under `applications/` without `.php`. `index.php?customers/search/search` opens it.
* A convention that works well: one folder per section, one folder per screen, and a Java package
  `com.example.app.web.<section>.<screen>` split into `am` (application modules), `vo` (views and rows) and `lov`
  (list classes). Code shared by a section goes in `web.<section>.shared`, code shared by everything in `web.shared`.
* Java classes under a package listed in `APPLICATION_PACKAGES` are loaded from `bin/` and can be replaced while the JVM
  runs (see [running.md](running.md#changing-code-while-it-runs)).
* `$this->getDir()` is the folder of the screen file relative to the document root (`applications/customers/search`).
  Assets that several screens share are easier to keep at a fixed path from the document root.

## 1. A static screen

`applications/hello/hello.php`:

```php
<?php
$this->setTitle('Hello');                 // browser title; set on the screen the URL opens
$title = new TextField();
$title->setValue('Hello, world');
$this->add($title);

$button = new Button();
$button->setText('Say hi');
$button->setActionE(function () { Response::info('Hi'); });   // a message box
$this->add($button);
?>
```

* `$this` is the `Application` being built. Add components with `add()`; it returns the component it was given, so
  `$this->add(new Group())->addClass('toolbar')` works.
* `setValue('text')` shows a constant. `new Label('text')` is the plain caption component. The constructor needs that
  text. A label has no `setValueE`; a method it does not have is forwarded to the application and does not change the
  caption. A note whose text changes is a `TextField`.
* Open it with `index.php?hello/hello`. A plain reload of the page keeps the screen that is in the session. Append `!`
  (`index.php?hello/hello!`) to build it again from the file.

### Look: a stylesheet and classes

* `$this->setCss('applications/app.css')` on the screen the URL opens links one stylesheet after `skylin/skylin.css`, so
  its rules win at equal specificity. The path is relative to the document root. The file's modification time is added
  as `?v=`, so an edited file is picked up on the next page load. `setIcon`, `setTitle` and `setHead` work the same way.
  The first application rendered on the page decides; later ones do not replace the values.
* `addClass('name')` adds a class next to the framework's own class (`group`, `label`, `RoTextField`, `button`, ...).
  Anything but letters, digits, `_`, `-` and spaces is dropped. Use classes for the look and keep `setStyle` for layout
  that belongs to one screen.
* An inline `style` cannot be overridden from a stylesheet without `!important`. Several components write inline
  styles of their own (for example the tab panels of `TabStrip` have `overflow:hidden`, and table cells have their
  pixel widths).
* `Label` and read-only `TextField` render `div.label` / `div.RoTextField`: `font-size: 0.7em`, no wrapping, ellipsis.

### A fixed bar and a scrolling body

A screen whose top bar must stay visible while the rest scrolls needs a height chain from the container down to the
body. With your own classes:

```css
.screen-root { display: flex; flex-direction: column; height: 100%; overflow: hidden; }
.screen-root > .screen-root { flex: 1 1 0; min-height: 0; }
.screen-bar  { flex: none; }
.screen-body { flex: 1 1 0; min-height: 0; overflow: auto; }
```

```php
$this->addClass('screen-root');                 // the application's own div
$page = $this->add(new Group())->setId('page');
$page->addClass('screen-root');
$bar = $page->add(new Group())->addClass('screen-bar');
$body = $page->add(new Group())->addClass('screen-body');
```

* The container of the application must have a definite height (for example an absolutely positioned tab panel).
* An application that has another application pushed on top of it renders a wrapper div with its own classes
  (`Application::render`), so `screen-root` on the application below keeps the chain intact.
* When both the bar and the body need the same row, put them in a `Group` with `noTag()` (no div of its own) and
  `setPropE('row', ...)`, so they stay direct flex children of the page.

## 2. A screen with data

### Java: a view and an application module

```java
package com.example.app.web.customers.search.vo;

import com.distantmelody.framework.skylin.AM;
import com.distantmelody.framework.skylin.View;

public class CustomerList extends View
{
    public CustomerList(AM am)
    {
        super(am);
        setQuery("select customer_id, name, city, created_at from customers"
            + " where (:name is null or name like :name)"
            + " order by name limit :max_rows");
        setRowType(CustomerRow.class);           // optional: a ViewRow subclass with behaviour
    }
}
```

```java
package com.example.app.web.customers.search.am;

import com.distantmelody.framework.skylin.AM;
import com.example.app.web.customers.search.vo.CustomerList;

public class SearchAM extends AM
{
    public SearchAM()
    {
        new CustomerList(this);                  // views are created in the constructor
    }

    public void search(String name)
    {
        CustomerList list = (CustomerList) getView("CustomerList");
        list.setBindVar("name", String.class, name == null ? null : "%" + name + "%");
        list.setBindVar("maxRows", Integer.class, 500);
        list.executeQuery();
    }
}
```

* Binds are `:snake_case` in the query and camelCase in `setBindVar` (`:max_rows` is `maxRows`). A bind used twice is
  set by one `setBindVar` call. `setBindVar` throws when the view has no bind of that name.
* Columns become camelCase too: `customer_id` is `customerId`, `created_at` is `createdAt`.
* The view's name is the class's simple name: `getView("CustomerList")`.
* When `setDataSource` runs (from `addAM` in PHP), every view generates its column metadata and its `init()` runs, then
  the AM's `init()`. A query does not run by itself: call `executeQuery()` where it makes sense (in `init()` for a list
  that shows at once, or from a search method).
* The SQL dialect is the database's. Skylin passes the query to JDBC after replacing the binds with `?`.

### PHP: attach the AM and show the rows

`applications/customers/search/search.php`:

```php
<?php
$this->addAM('am', 'com.example.app.web.customers.search.am.SearchAM', 'main');   // 'main' is a datasource name

$list = $this->add(new Iter());
$list->setId('results');
$list->setViewE(function () { return $this->a()->getAM('am')->getView('CustomerList'); });

$line = $list->add(new Group());
$line->setStyle('display:flex;gap:8px');
$line->add(new TextField('name'));          // bound to the column "name" of the Iter's row
$line->add(new TextField('city'));
?>
```

* `new TextField('col')` reads `$this->r()->getValue('col')`. It is editable when the row says the column is updateable
  (`ViewRow.isUpdateable`), and its input type comes from the row (`getHtmlInputType`: `number` for a number column).
* `Iter` renders its children once per row, with that row as the context row. `setViewE` returns a view, `setRowsE` a
  list of rows, `setView` / `setRows` take them directly.
* `getAM()` with no name is the first AM the screen added.
* The datasource name is one registered at startup (see [configuration.md](configuration.md#datasources)).

### Handlers

Handlers are stored as code strings and run with `eval`, with `$this` being the component they are on. Every setter whose
name ends in `E` takes a string or a closure; a closure is turned into the text of its body at once
(`skylin/Expression.php`).

```php
$button->setActionE(function () {
    $this->a()->getAM('am')->search((string) $this->r()->getValue('name'));
    $this->com('results')->refresh();
});
// the same handler as a string:
$button->setActionE('$this->a()->getAM("am")->search((string) $this->r()->getValue("name")); $this->com("results")->refresh();');
```

Here the button sits on a form row (see section 3), so `$this->r()` is that row and `name` is what the user typed.

The rules:

* **A handler is static code.** Never build a handler string from request input, database values or other runtime data
  (no `'... "' . $id . '" ...'`). Read data when the handler runs: `$this->r()`, `$this->c('name')`, the AM.
  Assembling a string from developer constants (a column name, a method name) while the screen is built is fine.
* **A closure may use** `$this`, superglobals and variables it assigns itself. It may not have parameters, `use (...)`,
  static variables, `fn`, nested closures or functions, `__DIR__` / `__FILE__` / `__LINE__` and similar, variable
  variables, or variables of the surrounding code. Only one closure per line. A refused closure throws an exception
  naming the file, the line and the reason.
* **Inside a handler:** `$this->a()` is the application, `$this->r()` the context row, `$this->c('name')` a context value
  (a property set with `setProp` on this component or an ancestor), `$this->s()` the security context,
  `$this->com('id')` a component of the same application. `com()` finds a component once it has been rendered.
* **Text setters wrap their argument in a string.** `Button::setText`, `Component::setTitle` (tooltip),
  `SearchLov::setTextStyle` and `Button::setImage` build `return "<argument>";`, and `TextField::setEditable`,
  `setMinDec`, `File::setUploadRendered` and `Splitter::setClosed` build `return <argument>;`. Pass developer constants
  only (no `"` or `$`), or use the `E` variant.
* `Application::register('name', 'code')` adds a function to an application, called as `$app->name(...)`. Its code is
  always a string, because it reads the call's arguments from `$arg`, which a closure cannot do.
* Named functions do not belong in a screen file: the file runs once per screen instance, and a second instance would
  declare the function again. Put logic in Java or in a PHP class.

### Reaching a view by name

A view of the screen's first AM reads as a property: `$this->CustomerList` in a handler, or
`$this->getAM()->CustomerList` in the screen file.

1. `Component::__get($name)` and `Application::__get($name)` forward to the first AM.
2. The AM is a `java` proxy. A property read sends a call to the method `name` with no arguments.
3. `AM` has no method `CustomerList`, so the JVM asks `AM.handleVirtualMethodCall`, which returns the view with that name.

The same fallback works further down: `$view->name` is the column `name` of the current row, `$row->name` is a column
of the row, and `$am->role()->ADMIN` asks `userInRole("ADMIN")`. A public method with the same name wins, and a name that
matches nothing logs `could not find method` and returns null. A second AM is reached by name:
`$this->a()->getAM('other')->SomeView`.

### A view whose rows are built in code

A view does not need a query. Declare the columns and create the rows:

```java
public class StatusList extends View
{
    public StatusList(AM am)
    {
        super(am);
        setColumnNames("code", "label");
        setColumnTypes(String.class, String.class);
    }

    @Override
    public void init()
    {
        add("A", "Active");
        add("S", "Suspended");
    }

    private void add(String code, String label)
    {
        ViewRow row = newRow();
        row.setValue("code", code);
        row.setValue("label", label);
    }
}
```

* Fill the rows from a method on the AM (`clear()`, then `newRow()` and `setValue` for each), or in an `executeQuery()`
  override when the rows depend on the owner row of a linked view (`getLinkedView` calls `executeQuery()` when it creates
  the view).
* With no query, `executeQuery()` returns before it clears anything, so rows built in code stay. Do not add an empty
  `executeQuery()` override for that.
* `setColumnTypes` takes `View.string` (`String`), `View.number` (`BigDecimal`), `View.date` (`Timestamp`) or any other
  class.
* `getValue` / `setValue` are the normal accessors. `getValueInternal` / `setValueInternal` bypass an override of
  `getValue` / `setValue` in a row class.
* A query writes each column straight into the row. It does not call `setValue`. An override of `setValue` runs for a
  user edit and for a Java call, and does not run when the row is loaded. On an `EditViewRow`, `setValueInternal`
  returns without marking the column when the new value equals the current one, and code after `super.setValue` still
  runs. Read the previous value before `super.setValue`.

## 3. Search form and results table

The pattern for "criteria on top, results below":

* A **form view** with columns and no query holds the criteria: one row, created in its `init()`. Bind the fields to
  it with `new TextField('col')`, `new Date('col')`, `new ChoiceLov('col')`, `new SearchLov('col')`. `setProp('row', $row)`
  keeps that object. When the view is cleared and filled again, bind with `setPropE('row', ...)` returning `getFirst()`,
  on a `Group` with `noTag()` when several parts of the page share the row.
* A **list view** whose query has optional binds: `(:x is null or t.col = :x)`.
* The search button calls a method that validates the criteria, sets the binds and runs the query, then refreshes the
  table.

```java
public class SearchForm extends View
{
    public SearchForm(AM am)
    {
        super(am);
        setColumnNames("name", "city", "createdFrom");
        setColumnTypes(View.string, View.string, View.date);
        setLov("city", CityLov.class);           // see "Lists" below
    }

    @Override
    public void init()
    {
        newRow();
    }
}
```

```php
<?php
$this->addAM('am', 'com.example.app.web.customers.search.am.SearchAM', 'main');

$form = $this->add(new Grid(2, 3));              // 2 columns, 3 rows, filled in the order children are added
$form->setProp('row', $this->getAM('am')->getView('SearchForm')->getFirst());
$form->setColStyle(0, 'padding-right:8px');
$form->add(new Label('Name'));
$form->add(new TextField('name'));
$form->add(new Label('City'));
$form->add(new ChoiceLov('city'));
$form->add(new Label('Created from'));
$form->add(new Date('createdFrom'));

$search = $this->add(new Button());
$search->setText('Search');
$search->setActionE(function () {
    $this->a()->getAM('am')->search();       // reads the criteria from the form row in Java
    $this->com('results')->refresh();
});

$table = $this->add(new Table());
$table->setId('results');
$table->setViewE(function () { return $this->a()->getAM('am')->getView('CustomerList'); });
$table->setContent('name', new TextField('name'), 200, new Label('Name'));
$table->setContent('city', new TextField('city'), 120, new Label('City'));
$table->setContent('createdAt', new Date('createdAt'), 110, new Label('Created'));
$table->setPageSize(25);
?>
```

* `Table::setContent(name, component, widthInPixels, headerComponent)` adds a column. Each column has one component,
  rendered once per row.
* Header sorting works for a column whose cell is bound to a view column (`new TextField('col')`, `Date`, `ChoiceLov`);
  for any other cell use `setSortOn(name, viewColumn)`. `View.setSortableColumns(...)` limits the sortable columns, and
  with no arguments makes none sortable. Number columns sort numerically, every other type by its text.
* A click on a row makes it the view's current row. `afterRowSelectE` runs after that.
  `setEnableUserRowSelectionE('return false;')` turns selection off.
* Report a problem from Java with `info(message)`, `warn`, `error` or `fatal` on the AM. The message box appears when
  the request ends.

## 4. Lists: ChoiceLov and SearchLov

`ChoiceLov` (a drop-down) and `SearchLov` (a key field with a popup) read a linked view of the row named `<column>_lov`.

```java
package com.example.app.web.customers.search.lov;

import com.distantmelody.framework.skylin.AM;
import com.distantmelody.framework.skylin.lov.Lov;

public class CityLov extends Lov
{
    public CityLov(AM am)
    {
        super(am);
        setQuery("select city, country_name as city_country from cities order by city");
        setDisplayColumns(new String[]{"city", "cityCountry"}, new String[]{"City", "Country"}, new int[]{150, 150});
        setUserFilterColumns(new String[]{"city"}, new String[]{"City"});
    }
}
```

* `View.setLov("city", CityLov.class)` on the form view registers the list for the column `city`.
* The list's key column has the same name as the form column (`city`). Choosing a row stores that key.
* **Choosing a row also copies every other column the two share.** `Lov.selectRowSetOtherValues` copies each column of
  the chosen list row onto the form row where the names match. Use it for a description beside an id. Name such columns
  for the list (`cityCountry`), not something generic like `description`, so two lists on one row do not write into
  the same column. Clearing the key clears the shared columns.
* `ChoiceLov` option text is the display columns joined by the separator (a space; `setSeperator` changes it).
* `SearchLov` shows the key in a text field with a search button. `setUserFilterColumns` adds filter boxes above the
  popup rows; typing there filters in the browser. In-list validation is on by default
  (`Lov.doInListValidation()`): a typed value that is not in the list is refused with "Value not in list", and an empty
  value is kept. Turn it off per field with `setInListValidationE('return false;')`.
* `setAfterLovSelectE(...)` is `afterValueChangeE`: typing a key and leaving the field runs the same path as a click in
  the popup. It returns nothing, so do not chain after it.
* The popup redraws only the `SearchLov`. Refresh the description field yourself:

  ```php
  $cell = $form->add(new Group())->setStyle('display:flex;gap:8px');
  $lov = $cell->add(new SearchLov('city'));
  $lov->setAfterLovSelectE(function () { $this->com('cityCountry')->refreshInRow($this->r()); });
  $cell->add(new TextField('cityCountry'))->setId('cityCountry');
  ```

* **Binds of a list follow the owner row.** A bind of the list's query is linked to the owner row's column of the
  same name (`:country_code` follows `countryCode`), so each row of a table can have its own list. The list runs again
  when a linked value changes.

## 5. Editing: rules, errors and save

### Where values go

`ViewRow.setValueFromFrontEnd` stores what the user typed:

1. An empty string becomes null.
2. A number column (`BigDecimal`) is parsed first. Text that is not a number is refused with "Invalid number" and the row
   keeps its old value. `1e2` is a number (100); `1.5.2` and `12abc` are not.
3. `validate(name, value, false)` runs. A `validationError` there refuses the value.
4. The value is stored, then `validate(name, value, true)` runs. A `validationError` there is shown on the field and the
   value stays.

Put business rules in the second call, so a bad but storable value is still on the page when it is drawn again. Use the
first call only for what the column cannot hold.

`Date` parses `dd-MMM-yyyy` itself; a value it cannot parse is refused with "Invalid Date Format" and never reaches the
row.

### Rules in the row

```java
public class CustomerRow extends EditViewRow
{
    @Override
    public void validate(String name, Object value, boolean serverValue)
    {
        if (serverValue && name.equals("email") && value != null && !value.toString().contains("@"))
        {
            validationError("Enter a valid e-mail address");
        }
    }
}
```

* `validationError(sentence)` throws `ValidationException`. Read the sentence back with
  `getCurrentValueValidation(column)`, which runs `validate` on the stored value and returns the sentence or null.
* `EditViewRow.concatValidationErrors(separator)` joins the sentences of the editable columns.
* `View.setEditableColumns(...)` and `setNotEditableColumns(...)` decide which columns are updateable.
  `setReadOnly(true)` makes every column read-only.

### The `validate()` idiom in PHP

```php
$save->setActionE(function () {
    if (!$this->a()->getAM('am')->save())
    {
        validate();
    }
    $this->com('page')->refresh();
});
```

* `validate()` (defined in `skylin/InitRequest.php`) sets a flag for the current request. While it is set, every editable
  `TextField` (single or multi-line) and `Date` that renders asks its row for `getCurrentValueValidation(col)` and, when
  there is a sentence, draws a red frame with the sentence as tooltip (`TextField_tip error`, `data-tip`).
* So a failed save just draws the page again. The typed values come from the row, the refused fields carry their error,
  and a message line can show the joined sentences. `ChoiceLov` and `Checkbox` draw no error mark; show their problems
  in a message line.
* `save()` here is a method of your AM. A typical one checks `concatValidationErrors` on the row, writes with its own SQL
  or with `EditView.save()`, and returns false when something failed.

### EditView

* An `EditView` records changes. `EditViewRow.setValueInternal` ignores a value equal to the stored one. A different
  value records the column (`isModified(col)`) and calls `markModified()`, which puts the row on `getModifiedRows()` and
  marks the owner row of a linked view too. Rows read by a query start unmodified.
* Modes: `viewMode()`, `editMode()` (the default) and `addMode()`. In view mode nothing is updateable. In add mode a row
  whose id column (`setRowIdCol`, default `id`) has a value is read-only. For a linked `EditView`, the mode of the top
  `EditView` in the owner chain applies.
* `EditView.save()` calls stored procedures: `updateProc("save_customer", "customerId", "name", "#user", "#error")` for
  each modified row and `deleteProc(...)` for each deleted one. An argument is a column name, or `#user` (the user
  name), or an out parameter `#info`, `#warn`, `#error` (shown as a message) or `#out` (`popOut()`). It returns 0 on
  success and -1 on an error. A successful save commits and clears the record of changes.
* `clearChanges()`, `executeQuery()` and `clear()` also clear the record of changes.

### A message line

A message line is an ordinary one-row view that the AM owns. This one is application code, not part of Skylin:

```java
public class MessageView extends View
{
    public MessageView(AM am)
    {
        super(am);
        setColumnNames("message", "messageIsError");
        setColumnTypes(String.class, Boolean.class);
        setReadOnly(true);
    }

    @Override
    public void init()
    {
        newRow();
        setMessage("", false);
    }

    public String getMessage()
    {
        Object value = getFirst().getValue("message");
        return value == null ? "" : value.toString();
    }

    public void setMessage(String text, boolean isError)
    {
        getFirst().setValue("message", text == null ? "" : text);
        getFirst().setValue("messageIsError", Boolean.valueOf(isError));
    }
}
```

In PHP, bind a field to its row (`setProp('row', $this->MessageView->getFirst())` and `new TextField('message')`).
`setReadOnly(true)` only changes `isUpdateable`; `setValue` from Java still writes the row.

### Permissions

* Check the view permission on the first line of the screen file and stop there:

  ```php
  if (!$this->s()->userInRole('CUSTOMER_VIEW')) { $this->add(new Label('Insufficient privilege')); return; }
  ```

* A button that needs more than the screen's own permission stays visible and is disabled:
  `$save->setEditableE(function () { return $this->a()->getAM('am')->userInRole('CUSTOMER_CHANGE'); });`.
  `Button` renders `disabled`, and `Button::click` refuses the click on the server while the expression is false or the
  button is not rendered.
* A Java method that something other than a guarded button can reach checks the permission itself.

## 6. Opening other screens

### In a tab

A portal screen holds a `TabStrip` and registers a function that other screens call:

```php
// the portal (applications/home/home.php)
$this->setProp('home', $this);                 // every component below the portal can reach it as $this->c('home')
$tabs = $this->add(new TabStrip());
$tabs->setId('tabs');
$tabs->setTab('Start', $this->newChildApplication('home/start'));
$tabs->setCloseable('Start', false);

$this->register('openApplication', '$app = $this->newChildApplication($arg[1]);
    for ($i = 2; $i < sizeof($arg); $i = $i + 2)
    {
        $app->setProp($arg[$i], $arg[$i + 1]);
    }
    $this->com("tabs")->setTab($arg[0], $app);');
```

```php
// any screen inside the portal: open a record in its own tab
$link->setActionE(function () {
    $id = (string) $this->r()->getValue('customerId');
    $this->c('home')->openApplication('Customer ' . $id, 'customers/detail/detail', 'customerId', $id);
});
```

```php
// the opened screen (applications/customers/detail/detail.php)
$this->addAM('am', 'com.example.app.web.customers.detail.am.DetailAM', 'main');
$state = (string) $this->getAM('am')->load(trim((string) $this->c('customerId')));
if ($state !== 'OK') { $this->add(new Label('Customer not found')); return; }
```

* `TabStrip` identifies tabs by heading. `setTab` with a heading that is already open replaces that tab's content and
  selects it. Put the record id in the heading to get one tab per record.
* Properties arrive as strings. Validate them in Java and check the permission on the opened screen too.
* The screen file runs once, when the instance is created, so what it reads is a snapshot of that moment.
* `getTopApplication()` compares applications with `==`, which compares objects property by property. A context key such
  as `home` is cheaper and reaches the portal from any depth.

### On top of the current screen (push / pop)

Use push when the second screen is a mode of the first: it covers the screen that opened it and gives it back.

```php
// in the screen below
$button->setActionE(function () {
    $app = $this->a()->newChildApplication('customers/detail/addresses');
    $app->setProp('customerId', (string) $this->r()->getValue('customerId'));
    $app->push($this->a());                  // push is called on the NEW application
});

// in the Back button of the pushed screen
$back->setActionE(function () {
    $this->a()->getBelow()->getAM('am')->reload();   // optional: let the screen below read again
    $this->a()->pop();                               // the screen below is drawn again, this one is destroyed
});
```

* The pushed screen has its own file, AM and components. The one below is not drawn while it is covered and keeps its
  state. `pop()` draws it again from that state, so anything it must read again is read before the pop.

### A dialog over the screen (ModalPanel)

```php
$dialog = $page->add(new ModalPanel());
$dialog->setId('editDialog');
$dialog->addClass('edit-dialog');            // on the inner div (id ..._group); the outer div is .modalDialog
$dialog->setProp('row', ...);
// ... title, fields, a Save and a Close button

$open->setActionE(function () { $this->a()->getAM('am')->prepareEdit(); $this->com('editDialog')->refreshAndOpen(); });
$close->setActionE(function () { $this->com('editDialog')->close(); });
```

* `open()`, `close()`, `refreshAndOpen()`. A refresh of the page (or of the panel itself) renders it closed; call
  `open()` after the refresh to keep it open. `startOpen()` renders it open the first time.
* Save follows the `validate()` idiom: on failure `validate()` and `refreshAndOpen()`; on success `close()` and refresh
  the page.

## 7. Rows in tables and lists

### A different control for each row

A table column has one component. When the control depends on the row, the cell is a `Group` that holds one component
per kind, each rendered only on the rows it applies to:

```php
$value = new Group();
$choice = $value->add(new ChoiceLov('settingValue'));
$choice->setRenderedE(function () { return $this->r()->isChoice(); });
$number = $value->add(new TextField('settingValue'));
$number->setRenderedE(function () { return $this->r()->isNumber(); });
$table->setContent('value', $value, 300, new Label('Value'));
```

* Several components can be bound to the same column; only the rendered one is on the page for a row. A component that
  is not rendered leaves an empty hidden `div`.
* The row class answers the questions (`isChoice()`), so the screen does not need to know the kinds.
* `getHtmlInputType(col)` answers `number` for a `View.number` column and `text` otherwise, so the same `TextField` is
  a number field on one row and a text field on another when the row class overrides it.

### Moving a row to another view

Move a row with `ViewRow.transfer(View to)`. It keeps every value and linked view, removes the row from its old view,
gives it the next link id of the new view, adds it at the end, and resets the cached ids of the row and the rows under
it. Assigning the row to another list by hand leaves references and the old link id behind, and a later click looks the
row up by that link id.

* `row.moveTo(otherRow)` puts it before another row of the same view.
* `view.setCurrentRow(row)` selects it (a `Table` draws the current row as selected); `setCurrentRow(-1)` clears it.
* `$this->com('enabledSelect')->focusInRow($row)` focuses the control of one row and scrolls it into view.

### A list nested inside itself

One `Iter` can draw every level of a tree. The row template contains an `Iter` that renders the same template for the
row's children:

```php
$roots = $this->add(new Iter());
$roots->setId('treeRoots');
$roots->setRowsE(function () { return $this->a()->getAM('am')->getView('Folders')->getRows(); });

$node = new Group();
$node->setId('treeNode');
$node->add(new TextField('name'));
$kids = $node->add(new Iter());
$kids->setId('treeKids');
$kids->setRowsE(function () { return $this->r()->visibleChildren(); });   // a method of the row class
$kids->add($node);                                                          // the same template again
$roots->add($node);
```

* An empty child list stops the nesting.
* `refreshInRow($row)` works at any depth, because the element id contains the row's link id chain. Refresh the node
  (`$this->com('treeNode')->refreshInRow($row)`), not its parts separately.
* A linked view gives each row its own child list: `getLinkedView(ChildView.class, "folderId", "parentId")` in a row
  method passes this row's `folderId` to the child view's bind `parentId`. With an odd number of arguments the first one
  is an instance name.

## 8. Client-side behaviour with Template

Normal components talk to the server for every click. For behaviour that happens in the browser alone (expand and
collapse, switching inner tabs), use a `Template`: an HTML file with binding points plus your own `<style>` and
`<script>`. Components render into the binding points, so data and security stay on the server.

```php
$page = $this->add(new Template($this->getDir() . '/template/list.html'));

$rows = $page->setCom('rows', new Iter());           // <skylin_rows> in list.html
$rows->noTag();
$rows->setViewE(function () { return $this->a()->getAM('am')->getView('OrderList'); });

$item = $rows->add(new Template($this->getDir() . '/template/item.html'));
$item->setCom('title', new TextField('orderNumber'));
$item->setStringE('open', function () { return $this->r()->getValue('isLatest') === 'Y' ? ' open' : ''; });
$lines = $item->setCom('lines', new Iter());
$lines->noTag()->setViewE(function () { return $this->r()->getLines(); });
$lines->add(new TextField('productName'));
```

`list.html`:

```html
<div id="<skylin_comId>" class="order-list">
<style>.order-list .item .body { display: none } .order-list .item.open .body { display: block }</style>
<skylin_rows>
<script>
(function () {
	var root = document.getElementById('<skylin_comId>');
	root.addEventListener('click', function (e) {
		var head = e.target.closest('.head');
		if (!head || !root.contains(head)) { return; }
		head.closest('.item').classList.toggle('open');
	});
})();
</script>
</div>
```

`item.html`: `<div class="item<skylin_open>"><div class="head"><skylin_title></div><div class="body"><skylin_lines></div></div>`

* The path is relative to the document root (plus `SKYLIN_ROOT`). The file is read when the template is created and
  kept in the session with the component.
* `<skylin_name>` is a binding point (case sensitive). `setCom(name, component)` renders a component there and returns
  it. `setString(name, text)` and `setStringE(name, code)` insert text. A template has no `add()`.
* `<skylin_comId>` (unique per template instance, row and application) and `<skylin_amId>` are built in. The template
  adds no id of its own: put `id="<skylin_comId>"` on the root element.
* Binding points are replaced with `str_replace` and nothing is escaped. Show data through components (a `TextField`
  escapes it); keep the string bindings for state the browser needs before any click, such as an initial class.
* Inline `<script>` and `<style>` run and apply when the HTML is inserted.
* `setEventE('name', code)` plus the JavaScript call `templateEvent('name', '<skylin_comId>', data)` sends an event to
  the server. In the handler, `$this->c('eventData')` is the data.
* Keep state in CSS classes, render the initial state on the server, and use one delegated listener on the root
  element, so several copies of the template (several tabs, rows) never interfere.

## 9. Details that matter

* **Bind variables only.** Never concatenate user input into SQL. A bind name ends at a space, `)`, `,`, `|`, a tab or a
  line break, so write `:x)` or `:x ` and not `:x;`. A colon outside single quotes always starts a bind name, so write
  casts as `cast(x as text)`, not `x::text`.
* **`LIMIT :max_rows` and `OFFSET :n` work.** Column generation probes the query with `limit 1` / `offset 0` and every
  other `?` replaced by null.
* **Types across the bridge.** Only `String`, `Integer`, `Double`/`Float`/`BigDecimal` and `Boolean` arrive in PHP as
  plain values. `Long`, `Timestamp` and other objects arrive as proxies. A `BigDecimal` arrives as a 64-bit float, which
  is exact for ordinary amounts and for integers up to 2^53.
* **Leave column types alone.** Select a number as a number: the field becomes a number field, right-aligned
  (`number-align`), and a null is drawn blank. Ids with leading zeros (codes, account numbers) stay text.
* **Dates.** Use a date column (`View.date`, a `java.sql.Timestamp`) with `new Date('col')`. JDBC `java.sql.Date`,
  `LocalDateTime` and `LocalDate` columns are mapped to `Timestamp`. The component shows and parses `dd-MMM-yyyy`.
* **Escaping.** `TextField` escapes its value (`htmlspecialchars`, `ENT_QUOTES | ENT_SUBSTITUTE`, UTF-8) wherever it
  writes it. `noEscape()` turns that off for a value that is HTML written by the developer. `Date`, `ChoiceLov` (option
  text and read-only label), `SearchLov` popup cells, `Label`, `Group::setText` headings, `Button` text and `Template`
  strings are written as they are: show data through a `TextField`.
* **Read-only fields.** `TextField::setValue` and `setValueE` set the editable expression to false, and `setValueE` also
  replaces the type expression (number or text, from the value). A link that is not editable renders disabled and
  ignores clicks, so a clickable link is `setValueE(...)`, then `setEditable('true')`, then `link()` and
  `setActionE(...)`.
* **Component ids** must not contain `x`, `X` or `$`. `setId('extras')` throws.
* **`Group::add` takes its argument by reference.** `add(new Label('a'))` works; PHP raises a notice, which
  `InitRequest.php`'s error level hides. `Grid::add` takes it by value.
* **`Grid(x, y)`** renders `x * y` cells and fills them in the order children are added. Missing children leave empty
  cells; children beyond `x * y` are not rendered.
* **An `Iter` with `noTag()`** has no element of its own, so refresh its parent, not the iterator.
* **Unicode.** Write text as it is, in UTF-8 files without a BOM. No `htmlentities`, no `utf8_encode`. Java
  `String.length()` counts UTF-16 code units (an emoji counts 2); PHP `strlen()` counts bytes.
* **Cost.** Every component asks the JVM for values when it renders, and each call is a round trip. A table with
  hundreds of rows and many columns renders noticeably slower than one page of 25.

## 10. Trying it out

* A PHP change needs no build: open a new instance of the screen (`index.php?path!`, or a new tab in a portal).
* A Java class under `APPLICATION_PACKAGES` is picked up when a new AM instance is created after the `.class` file in
  `bin/` changed. Instances that are already open keep their classes.
* A failed `eval` is written to the PHP error log and to SLF4J. `LOG_PATH` still copies standard output and standard error
  into `backend_out` / `backend_errors`; those files are the legacy copy.
* The endpoints are plain POSTs, so `curl` with a cookie jar can drive a screen: `change=<field id>` with
  `value=<text>` to `skylin/TextField.php`, `click=<button id>` to `skylin/Button.php`, and the other actions listed in
  [architecture.md](architecture.md#a-request-end-to-end). Component ids change when a component is drawn again; take
  them from the latest response.
