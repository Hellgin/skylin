<?php
require_once "TextField.php";
class Lov extends TextField
{
	private $lovE = 'return $this->r()->getLinkedView($this->c("col")."_lov");';
	//private $afterLovSelectE;
	
	function getLov()
	{
		return eval($this->lovE);
	}
	
	function selectRow($rowId,$lovRowId)
	{
		$this->setRow($rowId);
		
		if (!$this->getEditable() || !$this->getRendered())
		{
			return;
		}
		
		if (java_is_null($lovRowId))
		{
			$this->getLov()->selectRow(null);
		}
		else
		{
			$lov = $this->getLov();
			$lov->selectRow($lov->getRowByLinkId($lovRowId));
		}
		eval($this->getAfterValueChangeE());
	}	
	
	function setAfterLovSelectE($e)
	{
		//$this->afterLovSelectE = $e;
		$this->afterValueChangeE($e);
	}
}
?>