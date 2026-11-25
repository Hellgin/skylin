<?php
/*
 * Checks the component features that need no backend: $this->name finding a component before the AM, setRendered,
 * TabStrip tabs that are not rendered, FileArea's markup and after-upload handler, finalInit running after the screen
 * file, and the setters that return the component for chaining. Java objects are stand-ins, so no dev stack is needed.
 *
 *   php php/tests/components.php
 *
 * Exit code 0 when everything passed.
 */
error_reporting(E_ERROR | E_PARSE);
$failures = 0;
function check($name, $ok)
{
	global $failures;
	echo ($ok ? 'PASS  ' : 'FAIL  ').$name."\n";
	if (!$ok) $failures++;
}

function java_is_null($value)
{
	return $value === null;
}

require __DIR__.'/../skylin/Expression.php';
require __DIR__.'/../skylin/Application.php';
require __DIR__.'/../skylin/TabStrip.php';
require __DIR__.'/../skylin/Splitter.php';
require __DIR__.'/../skylin/Table.php';
require __DIR__.'/../skylin/FileArea.php';

/** Stands in for a Java AM: a property is a view of that name, and finalInit is recorded. */
class StubAM
{
	public $finalInits = 0;
	function __get($name)
	{
		return 'view '.$name;
	}
	function finalInit()
	{
		$this->finalInits++;
		$GLOBALS['order'][] = 'finalInit';
	}
}

class StubText extends Component
{
	private $text;
	function __construct($text)
	{
		parent::__construct();
		$this->text = $text;
	}
	function render()
	{
		return $this->text;
	}
}

class StubRow
{
	private $values;
	function __construct($values)
	{
		$this->values = $values;
	}
	function getValue($col)
	{
		return $this->values[$col];
	}
}

class StubView
{
	private $rows;
	function __construct($rows)
	{
		$this->rows = $rows;
	}
	function getRows()
	{
		return $this->rows;
	}
	function _getJavaObjectId()
	{
		return 12;
	}
	public $grants = array();
	public $revoked = array();
	function allowFileUpload($owner, $nameColumn, $blobColumn)
	{
		$this->grants[] = array($owner, $nameColumn, $blobColumn);
		return 'token'.count($this->grants).'<"&>';
	}
	function revokeFileUpload($owner)
	{
		$this->revoked[] = $owner;
	}
}

/** A second Java object, with its own id. */
class OtherStubView extends StubView
{
	function _getJavaObjectId()
	{
		return 13;
	}
}

function setPrivate($object, $class, $property, $value)
{
	$p = new ReflectionProperty($class, $property);
	$p->setValue($object, $value);
}

function getPrivate($object, $class, $property)
{
	$p = new ReflectionProperty($class, $property);
	return $p->getValue($object);
}

/** An application as InitRequest would hold it, without the session setup of the constructor. */
function newApplication($am)
{
	$app = (new ReflectionClass('Application'))->newInstanceWithoutConstructor();
	setPrivate($app, 'Application', 'applicationId', 1);
	setPrivate($app, 'Application', 'applicationModules', array('am' => $am));
	setPrivate($app, 'Application', 'firstAm', $am);
	$app->setContextParam('application', $app);
	return $app;
}

$_SESSION['DEFAULT_ROW'] = new DefaultRow();
$am = new StubAM();
$app = newApplication($am);

// ---- $this->name: a component of the application first, then the first AM
$save = new StubText('save');
$app->setComponent('save', $save);
$other = new StubText('other');
$other->setContextParam('application', $app);
check('application: $this->id is the component', $app->save === $save);
check('component: $this->id is a component of its application', $other->save === $save);
check('application: a name that is no component is the AM\'s', $app->CustomerList === 'view CustomerList');
check('component: a name that is no component is the AM\'s', $other->CustomerList === 'view CustomerList');
$shadow = new StubText('shadow');
$app->setComponent('CustomerList', $shadow);
check('a component id hides a view of the same name', $other->CustomerList === $shadow);
check('the view stays reachable through getAM()', $other->getAM()->CustomerList === 'view CustomerList');

// ---- setRendered
$c = new StubText('c');
check('setRendered returns the component', $c->setRendered('false') === $c);
check('setRendered(\'false\') hides', !$c->getRendered());
check('setRendered(\'true\') shows', (bool)$c->setRendered('true')->getRendered());
check('setRendered(true) shows', (bool)$c->setRendered(true)->getRendered());
check('setRendered(false) hides', !$c->setRendered(false)->getRendered());

