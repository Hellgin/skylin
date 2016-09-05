<?php

    class Out
    {
		static function init()
		{
			if (!isset($_SESSION['OUT']))
			{
				$root = realpath($_SERVER["DOCUMENT_ROOT"]);
				//require_once "$root/java/Java.inc";
				//require_once "$root/skylin/bridge/java.php";
				$_SESSION['OUT'] = new java('skylin.util.FrontEndOut');
			}
		}
		
		static function println($o)
		{
			$_SESSION['OUT']->println($o);
		}
	}


?>