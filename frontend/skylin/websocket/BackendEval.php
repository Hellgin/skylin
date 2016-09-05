<?php
	$input =  explode(',',file_get_contents('php://input'),3);
	session_id($input[0]);
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	$ret = Application::getApplication($input[1])->backendEval($input[2]);
	$ret = _objectToString($ret);
	echo(strlen($ret).','.$ret);
	Response::send();
?>