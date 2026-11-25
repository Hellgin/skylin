<?php
	class ClassLoader
	{
		static function load($className)
		{
			if (in_array($className,array(	'Application','Group','Button','Table','TextField','Response','TabStrip','Iter','Grid','Image',
											'Splitter','SearchLov','ChoiceLov','Lov','ModalPanel','Checkbox','Label','AnchorPanel','Date','IFrame',
											'File','FileArea','Toolbar','Template','Progress','Expression')))
			{
				require_once $className . ".php";
			}
			else if ($className == 'out')
			{
				require_once "out.php";
				out::init();
			}
			else if ($className == 'java')
			{
				//
				
				//require_once "$root/java/Java.inc";
				require_once "bridge/java.php";
				set_time_limit(300);
			}
		}
	}

?>