<?php

function jvmCall($data)
{
	if (!isset($_REQUEST['BRIDGE_CONNECTION']))
	{
		$_REQUEST['BRIDGE_CONNECTION'] = new BridgeConnection();
	}
	return $_REQUEST['BRIDGE_CONNECTION']->jvmCall_internal($data);
}

class BridgeConnection
{
	private $socket;
	
	public function __construct()
	{
		$service_port = 1400;
		$address = '127.0.0.1';
		$this->socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
		if ($this->socket === false) {
			error_log("socket_create() failed: reason: " . socket_strerror(socket_last_error()) . "\n",0);
			exit;
		}
		
		$result = socket_connect($this->socket, $address, $service_port);
		if ($result === false) {
			error_log("socket_connect() failed.\nReason: ($result) " . socket_strerror(socket_last_error($this->socket)) . "\n",0);
			exit;
		}
	}
	
	public function jvmCall_internal($in)
	{
		$size = strlen($in);
		$size = $size.'';
		while (strlen($size) < 7)
		{
			$size = '0'.$size;
		}
		$in = $size.$in;
		
		socket_write($this->socket, $in, strlen($in));
		
		$out = '';
		
		while (strlen($out) < 7)
		{
			$out = $out.socket_read($this->socket, 7 - strlen($out));
			if (socket_last_error($this->socket) != 0)exit;
		}
		$size = hexdec($out);
		$out = '';
		
		while (strlen($out) < $size)
		{
			$out = $out.socket_read($this->socket, $size - strlen($out));
			if (socket_last_error($this->socket) != 0)exit;
		}
		return $out;
	}
	
	public function __destruct()
	{
		socket_close($this->socket);
	}
	
}
?>