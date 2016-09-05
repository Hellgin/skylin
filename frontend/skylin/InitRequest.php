<?php
	spl_autoload_register(function($className) 
	{
		require_once 'ClassLoader.php';
		ClassLoader::load($className);
	});
	ClassLoader::load('java'); 
	require_once 'dependencies/closure/autoload.php';
?>