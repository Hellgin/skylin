<?php
require_once "Lov.php";
class ChoiceLov extends Lov
{
	function render()
	{
		$lov = $this->getLov();
		if ($lov == null)
		{
			return;
		}
		$rows = $lov->getRows();
		$fullId = $this->getFullId();
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
			if ((java_is_null($lovColValue) && java_is_null($this->getValue())) 
				|| (!java_is_null($lovColValue) && java_values($lovColValue == $this->getValue())))
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
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	$ss = explode('X',$_POST['selectChoiceLovRow'],2);
	$s = explode('x',$ss[0],4);
	Application::getApplication($s[1])->getComponent($s[2])->selectRow($s[3],$ss[1]);
	Response::send();
	}
?>