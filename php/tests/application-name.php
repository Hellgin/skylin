<?php
/*
 * Checks Application::applicationFile(): which application names from the URL open a screen file. index.php takes the
 * name from the raw query string (?customers/search) or, when the query string has '=', from $_GET
 * (?application=customers/search), so every name is tried both ways. No dev stack is needed.
 *
 *   php php/tests/application-name.php
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

require __DIR__.'/../skylin/Application.php';

// A document root with two screens, a folder without a screen, and files beside applications/ that must stay unreachable.
$root = sys_get_temp_dir().DIRECTORY_SEPARATOR.'skylin-application-name-'.getmypid();
mkdir("$root/applications/customers/search", 0777, true);
mkdir("$root/applications/empty", 0777, true);
mkdir("$root/applications2", 0777, true);
file_put_contents("$root/applications/top.php", '<?php');
file_put_contents("$root/applications/customers/search/search.php", '<?php');
file_put_contents("$root/secret.php", '<?php');
file_put_contents("$root/applications2/evil.php", '<?php');
file_put_contents("$root/applications/customers/notes.txt.php", '<?php');

/** The name index.php passes on for ?<query>: the raw query string, minus a trailing '!' (open fresh). */
function fromQueryString($query)
{
	if (strpos($query, '=') !== false)
	{
		parse_str($query, $get);
		return $get['application'] ?? null;
	}
	return substr($query, -1) == '!' ? substr($query, 0, -1) : $query;
}

/** True when the name opens the file, through both URL forms. */
function opens($name, $file)
{
	global $root;
	$expected = realpath("$root/applications/$file");
	$raw = Application::applicationFile($root, fromQueryString($name));
	$get = Application::applicationFile($root, fromQueryString('application='.rawurlencode($name)));
	return $expected !== false && $raw === $expected && $get === $expected;
}

/** True when neither URL form opens anything for the query (given exactly as it appears after '?'). */
function refused($query)
{
	global $root;
	$raw = Application::applicationFile($root, fromQueryString($query));
	$get = Application::applicationFile($root, fromQueryString('application='.$query));
	return $raw === null && $get === null;
}

// ---- screens open
check('top-level screen', opens('top', 'top.php'));
check('screen in a folder', opens('customers/search/search', 'customers/search/search.php'));
check('leading slash stays under applications/', opens('/customers/search/search', 'customers/search/search.php'));
check('trailing ! (open fresh) on the raw form', Application::applicationFile($root, fromQueryString('top!')) === realpath("$root/applications/top.php"));

// ---- what does not exist
check('missing screen', refused('missing'));
check('folder without a screen file', refused('empty'));
check('empty name', Application::applicationFile($root, '') === null);
check('no name', Application::applicationFile($root, null) === null);
check('array (?application[]=x)', Application::applicationFile($root, fromQueryString('application[]=top')) === null);

// ---- traversal, in every spelling that reaches index.php
check('../ to a file beside applications/', refused('../secret'));
check('../ to a sibling folder with the same prefix', refused('../applications2/evil'));
check('../ out of the document root', refused('../../../../etc/passwd'));
check('../ that comes back inside', refused('customers/../top'));
check('./ segment', refused('./top'));
check('%2e%2e%2f', refused('%2e%2e%2fsecret'));
check('%2E%2E/', refused('%2E%2E/secret'));
check('double encoded %252e%252e%252f', refused('%252e%252e%252fsecret'));
check('backslash ..\\', refused('..\\secret'));
check('encoded backslash ..%5c', refused('..%5csecret'));
check('absolute path', refused(str_replace('\\', '/', $root).'/secret'));
check('Windows drive path', refused('C:/Windows/win.ini'));
check('stream wrapper php://', refused('php://filter/resource=top'));
check('stream wrapper data:', refused('data://text/plain,x'));
check('NUL byte', refused('top%00'));
check('trailing newline', Application::applicationFile($root, "top\n") === null && refused('top%0a'));
check('space', refused('top%20'));
check('dot in the name', refused('customers/notes.txt'));
check('pearcmd technique', refused('../../../../usr/local/lib/php/pearcmd&+config-create+/x'));

// ---- a symlink inside applications/ that points outside it (skipped where symlinks cannot be made, e.g. Windows without the right)
if (@symlink("$root/secret.php", "$root/applications/linked.php"))
{
	check('symlink to a file outside applications/', refused('linked'));
}
else
{
	echo "SKIP  symlink to a file outside applications/ (cannot create symlinks here)\n";
}

// Clean up the temporary document root.
$it = new RecursiveIteratorIterator(new RecursiveDirectoryIterator($root, FilesystemIterator::SKIP_DOTS), RecursiveIteratorIterator::CHILD_FIRST);
foreach ($it as $f)
{
	$f->isDir() && !$f->isLink() ? rmdir($f->getPathname()) : unlink($f->getPathname());
}
rmdir($root);

echo $failures == 0 ? "\nAll passed\n" : "\n$failures FAILED\n";
exit($failures == 0 ? 0 : 1);
