<?php
require_once "Component.php";
class Progress extends Component
{
	private $valueE = 'return $this->r()->getValue($this->c("col"));';
	private $maxE = 'return 100;';
	
	public function __construct($autoSet = null,$rowVarName = null) 
	{
		parent::__construct();
		if ($autoSet != null)
		{
			$this->setProp("col",$autoSet);
		}
		if ($rowVarName != null)
		{
			$this->setRowVarName($rowVarName);
		}
	}

	function render()
	{
		$max = eval($this->maxE);
		$value = eval($this->valueE);
		return '<progress id="'.$this->getFullId().'" '.$this->getStyleFull().' value="'.$value.'" max="'.$max.'"></progress>';
	}
	
	function setValueE($e)
	{
		$this->valueE = $e;
		return $this;
	}
	
	function setMaxE($e)
	{
		$this->maxE = $e;
		return $this;
	}
}
?>