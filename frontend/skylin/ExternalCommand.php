<?php

/*	//disabled due to changes to websockets. Rethink this when needed again.

	$input = explode(',',$_SERVER['QUERY_STRING'],2);
	session_id(apc_fetch($input[0]));
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	//Application::getRootApplication()->handleExternalCommand($input[1]);
	$_SESSION[$input[0]]->getExtra()->handleExternalCommand($input[1]);
	Response::sendWebSocket();
	
	*/
	


?>