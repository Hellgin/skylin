<?php


	session_id(apc_fetch($_SERVER['QUERY_STRING']));
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	//foreach ($_SESSION['LATEST_SECURITY_CONTEXT']->getRoles() as $role)
	$sec = $_SESSION[$_SERVER['QUERY_STRING']];
	foreach ($sec->getRoles() as $role)
	{
		$ret = $ret.$role.',';
	}	
	if (strlen($ret) > 0)
	{
		$ret = substr($ret,0,strlen($ret)-1);
	}
	echo $ret;


?>