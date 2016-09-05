<?php

require_once "Component.php";
class Label extends Component
{
	private $text;
	public function __construct($text) 
	{
		parent::__construct();
		$this->text = $text;
	}
	
	function render()
	{
		/*
		if (java_is_null($this->r()))
		{
			$this->setContextParam($this->getRowVarName(),$this->c('defaultRow'));
		}
		*/
		return '<div id="'.$this->getFullId().'" class="label" '.$this->getStyleFull().'>'.$this->text.'</div>';
	}
}

?>