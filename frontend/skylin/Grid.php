<?php
	require_once "Component.php";	
	class Grid extends Component
	{
		private $children = array();
		private $collStyles = array();
		private $rowStyles = array();
		private $allStyles;
		private $x;
		private $y;
	
		private $vertAlign = 'middle';
		
		public function __construct($x = null,$y = null) 
		{
			parent::__construct();
			if (!is_null($x) && !is_null($y))
			{
				$this->setSize($x,$y);
			}
		}
	
		function render()
		{
			$i = 0;
			$ret=$ret.'<div class="grid" '.$this->getStyleFull().' id="'.$this->getFullId().'">';
			for ($y = 0; $y < $this->y; $y++) 
			{
				$ret=$ret.'<div style="display: table-row;">';
				for ($x = 0; $x < $this->x; $x++) 
				{
					$style = 'display: table-cell;vertical-align: '.$this->vertAlign;
					
					$style = $style.';'.$this->allStyles;
					
					if (isset($this->collStyles[$x]))
					{
						$style = $style.';'.$this->collStyles[$x];
					}
					if (isset($this->rowStyles[$y]))
					{
						$style = $style.';'.$this->rowStyles[$y];
					}
					
					$ret=$ret.'<div style="'.$style.'">';
					if (!is_null($this->children[$i]))
					{
						$ret = $ret.$this->children[$i]->renderInContext($this->getContext());
						$i++;
					}
					$ret=$ret.'</div>';
				} 
				$ret=$ret.'</div>';
			} 
			$ret=$ret.'</div>';
			return $ret;
		}
		
		function add($com)
		{
			array_push($this->children,$com);
			return $com;
		}
		
		function setSize($x,$y)
		{
			$this->x = $x;
			$this->y = $y;
			return $this;
		}
		
		function setColStyle($i,$s)
		{
			$this->collStyles[$i] = $s;
			return $this;
		}
		
		function setRowStyle($i,$s)
		{
			$this->rowStyles[$i] = $s;
			return $this;
		}
		
		function setAllStyle($s)
		{
			$this->allStyles = $s;
			return $this;
		}
		
		function vertAlignAll($w)
		{
			$this->vertAlign = $w;
			return $this;
		}
		
		function getLast()
		{
			return $this->children[sizeof($this->children)-1];
		}
		
		function getChildren()
		{
			return $this->children;
		}
	}

?>