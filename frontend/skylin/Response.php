<?php 
	class Response
	{
		static function addMessage($type,$message)
		{
			if ($_SERVER['REQUEST_METHOD'] == 'POST')
			{
				if (!isset($_REQUEST['RESPONSE']))
				{
					$_REQUEST['RESPONSE'] = array();
				}
				array_push($_REQUEST['RESPONSE'],$type.','.$message);
			}
			else
			{
				if (!isset($_SESSION['RESPONSE']))
				{
					$_SESSION['RESPONSE'] = array();
				}
				array_push($_SESSION['RESPONSE'],$type.','.$message);
			}
		}
		
		static function getDataToSend($includeBackEnd = true)
		{
			if ($includeBackEnd)
			{
				self::addBackEndMessages($_SESSION['ROOT_APPLICATION']);
			}
			if (isset($_REQUEST['RESPONSE']))
			{
				$ret = sizeof($_REQUEST['RESPONSE']).'';
				foreach ($_REQUEST['RESPONSE'] as $msg)
				{
					$ret = $ret.','.strlen($msg);
				}
				$ret = $ret . '-';
				foreach ($_REQUEST['RESPONSE'] as $msg)
				{
					$ret = $ret.$msg;
				}
			}
					
			if (isset($_SESSION['RESPONSE']))
			{
				$ret = sizeof($_SESSION['RESPONSE']).'';
				foreach ($_SESSION['RESPONSE'] as $msg)
				{
					$ret = $ret.','.strlen($msg);
				}
				$ret = $ret . '-';
				foreach ($_SESSION['RESPONSE'] as $msg)
				{
					$ret = $ret.$msg;
				}
				unset($_SESSION['RESPONSE']);
			}
			
			
			
			return $ret;
		}
		
		static function send($includeBackEnd = true)
		{
			echo self::getDataToSend($includeBackEnd);
			BridgeConnection.jvmCall('6');
			//session_write_close();
		}
		
		static function sendWebSocket($includeBackEnd = false)
		{
			BridgeConnection.jvmCall('7,'.self::getDataTosend($includeBackEnd));
			BridgeConnection.jvmCall('6');
		}
		
		static function addBackEndMessages($app)
		{
			if (!isset($_REQUEST['RESPONSE']))
			{
				$_REQUEST['RESPONSE'] = array();
			}
			foreach($app->getApplicationModules() as $a)
			{
				foreach($a->getMessages() as $m)
				{
					array_push($_REQUEST['RESPONSE'],$m);
				}
			}
			foreach($app->getChildApplications() as $a)
			{
				self::addBackEndMessages($a);
			}
			if ($app->getAbove() != null)
			{
				self::addBackEndMessages($app->getAbove());
			}
		}
		static function info($message)
		{
			self::addMessage('displaymsg','info,'.$message);
		}
		
		static function warn($message)
		{
			self::addMessage('displaymsg','warn,'.$message);
		}
		
		static function error($message)
		{
			self::addMessage('displaymsg','error,'.$message);
		}
		
		static function fatal($message)
		{
			self::addMessage('displaymsg','fatal,'.$message);
		}
		
		static function openWindow($window)
		{
			self::addMessage('openwindow',$window);
		}
		static function locationWindow($window)
		{
			self::addMessage('locationwindow',$window);
		}
		static function setTitle($title)
		{
			self::addMessage('settitle',$title);
		}
		static function click($com)
		{
			if ($com->getEditable())
			{
				self::addMessage('click',$com->getFullId());
			}
		}
		static function reload()
		{
			self::addMessage('reload','0');
		}
		static function redirect($url)
		{
			header('Location: '.$url);
			exit();
		}
		
		static function download($link)
		{
			self::addMessage('download',$link);
		}
	}
	if (isset($_POST['flush'])) 
	{
		require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
		Response::send();
	}
?>