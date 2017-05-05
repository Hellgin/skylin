<?php
	error_reporting(E_ERROR | E_WARNING | E_PARSE);
	//ini_set('memory_limit','256M');
	spl_autoload_register(function($className) 
	{
		require_once 'ClassLoader.php';
		ClassLoader::load($className);
	});
	ClassLoader::load('java'); 
	//require_once 'dependencies/closure/autoload.php';
	
	function validate($v = true)
	{
		$_REQUEST["SKYLIN_VALIDATE"] = $v;
	}
?>