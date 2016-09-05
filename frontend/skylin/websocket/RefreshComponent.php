<?php
	$input = explode(',',$_SERVER['QUERY_STRING']);
	session_id($input[0]);
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	Application::getApplication($input[1])->getComponent($input[2])->refresh();	
	Response::send();
?>