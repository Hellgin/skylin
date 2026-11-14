<?php
	// PHP 8 turned undefined array keys / properties (which the framework relies on returning null) into warnings.
	// Warnings are written to the PHP error log, never into the response body.
	error_reporting(E_ERROR | E_PARSE);
	ini_set('display_errors', '0');
	// Pages and responses are UTF-8 text (the browser reads the response framing as UTF-16 code units of the decoded
	// text, see Response::frame()): say so, whatever php.ini's default_charset is. A script that sends another
	// Content-Type later replaces this header.
	if (!headers_sent())
	{
		header('Content-Type: text/html; charset=UTF-8');
	}
	if (!($_SERVER['REQUEST_METHOD'] == 'POST' || ($allow_get && $_SERVER['REQUEST_METHOD'] == 'GET')))
	{
	    http_response_code(405);
	    exit(0);
	}
	$skylinBackendConnection;
	spl_autoload_register(function($className) 
	{
		require_once 'ClassLoader.php';
		ClassLoader::load($className);
	});
	

	require_once 'bridge/BridgeConnection.php';
	require_once 'bridge/BridgeSessionHandler.php';session_set_save_handler(new BridgeSessionHandler(jvmGetBridgeConnection()), true);
	ClassLoader::load('java');

	
	
	foreach ($_COOKIE as $key=>$val)
	{
	    jvmCall('d,'.$key.';'.$val);
	}
	//setcookie('hax', 2, time()+10,'/');
	
	function validate($v = true)
	{
		$_REQUEST["SKYLIN_VALIDATE"] = $v;
	}
?>