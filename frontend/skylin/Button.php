<?php

//use Opis\Closure\SerializableClosure;

require_once "Component.php";
class Button extends Component
{
	private $action;
	//private $actionF;
	//private $actionF_ser;
	private $textE = 'return "";';
	private $actionName;
	private $editableE;
	private $imageE1;
	private $imageE2 = 'return "";';
	
	//deprecated
	function setAction($action)
	{
		$this->setActionE($action);
		return $this;
	}
	function setActionE($action)
	{
		$this->action = $action;
		return $this;
	}
	/*
	function setActionF($action)
	{
		$this->actionF_ser = serialize(new SerializableClosure($action));
	}*/

	function setActionName($actionName)
	{
		$this->actionName = $actionName;
		return $this;
	}
	
	function setEditableE($e)
	{
		$this->editableE = $e;
		return $this;
	}
	
	function getEditable()
	{
		return is_null($this->editableE) || eval($this->editableE);
	}
	
	function render()
	{
		$this->setCurrentRow($this->r());
		$text = $this->getText();
		$image = $this->getImage();
		if (strlen($image) > 0)
		{
			$class = 'button_image';
		}
		else
		{
			$class = 'button';
		}
		
		$style = $this->getStyle();
		if (!is_null($this->actionName))
		{
			$an = 'action_name = "x'.$this->a()->getApplicationId().'_'.$this->actionName.'"';
		}
		if (!is_null($this->editableE) && !eval($this->editableE))
		{
			$dis = 'disabled';
		}
		/*
		if (!is_null($this->getStyle()))
		{
			$style =$style.';'.$this->getStyle();
		}
		*/

		if (strlen($text) > 0)
		{
			return '<button id="x'.$this->a()->getApplicationId().'x'.$this->getId().'x'.$this->r()->getLinkId().'" '.$an.$dis.' type="button" style="'.$style.'" class = "'.$class.'" '.$this->getTitleFull().'>'.$text.'</button>';		
		}
		else
		{
			return '<button id="x'.$this->a()->getApplicationId().'x'.$this->getId().'x'.$this->r()->getLinkId().'" '.$an.$dis.' type="button" style="'.$style.'" class = "'.$class.'" '.$this->getTitleFull().'><img src="'.$this->getImage().'"></button>';
		}
	}
	
	function getStyle()
	{
		$text = $this->getText();
		$image = $this->getImage();
		if (strlen($text) > 0 && strlen($image))
		{
			$style = 'background-image:url('.$image.');';
		}
		if (strlen($image) > 0)
		{
			//$class = 'button_image';
		}
		else
		{
			//$class = 'button';
			$style = $style.$this->c('SKYLIN_BUTTON_STYLE');
		}
		
		return $style.parent::getStyle();
	}
	
			

	
	function click($id)
	{
		$this->setContextParam('row',$this->r()->getView()->getRowByLinkId($id));
		if ((!is_null($this->editableE) && !eval($this->editableE)) || !$this->getRendered())
		{
			return;
		}
		if ($this->action != null)
		{
			eval($this->action);
		}
		/*
		else 
		{
			if ($this->actionF == null)
			{
				$this->actionF = unserialize($this->actionF_ser)->getClosure()->bindTo($this,$this);
			}
			$c = $this->actionF;
			$c();
		}*/
	}
	
	/*
	function __sleep()
	{
		$vars = get_object_vars($this);
		unset($vars['actionF']);
		return array_keys($vars);
	}
	*/
	
	function setText($t)
	{
		$this->textE = 'return "'.$t.'";';
		return $this;
	}
	
	function setTextE($t)
	{
		$this->textE = $t;
		return $this;
	}
	
	function getText()
	{
		return eval($this->textE);
	}
	
	function getImage()
	{
		if (!is_null($this->imageE1))
		{
			$ret = eval($this->imageE1);
		}
		return $ret.eval($this->imageE2);
	}

	function setImage($path)
	{
		$this->imageE1 = null;
		//$this->imageE2 = 'return "'.$_SESSION['WEB_APP'].$path.'";';
		$this->imageE2 = 'return "'.$path.'";';
		return $this;
	}
	
	function setImageRel($path)
	{
		$this->imageE1 = null;
		$this->imageE2 = 'return $this->a()->getDir()."/'.$path.'";';	
		return $this;
	}
	
	function setImageRelE($pathE)
	{
		$this->imageE1 = 'return $this->a()->getDir()."/";';
		$this->imageE2 = $pathE;	
		return $this;
	}
}

if (isset($_POST['click'])) 
{
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	$s = explode('x',$_POST['click'],4);
	Application::getApplication($s[1])->getComponent($s[2])->click($s[3]);
	Response::send();
}



?>