// ---- TabStrip: a tab that is not rendered is left out, and cannot be chosen
$tabs = new TabStrip();
$tabs->setTab('One', new StubText('content one'));
$tabs->setTab('Two', new StubText('content two'));
$tabs->setId('tabs');
$tabs->setContextParam('application', $app);
check('setTabRenderedE returns the strip', $tabs->setTabRenderedE('Two', 'return $this->c("showTwo");') === $tabs);
$tabs->setContextParam('showTwo', false);
$html = $tabs->render();
check('a hidden tab is not drawn', strpos($html, 'content one') !== false && strpos($html, 'content two') === false && strpos($html, 'Two') === false);
$tabs->setContextParam('showTwo', true);
$html = $tabs->render();
check('the tab is drawn once its expression is true', strpos($html, 'content one') !== false && strpos($html, 'content two') !== false);
$tabs->setContextParam('showTwo', false);
$tabs->changeTab('0');
$tabs->changeTab('1');
check('changeTab ignores a hidden tab', getPrivate($tabs, 'TabStrip', 'currentTab')->getHeading() === 'One');
$tabs->changeTab('9');
check('changeTab ignores a tab that does not exist', getPrivate($tabs, 'TabStrip', 'currentTab')->getHeading() === 'One');
$tabs->setContextParam('showTwo', true);
$tabs->changeTab('1');
check('changeTab goes to a shown tab', getPrivate($tabs, 'TabStrip', 'currentTab')->getHeading() === 'Two');

// ---- FileArea
$area = new FileArea('file', 'fileName');
$area->setId('area');
$area->setContextParam('application', $app);
$areaView = new StubView(array(new StubRow(array('fileName' => '<b>a&b.txt</b>')), new StubRow(array('fileName' => 'c.txt'))));
$area->setView($areaView);
$html = $area->render();
check('standalone: lists the file names, escaped', strpos($html, '&lt;b&gt;a&amp;b.txt&lt;/b&gt;') !== false && strpos($html, '<b>') === false && strpos($html, '>c.txt<') !== false);
check('standalone: class and view id', strpos($html, 'class="file_area file_area_standalone"') !== false && strpos($html, 'viewid="12"') !== false
	&& strpos($html, 'id="x1xareax0"') !== false);
check('rendering asks the view for an upload grant on its two columns, owned by the area', $areaView->grants === array(array('x1xareax0', 'fileName', 'file')));
check('the upload token is in the page, escaped', strpos($html, 'uploadtoken="token1&lt;&quot;&amp;&gt;"') !== false);
check('the column names are not in the page', strpos($html, 'namecol') === false && strpos($html, 'blobcol') === false);
$html = $area->render();
check('every render asks for a new grant and puts the new token in the page', count($areaView->grants) == 2 && strpos($html, 'uploadtoken="token2') !== false);
check('rendering the same view again revokes nothing (the new grant replaces the old)', $areaView->revoked === array());
$otherView = new OtherStubView(array());
$area->setView($otherView);
$area->render();
check('drawing another view revokes the grant on the previous one', $areaView->revoked === array('x1xareax0') && count($otherView->grants) == 1);
$area->setView(null);
$html = $area->render();
check('drawing no view revokes the grant too', $otherView->revoked === array('x1xareax0') && strpos($html, 'uploadtoken=""') !== false);
$area->setView($areaView);
$noView = new FileArea('file', 'fileName');
$noView->setId('noView');
$noView->setContextParam('application', $app);
$html = $noView->render();
check('no view: viewid and token are empty', strpos($html, 'viewid=""') !== false && strpos($html, 'uploadtoken=""') !== false);
$withChild = new FileArea('file', 'fileName');
$withChild->setId('withChild');
$withChild->setContextParam('application', $app);
$withChild->setView(new StubView(array(new StubRow(array('fileName' => 'a.txt')))));
check('setChild returns the child', $withChild->setChild(new StubText('the table')) instanceof StubText);
$html = $withChild->render();
check('with a child: draws the child, not the names', strpos($html, 'the table') !== false && strpos($html, 'a.txt') === false && strpos($html, 'file_area_standalone') === false);
$withViewE = new FileArea('file', 'fileName');
$withViewE->setId('withViewE');
$withViewE->setContextParam('application', $app);
$withViewE->setViewE('return new StubView(array(new StubRow(array("fileName" => "from viewE"))));');
check('setViewE gives the view', strpos($withViewE->render(), 'from viewE') !== false);
$GLOBALS['uploaded'] = null;
check('afterUploadE returns the area', $area->afterUploadE('$GLOBALS["uploaded"] = $this->getId();') === $area);
$area->upload('0');
check('upload runs afterUploadE', $GLOBALS['uploaded'] === 'area');

