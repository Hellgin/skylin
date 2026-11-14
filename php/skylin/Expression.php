<?php
/*
 * Expression: turns what a screen passes as "an expression" into the string Skylin keeps and eval()s.
 *
 * Skylin keeps its handlers as strings in the session and eval()s them later, in another request, inside the component
 * (so $this is the component, see Button::click). A closure cannot be kept in the session, so a closure given where an
 * expression is expected is turned into its source text straight away, and from then on it is the same as if the string had
 * been written out:
 *
 *     $save->setActionE('$this->com("page")->refresh();');                // as before
 *     $save->setActionE(function () { $this->com("page")->refresh(); });   // the body of the closure becomes the string
 *
 * The text is read from the source file (ReflectionFunction gives the file and the lines, the PHP tokenizer finds the body),
 * nothing is executed and no library is used. The closure only LOOKS like it runs where it is written; it runs later, as
 * a string, in the component. To make sure nothing can silently stop working because of that, this is limited on purpose:
 * only $this (the component), the superglobals and the variables the closure assigns itself may be used. A closure that
 * is not of that kind is refused with an exception saying where and why (never half-converted):
 *
 *   - parameters, use (...) variables and static variables (nothing is captured, nothing is passed in)
 *   - arrow functions (fn), which capture by name, and closures or functions inside the closure
 *   - __DIR__, __FILE__, __LINE__, __FUNCTION__, __CLASS__, __METHOD__, __NAMESPACE__, __TRAIT__ (they mean something else
 *     where the string is eval()'d)
 *   - variable variables ($$name, ${...})
 *   - a variable that is read before it is assigned in the closure (it would be a variable of the surrounding code)
 *   - two closures starting on the same line (the source position is a line, so they cannot be told apart)
 *   - a closure that does not come from a source file (eval()'d code, a first class callable such as strlen(...))
 *
 * Names in the body are not resolved: write class names the way they have to be read where the string is eval()'d (the
 * framework classes are global, so this is nothing to think about for them), and put no namespace or use import around the
 * closure that it depends on.
 */
class Expression
{
	/** The source text of a file as tokens, per file and modification time (the tree is rebuilt rarely, a file is read once). */
	private static $tokenCache = array();

	/**
	 * A string expression is returned as it is (and null as null); a closure becomes the source text of its body.
	 *
	 * @param string|Closure|null $value
	 * @return string|null
	 */
	static function toExpression($value)
	{
		if (is_null($value) || is_string($value))
		{
			return $value;
		}
		if ($value instanceof Closure)
		{
			return self::sourceOf($value);
		}
		throw new InvalidArgumentException('An expression is a string or a closure, not '.get_debug_type($value));
	}

	/** The text of the body of the closure, checked as described above. */
	private static function sourceOf(Closure $closure)
	{
		$reflection = new ReflectionFunction($closure);
		$file = $reflection->getFileName();
		if ($file === false || !is_file($file))
		{
			throw self::refused($reflection, 'it does not come from a source file (code run by eval() and first class callables cannot be used)');
		}
		$captured = array_keys($reflection->getStaticVariables());
		if (count($captured) > 0)
		{
			throw self::refused($reflection, 'it has use (...) or static variables ($'.implode(', $', $captured).'); only $this may be used');
		}

		$tokens = self::tokensOf($file);
		$open = self::findBodyStart($reflection, $tokens);
		$close = self::findBodyEnd($reflection, $tokens, $open);
		self::checkBody($reflection, $tokens, $open + 1, $close - 1);

		$text = '';
		for ($i = $open + 1; $i < $close; $i++)
		{
			$text .= $tokens[$i]->text;
		}
		return trim($text);
	}

