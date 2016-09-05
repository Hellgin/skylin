<?php
	require_once "Group.php";
	class ModalPanel extends Group
	{
		function render()
		{
			return '<div id="'.$this->getFullId().'" class="modalDialog">'.parent::render().'</div>';
		}
		
		function open()
		{
			Response::addMessage('setattr',$this->getFullId().',class,modalDialog modalDialog-open');
		}
		
		function close()
		{
			Response::addMessage('setattr',$this->getFullId().',class,modalDialog');
		}
		
		function getRenderId()
		{
			return $this->getFullId().'_group';
		}
		
		function refreshAndOpen()
		{
			$this->refresh();
			$this->open();
		}
	}
?>