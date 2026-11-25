<?php
	// Only the JVM calls this (AM.eval). The body is <token>,<session id>,<application id>,<PHP code>. The caller must be
	// on this machine and the token must be the JVM's (bridge command g), both checked before the session id is used.
	require_once $_SERVER['DOCUMENT_ROOT']."/skylin/LoopbackAddress.php";
	require_once $_SERVER['DOCUMENT_ROOT']."/skylin/bridge/BridgeConnection.php";
	$input = explode(',',file_get_contents('php://input'),4);
	if (!LoopbackAddress::is($_SERVER['REMOTE_ADDR'] ?? null) || count($input) != 4 || (string)jvmCall('g,'.$input[0]) !== 'true')
	{
		http_response_code(403);
		exit(0);
	}
	session_id($input[1]);
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	$ret = Application::getApplication($input[2])->backendEval($input[3]);
	$ret = _objectToString($ret);
	echo(strlen($ret).','.$ret);
	Response::send();
?>
