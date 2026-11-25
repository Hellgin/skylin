<?php
/*
 * Checks that BridgeSessionHandler sends only session ids of 22 to 128 letters, digits and '-' to the JVM, where the id
 * becomes part of a file name. A stub stands in for the bridge connection, so no dev stack is needed.
 *
 *   php php/tests/session-id.php
 *
 * Exit code 0 when everything passed.
 */
ob_start();   // session_id() cannot be set once anything was sent; the report is printed at the end
error_reporting(E_ERROR | E_PARSE);
$failures = 0;
function check($name, $ok)
{
	global $failures;
	echo ($ok ? 'PASS  ' : 'FAIL  ').$name."\n";
	if (!$ok) $failures++;
}

require __DIR__.'/../skylin/bridge/BridgeSessionHandler.php';

class StubConnection
{
	public $calls = array();
	public $answer = 'true';
	public $answers = array();   // by command, e.g. 'b,2' => ''

	function jvmCall_internal($command)
	{
		$this->calls[] = $command;
		return $this->answers[substr($command, 0, 3)] ?? $this->answer;
	}
}

$valid = array(str_repeat('a', 22), str_repeat('Z', 128), str_repeat('-', 22), '0123456789abcdef0123456789abcdef',
	'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-');
$invalid = array('/../../victim.txt', '..\\..\\victim.txt', '../../../../../../windows/win.ini', 'short', str_repeat('a', 21),
	str_repeat('a', 129), '', 'with space-0123456789abcdef', 'with.dot-0123456789abcdef', 'with_underscore-0123456789ab',
	'comma,b,5,0123456789abcdef', str_repeat("\u{e9}", 22), "nul\0-0123456789abcdefghij", "newline-0123456789abcdef\n",
	"0123456789abcdef0123456789abcdef\n");

// ---- the pattern
foreach ($valid as $id)
{
	check('valid '.json_encode($id), BridgeSessionHandler::isValidId($id));
}
foreach ($invalid as $id)
{
	check('not valid '.json_encode($id), !BridgeSessionHandler::isValidId($id));
}
check('null is not valid', !BridgeSessionHandler::isValidId(null));
check('an array is not valid', !BridgeSessionHandler::isValidId(array(str_repeat('a', 22))));
check('a number is not valid', !BridgeSessionHandler::isValidId(12345678901234567890123));

// ---- nothing with an invalid id reaches the JVM
$con = new StubConnection();
$handler = new BridgeSessionHandler($con);
foreach ($invalid as $id)
{
	check('read refuses '.json_encode($id), $handler->read($id) === false);
	check('write refuses '.json_encode($id), $handler->write($id, 'overwritten') === false);
	check('destroy refuses '.json_encode($id), $handler->destroy($id) === false);
	check('validateId refuses '.json_encode($id), $handler->validateId($id) === false);
	check('updateTimestamp refuses '.json_encode($id), $handler->updateTimestamp($id, 'x') === false);
}
session_id('/../../victim.txt');
check('the test set the session id', session_id() === '/../../victim.txt');
check('close with an invalid id succeeds', $handler->close() === true);
check('and none of these called the JVM', $con->calls === array());
check('open with an invalid id sends the save path without the id (strict mode replaces the id next)',
	$handler->open('C:/sessions', 'PHPSESSID') === true && $con->calls === array('b,1,C:/sessions,,PHPSESSID'));

// ---- a valid id does
$id = '0123456789abcdef0123456789abcdef';
check('validateId asks the JVM', $handler->validateId($id) === true && end($con->calls) === 'b,7,'.$id);
$con->answer = 'false';
check('validateId is false when the JVM does not know the session', $handler->validateId($id) === false);
$con->answer = 'true';
check('write sends the id', $handler->write($id, 'data') === true && end($con->calls) === 'b,3,'.$id.',data');
session_id($id);
check('open sends the id', $handler->open('C:/sessions', 'PHPSESSID') === true && end($con->calls) === 'b,1,C:/sessions,'.$id.',PHPSESSID');

// ---- new ids
$ids = array();
for ($i = 0; $i < 100; $i++)
{
	$ids[] = $handler->create_sid();
}
check('create_sid makes 32 hex digits', count(array_filter($ids, function($s) { return preg_match('/^[0-9a-f]{32}$/D', $s) === 1; })) === 100);
check('which are valid ids', count(array_filter($ids, array('BridgeSessionHandler', 'isValidId'))) === 100);
check('and all different', count(array_unique($ids)) === 100);

// ---- session_start with a cookie id that is not valid, as InitRequest does it
$con = new StubConnection();
$con->answers = array('b,2' => '', 'b,7' => 'false');
ini_set('session.use_strict_mode', '1');
session_set_save_handler(new BridgeSessionHandler($con), true);
session_id('/../../victim-0123456789abcdef');
check('session_start', session_start());
$new = session_id();
check('the id is replaced by one from create_sid', preg_match('/^[0-9a-f]{32}$/D', $new) === 1);
session_write_close();
check('PHP opened without the id, read and wrote the new one, and sent nothing else',
	$con->calls === array('b,1,'.session_save_path().',,PHPSESSID', 'b,2,'.$new, 'b,3,'.$new.',', 'b,4,'.$new));

$old = 'stale-0123456789abcdef0123';
foreach (array('false' => 'replaced', 'true' => 'kept') as $known => $what)
{
	$con->calls = array();
	$con->answers['b,7'] = $known;
	session_id($old);
	session_start();
	$id = session_id();
	session_write_close();
	check("a valid id the JVM ".($known === 'true' ? 'holds' : 'does not hold')." is $what", ($id === $old) === ($known === 'true')
		&& array_slice($con->calls, 0, 3) === array('b,1,'.session_save_path().','.$old.',PHPSESSID', 'b,7,'.$old, 'b,2,'.$id));
}

ob_end_flush();
echo $failures == 0 ? "\nAll passed\n" : "\n$failures FAILED\n";
exit($failures == 0 ? 0 : 1);
