<?php
	require_once "TextField.php";
	class Checkbox extends TextField
	{
		function render()
		{
			if (!is_null($this->rowE))
			{
				$this->setContextParam('row',eval($this->rowE));
			}
			/*
			if (java_is_null($this->c('row')))
			{
				$this->setContextParam('row',$this->c('defaultRow'));
			}
			*/
			$id = $this->getFullId();
			if ($this->getValue() == 'Y')
			{
				$checked = 'checked="true"';
			}
			if (!$this->getEditable())
			{
				$disabled = 'disabled';
			}
			
			return '<input id="'.$this->getFullId().'" class="check" type="checkbox" '.$checked.' '.$disabled.'>';
		}
		
		function click($linkId)
		{
			$this->setRow($linkId);
			if ($this->getValue() == 'Y')
			{
				$this->valueChange($linkId,'N');
			}
			else
			{
				$this->valueChange($linkId,'Y');
			}
		}
		
		
	}
	
	if (isset($_POST['checkBoxClick'])) 
	{
		require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php"; 
		$s = explode('x',$_POST['checkBoxClick'],4);
		Application::getApplication($s[1])->getComponent($s[2])->click($s[3]);
		Response::send();
	}

?>