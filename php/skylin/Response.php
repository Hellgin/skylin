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
		
		/**
		 * The length of a message in the unit the browser counts in: UTF-16 code units, which is what JavaScript's
		 * length and substring() work with (a character from U+10000 up, an emoji for example, counts 2). PHP's strlen()
		 * counts bytes, which differs as soon as a message has a character outside ASCII, and the browser would then cut
		 * the response in the wrong places. The text must be valid UTF-8, see frame().
		 */
		static function browserLength($text)
		{
			return intdiv(strlen(mb_convert_encoding($text, 'UTF-16LE', 'UTF-8')), 2);
		}
		
		/**
		 * A response as the browser's response() reads it: the number of messages, the length of each (browserLength())
		 * and a '-', then the messages one after the other. The same text is sent over HTTP and over the WebSocket, and
		 * both are UTF-8 on the wire and decoded to a string by the browser. A byte that is not valid UTF-8 would be
		 * decoded by the browser into something else than it is counted as here, so it is replaced first (by '?').
		 */
		private static function frame($messages)
		{
			$clean = array();
			foreach ($messages as $msg)
			{
				$clean[] = mb_scrub((string) $msg, 'UTF-8');
			}
			$ret = sizeof($clean).'';
			foreach ($clean as $msg)
			{
				$ret = $ret.','.self::browserLength($msg);
			}
			$ret = $ret . '-';
			foreach ($clean as $msg)
			{
				$ret = $ret.$msg;
			}
			return $ret;
		}
		
		static function getDataToSend($includeBackEnd,$sessionWriteClose)
		{
			$sessionResponse = null;
			if (isset($_SESSION['RESPONSE']))
			{
				$sessionResponse = $_SESSION['RESPONSE'];
				unset($_SESSION['RESPONSE']);
			}
			if ($sessionWriteClose)
			{
			     session_write_close();
			}
			
			if ($includeBackEnd)
			{
				self::addBackEndMessages($_SESSION['ROOT_APPLICATION']);
			}
			if (isset($_REQUEST['RESPONSE']))
			{
				$ret = self::frame($_REQUEST['RESPONSE']);
				unset($_REQUEST['RESPONSE']);
			}
					
			//if (isset($_SESSION['RESPONSE']))
			if($sessionResponse != null)
			{
				$ret = self::frame($sessionResponse);
				//unset($_SESSION['RESPONSE']);
			}
			
			
			
			return $ret;
		}
		
		static function send($includeBackEnd = true)
		{
			echo self::getDataToSend($includeBackEnd,true);
			//out::println('memory: '.(memory_get_peak_usage()/1024/1024). ' '.count($_SESSION['APPLICATIONS']));
			if (memory_get_peak_usage()/1024/1024 > 130)
			{
				$p = '';
				foreach ($_POST as $key => $value)
				{
					$p = $p.$key.'='.$value.' ';
				}
				
				/*
				foreach ($_SESSION['APPLICATIONS'] as $a)
				{
					$p = $p.$a->getName().'\n';
				}
				*/
				
				error_log('memory: '.(memory_get_peak_usage()/1024/1024). ' '.session_id(). ' '.count($_SESSION['APPLICATIONS']).' '.$p."\n",0);
			}
			//BridgeConnection.jvmCall('6');
		}
		
		static function sendWebSocket($topAppId,$includeBackEnd = true)
		{
		    if (jvmCall('f,'.$topAppId) == 'true')
		    {
		      jvmCall('7,'.$topAppId.','.self::getDataTosend($includeBackEnd,false));
		    }
		}
		
		static function addBackEndMessages($app)
		{
			if (!isset($_REQUEST['RESPONSE']))
			{
				$_REQUEST['RESPONSE'] = array();
			}
			foreach($app->getApplicationModules() as $a)
			{
				if ($a == null)
				{
					continue;
				}
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
		
		static function openWindow($window, $name = null)
		{
			// A newline is dropped on the way to the browser and the name gets stuck onto the address.
			// A control character makes the click do nothing. "||" survives, and these addresses do not contain it.
			if ($name !== null && $name !== '')
			{
				self::addMessage('openwindow', $window.'||'.$name);
			}
			else
			{
				self::addMessage('openwindow', $window);
			}
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
		
		static function setUrl($url)
		{
			self::addMessage('seturl',$url);
		}
	}
	if (isset($_POST['flush'])) 
	{
		require "InitRequest.php"; 
		Response::send();
	}
?>