	/**
	 * The index of the "{" that opens the body of the closure which starts on the line the reflection gives. Checks the head
	 * of the closure on the way: no parameters, no use, and not an arrow function.
	 */
	private static function findBodyStart(ReflectionFunction $reflection, array $tokens)
	{
		$line = $reflection->getStartLine();
		$starts = array();
		for ($i = 0; $i < count($tokens); $i++)
		{
			$token = $tokens[$i];
			if ($token->line > $line)
			{
				break;
			}
			if ($token->line == $line && ($token->id === T_FUNCTION || $token->id === T_FN))
			{
				$next = self::next($tokens, $i);
				// "function name(" is a declaration; a closure goes straight to "(" (or "&(")
				if ($next !== -1 && ($tokens[$next]->text === '(' || $tokens[$next]->text === '&'))
				{
					$starts[] = $i;
				}
			}
		}
		if (count($starts) == 0)
		{
			throw self::refused($reflection, 'the closure could not be found in the file at that line');
		}
		if (count($starts) > 1)
		{
			throw self::refused($reflection, 'more than one closure starts on this line; put each closure on a line of its own');
		}
		$start = $starts[0];
		if ($tokens[$start]->id === T_FN)
		{
			throw self::refused($reflection, 'arrow functions (fn) are not supported, they capture variables by name; write function () { ... }');
		}

		$i = self::next($tokens, $start);
		if ($tokens[$i]->text === '&')
		{
			$i = self::next($tokens, $i);
		}
		// the parameter list: nothing may be in it
		$close = self::matching($tokens, $i);
		if (self::next($tokens, $i) !== $close)
		{
			throw self::refused($reflection, 'it has parameters; nothing is passed to a handler string');
		}
		$i = self::next($tokens, $close);
		if ($i !== -1 && $tokens[$i]->id === T_USE)
		{
			throw self::refused($reflection, 'it has use (...) variables; only $this may be used');
		}
		// an optional return type is skipped
		while ($i !== -1 && $tokens[$i]->text !== '{')
		{
			$i = self::next($tokens, $i);
		}
		if ($i === -1)
		{
			throw self::refused($reflection, 'the body of the closure could not be found');
		}
		return $i;
	}

	/** The index of the "}" that closes the body which opens at $open; it has to be on the line the reflection says. */
	private static function findBodyEnd(ReflectionFunction $reflection, array $tokens, $open)
	{
		$depth = 0;
		for ($i = $open; $i < count($tokens); $i++)
		{
			$token = $tokens[$i];
			// "{", and the "{$" and "${" that start an expression inside a string
			if ($token->id === ord('{') || $token->id === T_CURLY_OPEN || $token->id === T_DOLLAR_OPEN_CURLY_BRACES)
			{
				$depth++;
			}
			else if ($token->id === ord('}'))
			{
				$depth--;
				if ($depth == 0)
				{
					if ($token->line != $reflection->getEndLine())
					{
						throw self::refused($reflection, 'the end of the closure found in the file (line '.$token->line.') is not where PHP says it ends (line '.$reflection->getEndLine().')');
					}
					return $i;
				}
			}
		}
		throw self::refused($reflection, 'the end of the closure could not be found');
	}

	/** Refuses what would mean something else once the text is eval()'d (see the top of this file). */
	private static function checkBody(ReflectionFunction $reflection, array $tokens, $from, $to)
	{
		$defined = array();
		for ($i = $from; $i <= $to; $i++)
		{
			$token = $tokens[$i];
			$id = $token->id;

			if ($id === T_FUNCTION || $id === T_FN)
			{
				throw self::refused($reflection, 'it contains a function or closure (line '.$token->line.'); not supported');
			}
			if ($id === T_DIR || $id === T_FILE || $id === T_LINE || $id === T_FUNC_C || $id === T_CLASS_C
				|| $id === T_METHOD_C || $id === T_NS_C || $id === T_TRAIT_C)
			{
				throw self::refused($reflection, 'it uses '.$token->text.' (line '.$token->line.'), which would mean something else where the text is eval()\'d');
			}
			if ($id === ord('$') || $id === T_DOLLAR_OPEN_CURLY_BRACES)
			{
				throw self::refused($reflection, 'it uses a variable variable (line '.$token->line.'); not supported');
			}
			if ($id !== T_VARIABLE)
			{
				continue;
			}

			$name = $token->text;
			$before = self::previous($tokens, $i);
			if ($before !== -1 && $tokens[$before]->id === T_DOUBLE_COLON)
			{
				continue;   // Class::$staticProperty is not a local variable
			}
			if ($name === '$this' || isset(self::superglobals()[$name]) || isset($defined[$name]))
			{
				continue;
			}
			if (self::assigns($tokens, $i))
			{
				$defined[$name] = true;
				continue;
			}
			throw self::refused($reflection, 'the variable '.$name.' (line '.$token->line.') is read before the closure assigns it; '
				.'a variable of the surrounding code is not there when the text is eval()\'d, only $this and the variables assigned in the closure can be used');
		}
	}

