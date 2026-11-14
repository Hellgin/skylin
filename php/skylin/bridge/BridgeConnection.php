<?php

/*
function jvmCall($data)
{
	if (!isset($_REQUEST['BRIDGE_CONNECTION']))
	{
		$_REQUEST['BRIDGE_CONNECTION'] = new BridgeConnection();
	}
	return $_REQUEST['BRIDGE_CONNECTION']->jvmCall_internal($data);
}
*/

function jvmGetBridgeConnection()
{
	if (!isset($_REQUEST['BRIDGE_CONNECTION']))
	{
		$_REQUEST['BRIDGE_CONNECTION'] = new BridgeConnection();
		//$_REQUEST['BRIDGE_CONNECTION']->jvmCall_internal('c,new con');
	}
	return $_REQUEST['BRIDGE_CONNECTION'];
}

function jvmCall($data)
{
	return jvmGetBridgeConnection()->jvmCall_internal($data);
}

/*
 * Bridge transport. The PHP side does not read any Skylin .cnf file (those belong to the JVM) and has no configuration
 * for the bridge at all: transport and socket path are fixed by design and must match the JVM's BridgeListener.java.
 *
 * Transport: the Unix domain socket whenever it is supported (Linux, macOS, Windows 10+ through the sockets
 * extension), TCP 127.0.0.1:1400 otherwise (in practice only Windows before 10 1803). The JVM
 * (BridgeListener.useTcpTransport) applies the same rule on its own. Support is probed on every OS by creating an
 * AF_UNIX socket, not derived from the OS name or php_uname(): under Apache on Windows PHP reports "Windows 8"
 * (6.2 build 9200) even on Windows 10/11, because httpd.exe has no compatibility manifest. If the two probes
 * disagree, that is a setup problem and the bridge does not connect.
 */
function skylinBridgeUsesTcp()
{
	$probe = @socket_create(AF_UNIX, SOCK_STREAM, 0);
	if ($probe === false)
	{
		return true;
	}
	socket_close($probe);
	return false;
}

/**
 * Path of the bridge Unix domain socket, fixed per OS (same values as BridgeListener.bridgeSocketPath in Java):
 * Windows C:\sky\bridge.sock, macOS /tmp/sky/bridge.sock (read-only root volume, no /sky), everything else
 * /sky/bridge.sock. The directory has to be usable by the web server's PHP and by the JVM.
 *
 * @param string|null $osFamily PHP_OS_FAMILY by default (parameter for tests)
 */
function skylinBridgeSocketPath($osFamily = null)
{
	if ($osFamily === null) $osFamily = PHP_OS_FAMILY;
	if ($osFamily === 'Windows') return 'C:\\sky\\bridge.sock';
	if ($osFamily === 'Darwin') return '/tmp/sky/bridge.sock';
	return '/sky/bridge.sock';
}
class BridgeConnection
{
	private $socket;

	public function __construct()
	{
		if (skylinBridgeUsesTcp())
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
				error_log("socket_connect() to $address:$service_port failed.\nReason: " . socket_strerror(socket_last_error($this->socket)) . "\n",0);
				exit;
			}

			socket_set_option($this->socket, SOL_TCP, TCP_NODELAY, 1);
		}
		else
		{
			$path = skylinBridgeSocketPath();
			$this->socket = socket_create(AF_UNIX, SOCK_STREAM, 0);
			if ($this->socket === false) {
				error_log("socket_create() failed: reason: " . socket_strerror(socket_last_error()) . "\n",0);
				exit;
			}

			$result = socket_connect($this->socket, $path);
			if ($result === false) {
				error_log("socket_connect() to unix socket '$path' failed.\nReason: " . socket_strerror(socket_last_error($this->socket)) . "\n",0);
				exit;
			}
		}
	}
	public function jvmCall_internal($in)
	{
	    //a method call (command 2) is answered in one or more frames, see the loop below. Decided before the request
	    //is replaced by the temp file command: a big request is a method call too.
	    $multi = (substr($in,0,1) == '2');
	    
	    $tempFileName = null;
	    if (strlen($in) > 1048576)
	    {
	        $tempFileName = tempnam(ini_get('upload_tmp_dir') ? ini_get('upload_tmp_dir') : sys_get_temp_dir(),'big');
	        $tempFile = fopen($tempFileName,'wb');//binary: the frame must not be changed (\n -> \r\n on Windows)     
	        fwrite($tempFile, $in);
	        fclose($tempFile);
	        $in = 'e,'.$tempFileName;
	    }

		$size = strlen($in);
		$size = $size.'';
		while (strlen($size) < 7)
		{
			$size = '0'.$size;
		}
		$in = $size.$in;
		
		socket_write($this->socket, $in, strlen($in));
		
		do
		{
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
    		
    		$type = substr($out,0,1);
    		if ($type == "3")
    		{
    		    $out = substr($out,1);
    		}
    		else //if ($type == "4")
    		{
    		    $file = substr($out,1);
    		    $out = file_get_contents($file);
    		    unlink($file);
    		}


    		if ($multi)
    		{
    		   $command = substr($out,0,1);
    		   if ($command == '1')
    		   {
    		      $data = explode(',',substr($out,1));
    		      $c = new BridgeConnection();
    		      $c->jvmCall_internal('8,'.session_id());
    		      $old  = $_REQUEST['BRIDGE_CONNECTION'];
    		      $_REQUEST['BRIDGE_CONNECTION'] = $c;
    		      //$c->jvmCall_internal('c,test!!!');
    		      $ret = Application::getApplication((int)$data[0])->backendEval($data[1]);
    		      $_REQUEST['BRIDGE_CONNECTION'] = $old;
    		      //$old->jvmCall_internal($ret);
    		   }
    		}
    		
		}while($multi && $command != '0');
		
		if ($multi)
		{
		    $out = substr($out,1);
		}
		if ($tempFileName != null)
		{
		    unlink($tempFileName);
		}
		return $out;
	}
	
	public function __destruct()
	{
		$this->jvmCall_internal('6');
		socket_close($this->socket);
	}
}
?>
