<?php
	require_once "Group.php";
    class Iter extends Group
    {
		private $rows;
		private $rowsE;
		private $viewE;
		private $view;
	  
		function setRows($r)
		{
		  $this->rows = $r;
		}
		
		function setRowsE($r)
		{
			$this->rowsE = $r;
		}
		
		function setViewE($v)
		{
			$this->viewE = $v;
		}
		
		function setView($v)
		{
			$this->view = $v;
		}
		
		function render()
		{
			if (!is_null($this->view))
			{
				$this->rows = $this->view->getRows();
			}
			else if (!is_null($this->viewE))
			{
				$view = eval($this->viewE);
				if ($view != null)
				{
					$this->rows = $view->getRows();
				}
				else 
				{
					return '';
				}
			}
			else if (!is_null($this->rowsE))
			{
				$this->rows = eval($this->rowsE);
			}
			$ret = '<div id="'.$this->getFullId().'">';
			foreach($this->rows as $row)
			{
				//$this->setContextParam('row',$row);
				$c = $this->getContext();
				$c['row'] = $row;
				$ret = $ret.parent::render($c);
			}
			$ret = $ret.'</div>';
			return $ret;
		}
		
		function getRenderId()
		{
			return '';
		}
	}
?>