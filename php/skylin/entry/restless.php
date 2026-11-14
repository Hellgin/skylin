<?php
require_once 'skylin/bridge/BridgeConnection.php';
require_once 'skylin/bridge/BridgeSessionHandler.php';


$sessid = $_COOKIE["PHPSESSID"];
if ($sessid != null)
{
    jvmCall('8,'.$_COOKIE["PHPSESSID"]);
}
else
{
    //This will likely never be used. restless services will normaly (always?) be used on existing sessions.
    
    session_set_save_handler(new BridgeSessionHandler(jvmGetBridgeConnection()), true);
    session_start();
    session_write_close();
    jvmCall('8,'.session_id());
}


$auth = null;
$headers = getallheaders();
if (array_key_exists('Authorization',$headers)){$auth = $headers['Authorization'];}
$qs = (isset($queryStringOverride)?$queryStringOverride:$_SERVER['QUERY_STRING']);
$call = $_SERVER['REQUEST_METHOD'].','.$auth.','.$qs;//temp: expand to include all headers
$data = file_get_contents('php://input');
if (strlen($data) > 0)
{
    $call = $call.'?'.$data;
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
?>