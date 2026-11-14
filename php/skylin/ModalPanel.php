<?php
	require_once "Group.php";
	class ModalPanel extends Group
	{
		private $startOpen = false;

		/** Open on the first render only. The flag is stored with the component, so a later refresh stays closed. */
		function startOpen()
		{
			$this->startOpen = true;
			return $this;
		}

		function render()
		{
			$class = 'modalDialog';
			if ($this->startOpen)
			{
				$class = 'modalDialog modalDialog-open';
				$this->startOpen = false;
			}
			return '<div id="'.$this->getFullId().'" class="'.$class.'">'.parent::render().'</div>';
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