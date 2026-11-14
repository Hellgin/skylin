<?php
require_once "Lov.php";
class ChoiceLov extends Lov
{
	function render()
	{
		$this->autoDetermineTableCol();
		$lov = $this->getLov();
		if ($lov == null)
		{
			return;
		}
		if ($lov->executeQueryOnOpen())
		{
		    $lov->executeQuery();
		}
		$rows = $lov->getRows();
		$fullId = $this->getFullId();
		$value = $this->getValue();
		$render = '';
		$selectedDisplay = null;
		foreach($rows as $row)
		{
			$display = null;
			foreach($lov->getDisplayColumns() as $col)
			{
				if ($display != null)
				{
					$display = $display.$lov->getSeperator();
				}
				$display=$display.$row->getValue($col->getDisplayColumn());
			}
			$selected = null;
			$lovColValue = $row->getValue($this->c('col'));
			if ((is_null($lovColValue) && is_null($value)) 
			    || (!is_null($lovColValue) && !is_null($value) && $lovColValue == $value))
			{
				$selected='selected';
				$selectedDisplay = $display;
				
			}
			$render=$render.'<option value="'.$fullId.'X'.$row->getLinkId().'" '.$selected.'>'.$display.'</option>';
		}
		
		if (!$this->getEditable())
		{
			return '<div id="'.$fullId.'" class="label" '.$this->getStyleFull().'>'.$selectedDisplay.'</div>';
		}
		else if (is_null($selectedDisplay))
		{
			$render='<option class="blank" selected></option>'.$render;
		}
		
		return 	'<select id="'.$fullId.'" class="choiceLov" '.$this->getStyleFull().'>'
					.$render.
				'</select>';
	}
}
if (isset($_POST['selectChoiceLovRow'])) 
{
	require "InitRequest.php"; ;
	$ss = explode('X',$_POST['selectChoiceLovRow'],2);
	$s = explode('x',$ss[0],4);
	Application::getApplication($s[1])->getComponent($s[2])->selectRow($s[3],$ss[1]);
	Response::send();
	}
?>