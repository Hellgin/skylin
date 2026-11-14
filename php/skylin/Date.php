<?php
require_once "TextField.php";
class Date extends TextField
{
	function render()
	{
		$this->autoDetermineTableCol();
		if (!is_null($this->rowE))
		{
			$this->setContextParam($this->getRowVarName(),eval($this->rowE));
		}
		$id = $this->getFullId();
		if ($this->getEditable())
		{
			$value = $this->getValue();
			$type = $this->getType($value);
			return '<div id = "'.$id.'" class="DateContainer">
					'.$this->getTipMarkup($id, $this->getValidationError()).'
					<input id = "'.$id.'_field" type="text" class="DateField" sk_type="'.$type.'" name="fname" value = "'.$value.'" '.$this->getStyleFull().'>

					</div>
							<button id="'.$id.'_calendarbutton" class="calendarButton"><img src="skylin/images/calendar.png"></button>
					</div>';

		}
		else
		{
			$value = $this->getValue();
			return '<div class = "RoTextField" id = "'.$id.'" '.$this->getStyleFull().'>'.$value.'</div>';
		}
	}
	
	function getValue()
	{
		$v = parent::getValue();
		if (is_null($v) || strlen($v) == 0)
		{
			return null;
		}
		return $_SESSION['UTIL']->dateToString($v,'dd-MMM-yyyy');
	}
	
	function valueChange($linkId,$value)
	{
		if (strlen($value) == 0)
		{
			parent::valueChange($linkId,null);
			return;
		}
		$v = $_SESSION['UTIL']->stringToDate($value);
		if (java_is_null($v))
		{
			$this->setRow($linkId);
			$this->setError("Invalid Date Format",$this->getFullId());
			return;
		}
		parent::valueChange($linkId,$v);
		if ($value != $this->getValue())
		{
			$this->refresh();
		}
	}
}

if (isset($_POST['changeDate']))
{
	require "InitRequest.php"; 
	$s = explode('x',$_POST['changeDate'],4);
	$t = explode(',',$s[3],2);
	$value = $t[1];
	$com = Application::getApplication($s[1])->getComponent($s[2]);
	$value = $com->getFilteredInput($value);
	$com->valueChange($t[0],$value);
	Response::send();
}
?>