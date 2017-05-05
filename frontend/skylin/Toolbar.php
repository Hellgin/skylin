<?php
require_once "Component.php";
class Toolbar extends Component
{
	private $tool;
	private $toolArray = array();
	private $toolItemArray = array();
	private $isObject;
	
	function render()
	{
		$ret = $ret.'<nav id="'.$this->getFullId().'" class="nav" style="'.$this->getStyle().'">'; 
		$ret = $ret.'<ul class="nav_ul">';
		foreach ($this->toolArray as $tool => $toolItems)
		{
	    	$ret = $ret.'<li class="nav_ul_li">';
	    	if (!is_string($toolItems) && !is_array($toolItems))
	    	{
	    		$com = $toolItems->renderInContext($this->getContext());
	    		if ($toolItems->getRendered())
	    		{
	    			$ret = $ret.'<div class="nav_ul_li_div">'.$com.'</div>';
	    		}
	    	}
	    	else
	    	{
	    		$ret = $ret.'<button class="nav_ul_li_button"><img src="skylin/images/black_arrow_down.png">'.$tool.'</button>';
	    		$ret = $ret.'<ul class="nav_ul_li_ul">';
	    		foreach($toolItems as $toolItem => $action)
	    		{
	    			if (ctype_xdigit($toolItem)) 
	    			{
	    				$ret = $ret.'<li><div>'.$action->renderInContext($this->getContext()).'</div></li>';
	    			}
	    			else
	    			{
	    				$ret = $ret.'<li><div class="nav_ul_li_ul_div" >'.$action->renderInContext($this->getContext()).'</div></li>';
	    			}
	    		}
	    		$ret = $ret.'</ul>';
	    	}
		    $ret = $ret.'</li>';
		}
		$ret = $ret.'</ul>';
	    $ret = $ret.'</nav>';
	    
	    unset($tool);
	    unset($toolItem);
	    
		return $ret;
	}
	
	function add($o)
	{
		return $this->setTool($o);
	}
	
	function setTool($o)
	{
		if (isset($o))
		{
			if(!is_string($o))
			{
				$id = $_SESSION['UTIL']->getSecureUnsignedNumberAsHexString(32);
				$this->toolArray[$id] = $o;
				$this->isObject = true;
				return $o;
			}
			else
			{
				$this->tool = $o;
				unset($this->toolItemArray);
				$this->isObject = false;
			}
		}		
	}
	
	function setToolItem($o, $a = null, $e = null)
	{
		if (isset($o) && is_string($a))
		{
			$id = $o;
			$action = new TextField();
			$action->link();
			$action->setValue($id);
			$action->setAction($a);
			
			if (isset($e))
			{
				$action->setEditableE($e);
			}
			else
			{
				$action->setEditable(true);
			}
		}
		else if (!is_string($o) && !isset($a))
		{
			$id = $_SESSION['UTIL']->getSecureUnsignedNumberAsHexString(32);
			$action = $o;
		}
		
		if(!$this->isObject && isset($action))
		{
			$this->toolItemArray[$id] = $action;
			$this->toolArray = array_merge($this->toolArray, array($this->tool=>$this->toolItemArray));
		}
	}
}
?>