// ---- not editable or not rendered: no token in the page, and the area's grant is withdrawn
$gated = new FileArea('file', 'fileName');
$gated->setId('gated');
$gatedView = new StubView(array());
$gated->setView($gatedView);
$context = array('application' => $app, 'canUpload' => true);
check('setEditableE returns the area', $gated->setEditableE('return $this->c("canUpload");') === $gated);
$gated->renderInContext($context);
check('editable: a grant', count($gatedView->grants) == 1 && $gatedView->revoked === array());
$context['canUpload'] = false;
$html = $gated->renderInContext($context);
check('a render while not editable revokes the grant', $gatedView->revoked === array('x1xgatedx0'));
check('and asks for no new one', count($gatedView->grants) == 1);
check('and puts no token in the page, marked disabled', strpos($html, 'uploadtoken=""') !== false && strpos($html, 'file_area_disabled') !== false && strpos($html, 'viewid="12"') !== false);
$gated->renderInContext($context);
check('a second render while not editable has nothing left to revoke', $gatedView->revoked === array('x1xgatedx0'));
$context['canUpload'] = true;
$html = $gated->renderInContext($context);
check('editable again: a new grant and token', count($gatedView->grants) == 2 && strpos($html, 'uploadtoken="token2') !== false && strpos($html, 'file_area_disabled') === false);
$gated->setRendered('false');
$html = $gated->renderInContext($context);
check('a render while not rendered revokes the grant', $gatedView->revoked === array('x1xgatedx0', 'x1xgatedx0') && count($gatedView->grants) == 2);
check('and draws only the hidden placeholder', strpos($html, 'display:none') !== false && strpos($html, 'uploadtoken') === false);
$gated->renderInContext($context);
check('a second render while not rendered has nothing left to revoke', count($gatedView->revoked) == 2);
$gated->setRendered('true');
$gated->renderInContext($context);
check('rendered again: a new grant', count($gatedView->grants) == 3);
check('setEditable returns the area', $gated->setEditable('false') === $gated);
$html = $gated->renderInContext($context);
check('setEditable(\'false\') revokes and gives no token', count($gatedView->revoked) == 3 && strpos($html, 'uploadtoken=""') !== false);

$gated->afterUploadE('$GLOBALS["uploaded"] = $this->getId();');
$GLOBALS['uploaded'] = null;
$gated->upload('0');
check('afterUploadE does not run while not editable', $GLOBALS['uploaded'] === null);
$gated->setEditable('true')->setRendered('false');
$gated->upload('0');
check('afterUploadE does not run while not rendered', $GLOBALS['uploaded'] === null);
$gated->setRendered('true');
$gated->upload('0');
check('afterUploadE runs while editable and rendered', $GLOBALS['uploaded'] === 'gated');

// ---- finalInit runs once for every AM, after the screen file
$root = sys_get_temp_dir().DIRECTORY_SEPARATOR.'skylin-components-'.getmypid();
mkdir("$root/applications", 0777, true);
file_put_contents("$root/applications/screen.php", '<?php $GLOBALS["order"][] = "screen"; $this->applicationModules["second"] = $GLOBALS["secondAm"];');
$_SERVER['DOCUMENT_ROOT'] = $root;
$_SESSION['UTIL'] = new class { function getFrameworkParam($name) { return ''; } };
$GLOBALS['order'] = array();
$GLOBALS['secondAm'] = new StubAM();
$screen = (new ReflectionClass('Application'))->newInstanceWithoutConstructor();
setPrivate($screen, 'Application', 'applicationModules', array());
setPrivate($screen, 'Application', 'name', 'screen');
$screen->create();
check('finalInit runs after the screen file', $GLOBALS['order'] === array('screen', 'finalInit'));
check('finalInit runs once per AM', $GLOBALS['secondAm']->finalInits === 1);
unlink("$root/applications/screen.php");
rmdir("$root/applications");
rmdir($root);

// ---- setters that return the component
$splitter = new Splitter();
check('Splitter::setClosed returns the splitter', $splitter->setClosed('true') === $splitter);
check('Splitter::setClosedE returns the splitter', $splitter->setClosedE('return false;') === $splitter);
$table = new Table();
check('Table::setHeight returns the table', $table->setHeight(300) === $table);

echo $failures == 0 ? "\nAll passed\n" : "\n$failures FAILED\n";
exit($failures == 0 ? 0 : 1);
