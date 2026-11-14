<?php
class BridgeSessionHandler implements SessionHandlerInterface
{
    private $savePath;
    private $bridgeConnection;

    function __construct($con)
    {
    	$this->bridgeConnection = $con;
    }
    
    public function open($savePath, $sessionName)
    {
    	$this->savePath = $savePath;
    	//return ((string)jvmCall('b,1,'.$savePath.','.session_id().','.$sessionName)) === 'true';
    	$_REQUEST['USING_BRIDGE_SESSION_HANDLER'] = true;
    	return ((string)$this->bridgeConnection->jvmCall_internal('b,1,'.$savePath.','.session_id().','.$sessionName)) === 'true';
    }
    
    public function read($id)
    {
    	//return (string)jvmCall('b,2,'.$id);
    	return (string)$this->bridgeConnection->jvmCall_internal('b,2,'.$id);
    }

    public function write($id, $data)
    {
    	//return ((string)(new BridgeConnection())->jvmCall_internal('b,3,'.$id.','.$data)) === 'true';
    	return ((string)$this->bridgeConnection->jvmCall_internal('b,3,'.$id.','.$data)) === 'true';
    	//return (string)jvmCall('b,3,'.$id.','.$data);
    }
    
    public function close()
    {
    	//return ((string)(new BridgeConnection())->jvmCall_internal('b,4,'.session_id())) === 'true';
    	$ret = ((string)$this->bridgeConnection->jvmCall_internal('b,4,'.session_id())) === 'true';
    	//$this->bridgeConnection->jvmCall_internal('6');
    	return $ret;
    	//return (string)jvmCall('b,4,'.session_id());
    }



    public function destroy($id)
    {
    	return ((string)$this->bridgeConnection->jvmCall_internal('b,5,'.$id)) === 'true';
    }

    public function gc($maxlifetime)
    {
    	return ((string)$this->bridgeConnection->jvmCall_internal('b,6,'.$maxlifetime)) === 'true';
    }
}
?>