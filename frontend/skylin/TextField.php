<?php

require_once "Component.php";
class TextField extends Component
{
	private $valueE = 'return $this->r()->getValue($this->c("col"));';
	private $valueUpdate = 'return $this->r()->setValueFromFrontEnd($this->c("col"),$this->getCurrentValue());';
	private $editableE = 'return java_values($this->r()->isUpdateable($this->c("col")));';
	private $currentValue;
	private $type = 'return $this->r()->getHtmlInputType($this->c("col"));';
	private $action;
	private $controlType = 0;
	private $rowE;
	private $actionOnValueChange = false;
	private $inputFilterE = 'return $this->c("INPUT_VALUE");';
	private $afterValueChangeE;
	private $password;
	private $minDecE = 'return -1;';
	
	public function __construct($autoSet = null,$rowVarName = null) 
	{
		parent::__construct();
		if ($autoSet != null)
		{
			$this->setProp("col",$autoSet);
		}
		if ($rowVarName != null)
		{
			$this->setRowVarName($rowVarName);
		}
	}
	

	
	function bindToFirst($view,$col)
	{
		$this->setProp('view',$view);
		$this->setProp("col",$col);
		$this->rowE = 'return $this->c("view")->getFirst();';
		
	}
	
	function bindToCurrent($view,$col)
	{
		$this->setProp('view',$view);
		$this->setProp("col",$col);
		$this->rowE = 'return $this->c("view")->getCurrent();';
	}
	
	function setValueE($value)
	{
		$this->valueE = $value;	
		$this->editableE = "return false;";
	}
	
	function setValue($value)
	{
		$this->setProp('staticValue',$value);
		$this->valueE = 'return $this->c("staticValue");';
		$this->editableE = "return false;";
	}
	
	function getValue()
	{
		return eval($this->valueE);
	}
	
	function setTypeE($value)
	{
		$this->type = $value;	
	}
	
	function getType()
	{
		return eval($this->type);
	}	
	
	function setEditableE($value)
	{
		$this->editableE = $value;
	}
	
	function setEditable($value)
	{
		$this->editableE = 'return '.$value.';';
	}
	
	function getEditable()
	{
		return eval($this->editableE);
	}
	
	function setSourceCheckE($value)
	{
		$this->sourceCheckE = $value;
	}
	
	function getSourceCheck()
	{
		return eval($this->sourceCheckE);
	}
	
	function setMinDecE($value)
	{
		$this->minDecE = $value;
	}
	
	function setMinDec($value)
	{
		$this->minDecE = 'return '.$value.';';
	}
	
	function getMinDecE()
	{
		return eval($this->minDecE);
	}
	
	function setValueUpdateE($v)
	{
		$this->valueUpdate = $v;
	}
	
	function setCurrentValue($v)
	{
		$this->currentValue = $v;
	}
	
	function getCurrentValue()
	{
		return $this->currentValue;
	}
	
	
	function render()
	{
		if (!is_null($this->rowE))
		{
			$this->setContextParam($this->getRowVarName(),eval($this->rowE));
		}
		/*
		if (java_is_null($this->r()))
		{
			$this->setContextParam($this->getRowVarName(),$this->c('defaultRow'));
		}
		*/
		//$this->setCurrentRow($this->r());
		if ($this->password) 
		{
			$htmlType = 'password';
		}
		$id = $this->getFullId();
		if ($this->controlType == 1)
		{
			$value = $this->getValue();
			if ($this->getEditable())
			{
				return '<div id="'.$id.'" class = "LinkTextField" '.$this->getStyleFull().'>'.$value.'</div>';
			}
			else 
			{
				return '<div id="'.$id.'" class = "LinkTextField" '.$this->getStyleFull().' disabled>'.$value.'</div>';
			}		
		}
		else if ($this->controlType == 2)
		{
			if ($this->getEditable())
			{
				$type = $this->getType();
				$value = $this->getValue();
				return '<div id = "'.$id.'_tip" class = "TextField_tip_disabled"><textarea class="TextFieldMulti" id = "'.$id.'" sk_type="'.$type.'" name="fname" '.$this->getStyleFull().'>'.$value.'</textarea></div>';
			}
			else
			{
				//$value = $this->getValue();
				//return '<div class = "RoTextField" id = "'.$id.'" '.$this->getStyleFull().'>'.$value.'</div>';
				$type = $this->getType();
				$value = $this->getValue();
				return '<div id = "'.$id.'_tip" class = "TextField_tip_disabled"><textarea readonly class="TextFieldMulti" id = "'.$id.'" sk_type="'.$type.'" name="fname" '.$this->getStyleFull().'>'.$value.'</textarea></div>';
			}
		}
		if ($this->getEditable())
		{
			$type = $this->getType();
			$value = $this->getValue();
			$style = $this->getStyle();
			if ($type == 'number')
			{	
				$style = $style.';text-align:right';
				$minDec = $this->getMinDecE();
				if ($minDec >= 0) 
				{
					$value = number_format ($value,$minDec,'.','');
				}		
			}
			return '<div id = "'.$id.'_tip" class = "TextField_tip_disabled"><input id = "'.$id.'" type="'.$htmlType.'" class="TextField" sk_type="'.$type.'" name="fname" value = "'.$value.'" style="'.$style.'"></div>';
		}
		else
		{
			$value = $this->getValue();
			$type = $this->getType();
			$style = $this->getStyle();
			if ($type == 'number')
			{	
				$style = $style.';text-align:right';
				$minDec = $this->getMinDecE();
				if ($minDec >= 0)
				{
					$value = number_format ($value,$minDec,'.','');
				}
			}
			return '<div class = "RoTextField" id = "'.$id.'" style="'.$style.'">'.$value.'</div>';
		}
	}
	
