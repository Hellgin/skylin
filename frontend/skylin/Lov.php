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
		/*
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
		*/
	/*
		$lov = $this->getLov();
		if ($this->valueChange_step1($rowId,$lov->getRowByLinkId($lovRowId)->getValue($this->c('col'))))
		{
			$lov->selectRowSetOtherValues($this->c('col'),$lov->getRowByLinkId($lovRowId));
			$this->valueChange_step2($rowId);
		}
	*/
		$lov = $this->getLov();
		$this->valueChange($rowId,$lov->getRowByLinkId($lovRowId)->getValue($this->c('col')));

	}

	function valueChange_step2($rowId,$value)
	{
		$lov = $this->getLov();
		$lov->selectRowSetOtherValues($this->c('col'),$lov->findRowByUserInput($this->c('col'),$value));
		return parent::valueChange_step2($rowId,$value);
	}
	
	function setAfterLovSelectE($e)
	{
		//$this->afterLovSelectE = $e;
		$this->afterValueChangeE($e);
	}
}
?>