	/** True when this occurrence of a variable gives it a value: "$a = ...", "$a ??= ...", "... as $a", "... as $k => $a". */
	private static function assigns(array $tokens, $i)
	{
		$next = self::next($tokens, $i);
		if ($next !== -1 && ($tokens[$next]->id === ord('=') || $tokens[$next]->id === T_COALESCE_EQUAL))
		{
			return true;
		}
		$previous = self::previous($tokens, $i);
		if ($previous === -1)
		{
			return false;
		}
		if ($tokens[$previous]->id === T_AS)
		{
			return true;
		}
		// foreach ($list as $key => $value): the value follows the "=>" of a key that follows "as"
		if ($tokens[$previous]->id === T_DOUBLE_ARROW)
		{
			$key = self::previous($tokens, $previous);
			if ($key !== -1 && $tokens[$key]->id === T_VARIABLE)
			{
				$as = self::previous($tokens, $key);
				return $as !== -1 && $tokens[$as]->id === T_AS;
			}
		}
		return false;
	}

	private static function superglobals()
	{
		return array('$_GET' => 1, '$_POST' => 1, '$_REQUEST' => 1, '$_SESSION' => 1, '$_SERVER' => 1, '$_COOKIE' => 1,
			'$_FILES' => 1, '$_ENV' => 1, '$GLOBALS' => 1);
	}

	/** The index of the bracket that closes the "(" at $open. */
	private static function matching(array $tokens, $open)
	{
		$depth = 0;
		for ($i = $open; $i < count($tokens); $i++)
		{
			if ($tokens[$i]->text === '(')
			{
				$depth++;
			}
			else if ($tokens[$i]->text === ')')
			{
				$depth--;
				if ($depth == 0)
				{
					return $i;
				}
			}
		}
		return -1;
	}

	/** The index of the next token that is not whitespace or a comment, or -1. */
	private static function next(array $tokens, $i)
	{
		for ($i = $i + 1; $i < count($tokens); $i++)
		{
			if (!self::blank($tokens[$i]))
			{
				return $i;
			}
		}
		return -1;
	}

	/** The index of the previous token that is not whitespace or a comment, or -1. */
	private static function previous(array $tokens, $i)
	{
		for ($i = $i - 1; $i >= 0; $i--)
		{
			if (!self::blank($tokens[$i]))
			{
				return $i;
			}
		}
		return -1;
	}

	private static function blank(PhpToken $token)
	{
		return $token->id === T_WHITESPACE || $token->id === T_COMMENT || $token->id === T_DOC_COMMENT;
	}

	private static function tokensOf($file)
	{
		$key = $file.'|'.filemtime($file);
		if (!isset(self::$tokenCache[$key]))
		{
			self::$tokenCache = array();   // only the file that is being read now is kept
			self::$tokenCache[$key] = PhpToken::tokenize(file_get_contents($file));
		}
		return self::$tokenCache[$key];
	}

	private static function refused(ReflectionFunction $reflection, $why)
	{
		return new InvalidArgumentException('This closure cannot be used as an expression ('.$reflection->getFileName().':'
			.$reflection->getStartLine().'): '.$why);
	}
}
