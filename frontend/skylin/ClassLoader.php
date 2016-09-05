<?php
	error_reporting(E_ERROR | E_WARNING | E_PARSE);
	class ClassLoader
	{
		static function load($className)
		{
			$root = realpath($_SERVER["DOCUMENT_ROOT"]);
			if (in_array($className,array(	'Application','Group','Button','Table','TextField','Response','TabStrip','Iter','Grid','Image',
											'Splitter','SearchLov','ChoiceLov','Lov','ModalPanel','Checkbox','Label','AnchorPanel','Date','IFrame',
											'File','Toolbar')))
			{
				require_once "$root/skylin/".$className . ".php";
			}
			else if ($className == 'out')
			{
				require_once "$root/skylin/out.php";
				out::init();
			}
			else if ($className == 'java')
			{
				//
				
				//require_once "$root/java/Java.inc";
				require_once "$root/skylin/bridge/java.php";
				set_time_limit(300);
			}
		}
	}

?>