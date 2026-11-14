<?php
	$input =  explode(',',file_get_contents('php://input'),3);
	session_id($input[0]);
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	// Off unless FrameworkParams.cnf contains BACKEND_EVAL:true. The body is PHP that backendEval runs.
	$backendEval = isset($_SESSION['UTIL']) ? $_SESSION['UTIL']->getFrameworkParam('BACKEND_EVAL') : null;
	if (strcasecmp((string)$backendEval, 'true') !== 0)
	{
		http_response_code(403);
		exit(0);
	}
	$ret = Application::getApplication($input[1])->backendEval($input[2]);
	$ret = _objectToString($ret);
	echo(strlen($ret).','.$ret);
	Response::send();
?>