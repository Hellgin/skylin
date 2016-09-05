<?php
	require_once "skylin/bridge/BridgeConnection.php";
	session_start();
	jvmCall('8,'.session_id());
	$call = $_SERVER['QUERY_STRING'];
	$data = file_get_contents('php://input');
	if (strlen($data) > 0)
	{
		$call = $call.','.$data;
	}
	$all = jvmCall('9,'.$call);
	$t = explode(',',$all,2);
	$headerSize = $t[0];
	$header = substr($t[1],0,$headerSize);
	$body = substr($t[1],$headerSize,strlen($t[1]));
	
	$t = explode(',',$header,2);
	$t = explode(',',$t[1],$t[0]+1);
	
	$headerData = $t[sizeof($t)-1]; 

	for ($i = 0; $i < sizeof($t)-1;$i = $i + 1)
	{
		$h = substr($headerData,0,$t[$i]);
		$headerData =substr($headerData,$t[$i],strlen($headerData));
		header($h,false);
	}
	echo $body;
	
	
	
	/*
	$t = explode(',',$all,2);
	$headerSize = $t[0];
	$header = substr($t[1],0,$headerSize);
	$body = substr($t[1],$headerSize,strlen($t[1]));
	
	header($header);
	header('Content-Disposition: attachment; filename="lol.png"');
	echo $body;
	*/
	
?>