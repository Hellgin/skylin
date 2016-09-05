<?php
require_once "BridgeConnection.php";
session_start();
$res = jvmCall('8,'.session_id());
if ($res == '1')
{
	session_unset();
	if ($_SERVER['REQUEST_METHOD'] === 'POST')//copy pasted during refactoring. double check if neccesary
	{
		Response::reload();
		Response::send(false);
		exit();
	}
}
if (isset($_REQUEST['SLEEPING_JAVA_OBJECTS']))
{
	foreach ($_REQUEST['SLEEPING_JAVA_OBJECTS'] as $o)
	{
		$objectsInSession = $objectsInSession.','.$o;
	}
	$res = jvmCall("5".$objectsInSession);
}
class java
{
	private $id;
	private $extra;

	public function __construct($class = null)
	{
		if ($class != null)
		{
			if (gettype($class) == 'string')
			{
				$this->id = jvmCall('0,'.$class);
			}
			else 
			{
				$this->id = jvmCall('1,'.$class->_getJavaObjectId());
			}
		}
	}

	public function __toString()
	{
		return $this->toString();
	}
	
	public function __call($name, $arguments)
	{
		//echo "Calling object method '$name' ". implode(', ', $arguments). "\n";
		//echo "Calling object method '$name' ". $arguments[0]."\n".gettype($arguments[0]);
		
		//echo 'instance method,'.$this->id.','.$name;
		foreach ($arguments as $a)
		{
			$aData = _objectToString($a);

			$data = $data.$aData;
			$offsets = $offsets.strlen($aData).',';
		}
		$res = jvmCall('2,'.$this->id.','.$name.','.sizeof($arguments).','.$offsets.$data);
		return _stringToObject($res);
	}
	
	public function _getJavaObjectId()
	{
		return $this->id;
	}
	
	public function _setJavaObjectId($id)
	{
		$this->id = $id;
	}
	
	public function __wakeup()
	{
		if (!isset($_REQUEST['SLEEPING_JAVA_OBJECTS']))
		{
			$_REQUEST['SLEEPING_JAVA_OBJECTS'] = array();
		}
		array_push($_REQUEST['SLEEPING_JAVA_OBJECTS'],$this->id);
	}
	
	function setExtra($e)
	{
		$this->extra = $e;
	}
	
	function getExtra()
	{
		return $this->extra;
	}
	
}

function java($class)
{
	$j = new java();
	$j->_setJavaObjectId(intval(jvmCall('3,'.$class)));
	return $j;
}

function java_is_null($val)//for compatibility with old skylin code that was written against the old java-php bridge
{
	return is_null($val);
}

function java_values($val)//for compatibility with old skylin code that was written against the old java-php bridge
{
	return $val;
}

function java_servlet_context()
{
	$j = new java();
	$j->_setJavaObjectId(intval(jvmCall('4')));
	return $j;
}

function java_bridge_session()
{
	$j = new java();
	$j->_setJavaObjectId(intval(jvmCall('a')));
	return $j;
}

function no_compromise()
{
	$t = new Label('Skylin. There will be no compromise.');
	$t->setStyle('margin: 5px');
	return $t;
}

function _stringToObject($str)
{
	$t = substr($str,0,1);
	$data = substr($str,1,strlen($str)-1);

	if ($t == 'a')
	{
		$ret = array();
		$sizeS = substr($data,0,strpos($data,','));
		$data = substr($data,strlen($sizeS)+1);
		$size = intval($sizeS);
		$split = explode(',',$data,$size+1);
		$data = $split[$size];
		$nextOffset = 0;
		for ($i = 0; $i < $size;$i = $i + 1)
		{
			$splitSize = $split[$i];
			array_push($ret,_stringToObject(substr($data,$nextOffset,$splitSize)));
			$nextOffset = $nextOffset + $splitSize;
		}
		return $ret;
	}
	if ($t == 's')
	{
		return $data;
	}
	if ($t == 'i')
	{
		return intval($data);
	}
	if ($t == 'd')
	{
		return floatval($data);
	}
	if ($t == 'b')
	{
		if ($data == 'T')
		{
			return true;
		}
		return false;
	}
	if ($t == 'o')
	{
		$j = new java();
		$j->_setJavaObjectId(intval($data));
		return $j;
	}
}

function _objectToString($a)
{
	$type = gettype($a);
	if ($type == 'string')
	{
		return's'.$a;
	}
	else if ($type == 'integer')
	{
		return 'i'.$a;
	}
	else if ($type == 'boolean')
	{
		if ($a)
		{
			return 'bT';
		}
		else
		{
			return 'bF';
		}
	}
	else if ($type == 'double')
	{
		return 'd'.$a;
	}
	else if ($type == 'array')
	{
		$size = 0;
		foreach ($a as $o)
		{
			$size = $size + 1;
			$str = _objectToString($o);
			$data = $data.$str;
			$sizes = $sizes.strlen($str).",";
		}
		if ($size == 0)
		{
			return "a0";
		}
		return "a".$size.",".$sizes.$data;
	}
	else if ($type == 'object')
	{
		if (get_class($a) != 'java')
		{
			if (get_class($a) == 'DefaultRow' || get_class($a) == 'DefaultView')
			{
				return 'n';
			}
			error_log("unsuported skylin bridge object argument class: ".get_class($a).'. All passed objects must be java objects.', 0);
			exit;
		}
		return 'o'.$a->_getJavaObjectId();
	}
	else if ($type == 'NULL')
	{
		return 'n';
	}
	else
	{
		error_log("unsuported skylin bridge argument type: ".$type, 0);
		exit;
	}
}

?>