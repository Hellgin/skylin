<?php
require_once "Group.php";
class IFrame extends Group
{
	private $url;
	private $onloadE;
	
	public function __construct($url)
	{
		$this->url = $url;
	}
	
	function setOnloadE($onloadE)
	{
	    $this->onloadE = Expression::toExpression($onloadE);
	    return $this;
	}
	
	function render()
	{
	    $ol = '';
	    if ($this->onloadE != null)
	    {
	        $ol = 'onload="'.eval($this->onloadE).'"';
	    }
		return '<div id="'.$this->getFullId().'" style="width:100%;height:100%"><iframe src="'.eval($this->url).'" seamless="seamless" frameBorder="0" style="width:100%;height:100%" '.$ol.'></iframe></div>';
	}
	
	function postMessage($message)
	{
	   Response::addMessage('postmessage',$this->getFullId().','.$message);
	}
}
?>