<?php
/*
 * Checks Expression::toExpression(): strings pass through, a closure becomes the text of its body (which, eval()'d on an
 * object, does what the closure does there), and every closure that would mean something else once it is a string is refused.
 * No dev stack is needed.
 *
 *   php -c runtime\conf\php.ini framework\php\tests\expression.php
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

require __DIR__.'/../skylin/Expression.php';

/** True when converting the closure is refused and the message contains $why. */
function refused($why, $value)
{
	try
	{
		Expression::toExpression($value);
	}
	catch (InvalidArgumentException $e)
	{
		return strpos($e->getMessage(), $why) !== false;
	}
	return false;
}

/** Stands in for a component: the string is eval()'d inside it, like Button::click does. */
class Probe
{
	public $log = array();
	public $name = 'probe';
	public static $count = 7;

	function note($text)
	{
		$this->log[] = $text;
		return $this;
	}

	function run($expression)
	{
		return eval($expression);
	}
}

// ---- what goes through unchanged
check('a string is returned as it is', Expression::toExpression('return 1;') === 'return 1;');
check('null is returned as null', Expression::toExpression(null) === null);
check('something else is refused', refused('not int', 5));

// ---- a closure becomes its body
$simple = function ()
{
	$this->note("a");
	return 42;
};
check('body text of a closure', str_replace("\r\n", "\n", Expression::toExpression($simple)) === "\$this->note(\"a\");\n\treturn 42;");

$probe = new Probe();
check('the text does what the closure does', $probe->run(Expression::toExpression($simple)) === 42 && $probe->log === array('a'));

$probe = new Probe();
$other = function () { $this->note('one-line')->note('x'); };
check('closure on one line', Expression::toExpression($other) === "\$this->note('one-line')->note('x');");

// ---- what is allowed inside
$allowed = function ()
{
	// a comment stays with the code
	$total = 0;
	foreach (array(1, 2, 3) as $key => $value)
	{
		$total += $value;
	}
	$text = "name: {$this->name} / $this->name / }";
	$items = array('a' => $_SERVER['SERVER_NAME'] ?? 'none');
	$never ??= Probe::$count;
	if ($total > 1)
	{
		return $text.' '.$total.' '.$never;
	}
};
$text = Expression::toExpression($allowed);
check('local variables, foreach, interpolation, superglobals, static property are allowed', $text !== null && strpos($text, '$total') !== false);
check('the allowed closure gives the same result as a string', (new Probe())->run($text) === 'name: probe / probe / } 6 7');
check('braces in strings do not end the body', substr($text, -1) === '}' || strpos($text, 'return $text') !== false);

$static = static function () { return 5; };
check('a static closure is allowed', Expression::toExpression($static) === 'return 5;');

$typed = function (): int { return 3; };
check('a return type is skipped', Expression::toExpression($typed) === 'return 3;');

$comments = function () /* after */ { return 9; };
check('comments in the head', Expression::toExpression($comments) === 'return 9;');

// ---- what is refused
$value = 3;
check('use variable is refused', refused('use (...)', function () use ($value) { return $value; }));
check('parameters are refused', refused('parameters', function ($a) { return $a; }));
check('arrow function is refused', refused('arrow functions', fn() => 1));
check('nested closure is refused', refused('function or closure', function ()
{
	return function () { return 1; };
}));
check('nested closure on the same line is refused', refused('more than one closure', function () { return function () { return 1; }; }));
check('__DIR__ is refused', refused('__DIR__', function () { return __DIR__; }));
check('__FILE__ is refused', refused('__FILE__', function () { return __FILE__; }));
check('__LINE__ is refused', refused('__LINE__', function () { return __LINE__; }));
check('__CLASS__ is refused', refused('__CLASS__', function () { return __CLASS__; }));
check('variable variable is refused', refused('variable variable', function () { $n = 'a'; return $$n; }));
check('variable of the surrounding code is refused', refused('$value', function () { return $value; }));
check('variable read before it is assigned is refused', refused('$later', function () { $x = $later; $later = 1; return $x; }));
check('static variable is refused', refused('static variables', function () { static $calls = 0; return ++$calls; }));
check('two closures on a line are refused', refused('more than one closure', array(function () { return 1; }, function () { return 2; })[0]));

$evaluated = eval('return function () { return 1; };');
check('closure from eval() is refused', refused('source file', $evaluated));
check('first class callable is refused', refused('source file', strlen(...)));

// ---- the closure that is used in the Terminal screen (clients/terminal.php): same text as the string it replaced
$save = function ()
{
	if (!$this->a()->getAM("am")->save())
	{
		validate();
	}
	$this->com("page")->refresh();
};
$old = 'if (!$this->a()->getAM("am")->save())
        {
            validate();
        }
        $this->com("page")->refresh();';
$normalise = function ($s) { return preg_replace('/\s+/', '', $s); };
check('the Terminal save closure has the code of the old string', $normalise(Expression::toExpression($save)) === $normalise($old));

echo $failures == 0 ? "\nAll passed\n" : "\n$failures FAILED\n";
exit($failures == 0 ? 0 : 1);
