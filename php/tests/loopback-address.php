<?php
/*
 * Checks LoopbackAddress::is(), the address half of BackendEval.php's check (the other half is the JVM's token, see the
 * bridge tests). No dev stack is needed.
 *
 *   php php/tests/loopback-address.php
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

require __DIR__.'/../skylin/LoopbackAddress.php';

// ---- this machine
foreach (array('127.0.0.1', '127.0.0.2', '127.255.255.254', '::1', '0:0:0:0:0:0:0:1', '::ffff:127.0.0.1', '::FFFF:127.1.2.3') as $a)
{
	check("loopback $a", LoopbackAddress::is($a));
}

// ---- anything else
foreach (array(
	'10.0.0.1', '172.17.0.1', '192.168.1.10', '8.8.8.8', '0.0.0.0', '128.0.0.1', '126.255.255.255',
	'::', '::2', '::ffff:10.0.0.1', '::ffff:7f00:1:0', '2001:db8::1', 'fe80::1', '::127.0.0.1', '64:ff9b::7f00:1'
) as $a)
{
	check("not loopback $a", !LoopbackAddress::is($a));
}

// ---- not an address
foreach (array('', 'localhost', '127.0.0.1 ', ' 127.0.0.1', '127.0.0.1:80', '127.1', '127.0.0.1/8', '127.0.0.1, 10.0.0.1', "127.0.0.1\0", '[::1]', 'fe80::1%eth0') as $a)
{
	check('not an address '.json_encode($a), !LoopbackAddress::is($a));
}
check('null', !LoopbackAddress::is(null));
check('array', !LoopbackAddress::is(array('127.0.0.1')));
check('number', !LoopbackAddress::is(2130706433));

echo $failures == 0 ? "\nAll passed\n" : "\n$failures FAILED\n";
exit($failures == 0 ? 0 : 1);
