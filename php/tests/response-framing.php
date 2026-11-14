<?php
/*
 * Checks how Response builds the text the browser reads (count,length1,length2,...-message1message2...): the lengths must
 * be in the unit the browser's response() counts in, UTF-16 code units, so that text outside ASCII is cut in the right places.
 * No dev stack is needed.
 *
 *   php -c runtime\conf\php.ini framework\php\tests\response-framing.php
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

require __DIR__.'/../skylin/Response.php';

/**
 * What the browser does with the response (skylin-precontent.js, response()): the text arrives as UTF-8 and becomes a
 * JavaScript string, which is indexed in UTF-16 code units. Returns the messages, as UTF-8 again.
 */
function browserParse($bytes)
{
	$units = mb_convert_encoding($bytes, 'UTF-16LE', 'UTF-8');   // two bytes per code unit
	$text = array();
	for ($i = 0; $i < strlen($units); $i += 2) $text[] = substr($units, $i, 2);
	$s = implode('', array_map(fn($u) => $u, $text));
	// header: ASCII up to the first '-'
	$dash = strpos($bytes, '-');
	$header = explode(',', substr($bytes, 0, $dash));
	$count = (int) array_shift($header);
	$pos = 2 * ($dash + 1);   // the header is ASCII, so one unit per byte
	$messages = array();
	for ($m = 0; $m < $count; $m++)
	{
		$len = (int) $header[$m];
		$messages[] = mb_convert_encoding(substr($s, $pos, 2 * $len), 'UTF-8', 'UTF-16LE');
		$pos += 2 * $len;
	}
	return array($count, $messages, $pos === strlen($s));
}

$frame = new ReflectionMethod('Response', 'frame');

check('browserLength: ascii', Response::browserLength('abc') === 3);
check('browserLength: empty', Response::browserLength('') === 0);
check('browserLength: 2 byte char counts 1', Response::browserLength("caf\u{e9}") === 4);
check('browserLength: 3 byte char counts 1', Response::browserLength("\u{20ac}") === 1);
check('browserLength: cjk', Response::browserLength("\u{65e5}\u{672c}\u{8a9e}") === 3);
check('browserLength: 4 byte char (emoji) counts 2', Response::browserLength("\u{1F600}") === 2);

$sets = array(
	'ascii only' => array('first', 'second,with,commas', '3'),
	'2 byte' => array("caf\u{e9} cr\u{e8}me", "\u{fc}\u{f1}\u{df}", 'plain'),
	'3 byte' => array("\u{20ac}100", "\u{65e5}\u{672c}\u{8a9e}", "x"),
	'4 byte' => array("\u{1F600}", "a\u{1F4A9}b", "\u{1F600}\u{1F600}"),
	'mixed, with empty' => array('', "a\u{e9}\u{20ac}\u{1F600}b", '', "end,5-6"),
	'looks like a header' => array("12,3,4-xyz", "\u{e9}-\u{e9}", "9"),
	'markup' => array("<span class=\"a\">\u{e9}</span>", "x = 'y\u{20ac}';", "\n\r\n\t"),
	'big' => array(str_repeat("\u{20ac}ab\u{1F600}", 100000), 'tail'),
);
foreach ($sets as $name => $messages)
{
	$out = $frame->invoke(null, $messages);
	list($count, $parsed, $consumed) = browserParse($out);
	check("frame '$name': message count", $count === count($messages));
	check("frame '$name': every message arrives intact", $parsed === $messages);
	check("frame '$name': nothing left over or missing", $consumed);
}

// The case that was wrong when the lengths were byte lengths: the second message is cut short and the third is shifted.
$out = $frame->invoke(null, array("caf\u{e9}", "\u{20ac}\u{20ac}", 'ok'));
check('frame header is in UTF-16 units: 3,4,2,2-', strpos($out, '3,4,2,2-') === 0);

// Text that is not valid UTF-8 cannot arrive in the browser as it was counted: it is replaced before it is counted.
$out = $frame->invoke(null, array("bad\xff\xfe byte", "ok\u{e9}", "cut\xe2\x82"));
list($count, $parsed, $consumed) = browserParse($out);
check('invalid UTF-8: replaced, the frame stays consistent', $count === 3 && $consumed && $parsed[1] === "ok\u{e9}");
check('invalid UTF-8: every message is valid UTF-8 now', mb_check_encoding($parsed[0], 'UTF-8') && mb_check_encoding($parsed[2], 'UTF-8'));

echo $failures === 0 ? "\nall passed\n" : "\n$failures FAILED\n";
exit($failures === 0 ? 0 : 1);