	function setRow($linkId)
	{
		if (!is_null($this->rowE))
		{
			$this->setContextParam('row',eval($this->rowE));
		}
		else
		{
			$this->setContextParam('row',$this->r()->getView()->getRowByLinkId($linkId));
		}
	}
	
	function valueChange($linkId,$value)
	{
		$this->setRow($linkId);
		
		if (!$this->getEditable() || !$this->getRendered())
		{
			return;
		}
			
		$this->setContextParam('OLD_VALUE',$this->getValue());
		
		$this->setCurrentValue($value);
		$result = eval($this->valueUpdate);
		$id = $this->getFullId();
		$this->setError($result,$id);
		$this->setContextParam('NEW_VALUE',$this->getValue());
		
		if ($result == null && $this->actionOnValueChange)
		{
			$this->click($linkId);
		}
		if ($result == null && isset($this->afterValueChangeE))
		{
			$this->setContextParam('row',$this->r()->getView()->getRowByLinkId($linkId));
			eval($this->afterValueChangeE);
		}
		

	}
	
	function setAction($a)//deprecated
	{
		$this->afterEnterKey($a);
	}
	
	function setActionE($a)
	{
		$this->afterEnterKey($a);
	}
	
	function afterEnterKey($a)
	{
		$this->action = $a;
	}
	
	function click($id)
	{
		$this->setContextParam('row',$this->r()->getView()->getRowByLinkId($id));
		if ($this->controlType == 1 && !$this->getEditable())
		{
			return;
		}
		eval($this->action);
	}
	
	function link()
	{
		$this->controlType = 1;
	}
	
	function actionOnValueChange()//deprecated
	{
		$this->actionOnValueChange = true;
	}
	
	function isActionOnValueChange()
	{
		return $this->actionOnValueChange;
	}
	
	function setAfterValueChange($e)//deprecated
	{
		$this->afterValueChange($e);
	}
	
	function afterValueChange($e)//deprecated
	{
		$this->afterValueChangeE = $e;
	}
	
	function afterValueChangeE($e)
	{
		$this->afterValueChangeE = $e;
	}
	
	function getAfterValueChangeE()
	{
		return $this->afterValueChangeE;
	}
	
	
	function setError($result,$id)
	{
		if (!java_is_null($result))
		{
			Response::addMessage('setprop',$id.'_tip,class,TextField_tip error');
			Response::addMessage('setattr',$id.'_tip,data-tip,'.$result);
			$ba = $this->c()['block_action'];
			if (!is_null($ba))
			{
				Response::addMessage('setattr',$id.'_tip,block_action,'.'x'.$this->a()->getApplicationId().'_'.$ba);
			}
			$this->setContextParam('ERROR',$result);
		}
		else
		{
			Response::addMessage('setprop',$id.'_tip,class,TextField_tip_disabled');
			Response::addMessage('setattr',$id.'_tip,block_action,');
			$this->setContextParam('ERROR',null);
		}		
	}
	function getFilteredInput($value)
	{
		$this->setContextParam('INPUT_VALUE',$value);
		return eval($this->inputFilterE);
	}
	
	function setInputFilterE($e)
	{
		$this->inputFilterE = $e;
	}
	
	function password()
	{
		$this->password = true;
	}
	
	function multiLine()
	{
		$this->controlType = 2;
	}
}

if (isset($_POST['change'])) 
{
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	$s = explode('x',$_POST['change'],4);
	$t = explode(',',$s[3],2);
	$value = $t[1];
	$com = Application::getApplication($s[1])->getComponent($s[2]);
	$value = $com->getFilteredInput($value);
	$com->valueChange($t[0],$value);
	Response::send();
}


if (isset($_POST['linkClick'])) 
{
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	$s = explode('x',$_POST['linkClick'],4);
	if (!Application::getApplication($s[1])->getComponent($s[2])->isActionOnValueChange())
	{
		Application::getApplication($s[1])->getComponent($s[2])->click($s[3]);	
	}	
	Response::send();
}
?>