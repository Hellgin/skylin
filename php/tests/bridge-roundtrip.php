<?php
/*
 * End to end check of the PHP <-> JVM bridge with text outside ASCII, through the real PHP bridge code
 * (framework/php/skylin/bridge) and the running Skylin JVM. Needs the dev stack: dev\start-all.ps1 (only the backend is used).
 *
 *   php -c runtime\conf\php.ini framework\php\tests\bridge-roundtrip.php [session-directory]
 *
 * Uses JDK classes as the Java side (StringBuilder, Arrays), so nothing has to be deployed. The optional argument is the
 * directory of the PHP session files (runtime\sessions); then the stored file is compared with what PHP wrote.
 * Exit code 0 when everything passed.
 */
error_reporting(E_ERROR | E_PARSE);
ob_start();   // PHP refuses to start a session once anything was sent; the report is printed at the end
$failures = 0;
function check($name, $ok)
{
	global $failures;
	echo ($ok ? 'PASS  ' : 'FAIL  ').$name."\n";
	if (!$ok) $failures++;
}
function utf16Length($s)
{
	return intdiv(strlen(mb_convert_encoding($s, 'UTF-16LE', 'UTF-8')), 2);
}

require __DIR__.'/../skylin/bridge/BridgeConnection.php';
require __DIR__.'/../skylin/bridge/java.php';       // starts a session (php.ini's session.save_path) and registers it with the JVM
session_destroy();                                   // its file is not needed

$samples = array(
	'ascii' => 'plain ascii, with commas, 1,2,3 and s7',
	'2 byte' => "caf\u{e9} cr\u{e8}me \u{fc}\u{f1}\u{df}",
	'3 byte' => "\u{20ac}100 \u{2019}quoted\u{2019}",
	'cjk' => "\u{65e5}\u{672c}\u{8a9e}",
	'4 byte' => "\u{1F600}\u{1F4A9}",
	'mixed' => "a\u{e9}\u{20ac}\u{1F600}b,3,\u{65e5}",
	'empty' => '',
);

// --- strings as arguments and as results (instanceMethod, stringToObject, objectToString)
foreach ($samples as $name => $s)
{
	$sb = new java('java.lang.StringBuilder');
	$sb->append($s);
	check("string '$name' round trip", $sb->toString() === $s);
	check("string '$name': Java sees ".utf16Length($s).' UTF-16 units', $sb->length() === utf16Length($s));
}

// two arguments in one call: both must be cut by bytes
$sb = new java('java.lang.StringBuilder');
$sb->append("\u{1F600}\u{e9}");
$sb->append("\u{20ac},12,\u{65e5}");
check('two string arguments one after the other', $sb->toString() === "\u{1F600}\u{e9}\u{20ac},12,\u{65e5}");
$sb = new java('java.lang.StringBuilder');
$res = $sb->insert(0, "\u{e9}\u{1F600}");
check('int and string arguments in one call', $sb->toString() === "\u{e9}\u{1F600}");
$sb->insert(1, "\u{20ac}");
check('int and string arguments in one call (insert at 1)', $sb->toString() === "\u{e9}\u{20ac}\u{1F600}");

// --- arrays as arguments and as results
$arrays = java('java.util.Arrays');
$list = array_values($samples);
$back = $arrays->asList($list);
check('array of mixed strings as argument and result', $back === $list);
check('array with an empty string only', $arrays->asList(array('')) === array(''));
check('empty array', $arrays->asList(array()) === array());

// --- frame over 1 MB (PHP hands it over in a temp file) and a big reply
$unit = "\u{20ac}abc\r\n\u{1F600}\n";
$big = str_repeat($unit, 200000);        // 1.2 million characters, 2.4 MB
$sb = new java('java.lang.StringBuilder');
$sb->append($big);
$out = $sb->toString();
check('2.4 MB string, through the temp file and back ('.strlen($big).' bytes)', $out === $big);
check('2.4 MB string: length seen by Java', $sb->length() === utf16Length($big));
$bigList = array($big, "\u{e9}", $big);
check('big array', $arrays->asList($bigList) === $bigList);

// --- the session handler keeps session data byte for byte
require_once __DIR__.'/../skylin/bridge/BridgeSessionHandler.php';
session_write_close();
ini_set('session.use_strict_mode', '1');   // as BridgeSessionHandler::install
$handler = new BridgeSessionHandler(jvmGetBridgeConnection());
session_set_save_handler($handler, true);
mt_srand(7);
$binary = '';
for ($i = 0; $i < 600; $i++) $binary .= chr(mt_rand(0, 255));
$sessionStrings = array(
	'binary'   => "\xff\xfe\x00,\xc3\xa9\xe2\x82\xac\xf0\x9f\x98\x80,\x80\xc3",
	'random'   => $binary,
	'unicode'  => implode('', $samples),
	'bigunicode' => $big,
);
$asked = 'unicodetest'.bin2hex(random_bytes(6));
session_id($asked);
check('session_start with the bridge handler', session_start());
$sid = session_id();
check('an id the JVM does not know is replaced by a new one (validateId, create_sid)', $sid !== $asked && preg_match('/^[0-9a-f]{32}$/D', $sid) === 1);
foreach ($sessionStrings as $k => $v) $_SESSION[$k] = $v;
$_SESSION['nested'] = array('k,1' => "\u{e9},\u{20ac}", 7 => "\xf0\x9f");
$serialized = session_encode();   // what PHP writes, before the session is closed
session_write_close();
if (isset($argv[1]) && is_dir($argv[1]))//the directory of session.save_path of the running stack
{
	$file = rtrim($argv[1], '/\\').DIRECTORY_SEPARATOR.'sess1_'.$sid;
	check('session file holds exactly the serialized session ('.strlen($serialized).' bytes)', is_file($file) && file_get_contents($file) === $serialized);
}
// What PHP reads on the next request. (Not by starting the session again: opening a session id the JVM does not know
// deletes its file, because after a JVM restart that session's objects are gone.)
$stored = $handler->read($sid);
check('read() returns exactly what was written ('.strlen($stored).' bytes)', $stored === $serialized);
check('the stored text still holds every value (binary, random bytes, unicode, 2.4 MB, nested)',
	strpos($stored, serialize($sessionStrings['binary'])) !== false
	&& strpos($stored, serialize($sessionStrings['random'])) !== false
	&& strpos($stored, serialize($sessionStrings['bigunicode'])) !== false
	&& strpos($stored, serialize(array('k,1' => "\u{e9},\u{20ac}", 7 => "\xf0\x9f"))) !== false);
$handler->destroy($sid);
check('destroy() removes it', $handler->read($sid) === '');

ob_end_flush();
echo $failures === 0 ? "\nall passed\n" : "\n$failures FAILED\n";
exit($failures === 0 ? 0 : 1);
