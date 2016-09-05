<?php
require_once "Group.php";
class IFrame extends Group
{
	private $url;
	
	public function __construct($url)
	{
		$this->url = $url;
	}
	
	function render()
	{
		return '<div id="'.$this->getFullId().' style="width:100%;height:100%"><iframe src="'.eval($this->url).'" seamless="seamless" frameBorder="0" style="width:100%;height:100%"></iframe></div>';
	}
}
?>