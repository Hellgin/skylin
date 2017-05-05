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
		return $this;
		
	}
	
	function bindToCurrent($view,$col)
	{
		$this->setProp('view',$view);
		$this->setProp("col",$col);
		$this->rowE = 'return $this->c("view")->getCurrent();';
		return $this;
	}
	
	function setValueE($value)
	{
		$this->valueE = $value;	
		$this->editableE = "return false;";
		return $this;
	}
	
	function setValue($value)
	{
		$this->setProp('staticValue',$value);
		$this->valueE = 'return $this->c("staticValue");';
		$this->editableE = "return false;";
		return $this;
	}
	
	function getValue()
	{
		return eval($this->valueE);
	}
	
	function setTypeE($value)
	{
		$this->type = $value;	
		return $this;
	}
	
	function getType()
	{
		return eval($this->type);
	}	
	
	function setEditableE($value)
	{
		$this->editableE = $value;
		return $this;
	}
	
	function setEditable($value)
	{
		$this->editableE = 'return '.$value.';';
		return $this;
	}
	
	function getEditable()
	{
		return eval($this->editableE);
	}
	
	function setSourceCheckE($value)
	{
		$this->sourceCheckE = $value;
		return $this;
	}
	
	function getSourceCheck()
	{
		return eval($this->sourceCheckE);
	}
	
	function setMinDecE($value)
	{
		$this->minDecE = $value;
		return $this;
	}
	
	function setMinDec($value)
	{
		$this->minDecE = 'return '.$value.';';
		return $this;
	}
	
	function getMinDecE()
	{
		return eval($this->minDecE);
	}
	
	function setValueUpdateE($v)
	{
		$this->valueUpdate = $v;
		return $this;
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
		if ($this->c("skylin_draggableContainer") != null)
		{
			$draggableContainer = 'draggablecontainer='.$this->c("skylin_draggableContainer");
		}
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
				return '<div id = "'.$id.'_tip" class = "TextField_tip_disabled"><textarea class="TextFieldMulti" id = "'.$id.'" sk_type="'.$type.'" name="fname" '.$this->getStyleFull().' '.$draggableContainer.'>'.$value.'</textarea></div>';
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
				$style = 'text-align:right;'.$style;
				$minDec = $this->getMinDecE();
				if ($minDec >= 0) 
				{
					$value = number_format ($value,$minDec,'.','');
				}		
			}
			$classes = "TextField";
			if($this->c("skylin_focusEdit"))
			{
				$classes = $classes.' TextField_focusedit';
			}
			
			if ($_REQUEST["SKYLIN_VALIDATE"])
			{
				$error = $this->r()->getCurrentValueValidation($this->c("col"));
				if ($error != null)
				{
					return '<div id = "'.$id.'_tip" class = "TextField_tip error" data-tip="'.$error.'"><input id = "'.$id.'" type="'.$htmlType.'" class="'.$classes.'" sk_type="'.$type.'" name="fname" value = "'.$value.'" style="'.$style.'" '.$draggableContainer.'></div>';
				}
			}
			return '<div id = "'.$id.'_tip" class = "TextField_tip_disabled"><input id = "'.$id.'" type="'.$htmlType.'" class="'.$classes.'" sk_type="'.$type.'" name="fname" value = "'.$value.'" style="'.$style.'" '.$draggableContainer.'></div>';
		}
		else
		{
			$value = $this->getValue();
			$type = $this->getType();
			$style = $this->getStyle();
			if ($type == 'number')
			{	
				$style = 'text-align:right;'.$style;
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
		if ($this->valueChange_step1($linkId,$value))
		{
			$this->valueChange_step2($linkId,$value);
		}
	}
	
	function valueChange_step1($linkId,$value)
	{
		$this->setRow($linkId);
		
		if (!$this->getEditable() || !$this->getRendered())
		{
			return false;
		}
			
		$this->setContextParam('OLD_VALUE',$this->getValue());
		
		$this->setCurrentValue($value);
		$result = eval($this->valueUpdate);
		$id = $this->getFullId();
		$this->setError($result,$id);
		$this->setContextParam('NEW_VALUE',$this->getValue());
		
		return $result == null;
	}
	
	function valueChange_step2($linkId,$value)
	{
		if ($this->actionOnValueChange)
		{
			$this->click($linkId);
		}
		if (isset($this->afterValueChangeE))
		{
			$this->setContextParam('row',$this->r()->getView()->getRowByLinkId($linkId));
			eval($this->afterValueChangeE);
		}	
	}
	
	function setAction($a)//deprecated
	{
		return $this->afterEnterKey($a);
	}
	
	function setActionE($a)
	{
		return $this->afterEnterKey($a);
	}
	
	function afterEnterKey($a)
	{
		$this->action = $a;
		return $this;
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
		return $this;
	}
	
	function actionOnValueChange()//deprecated
	{
		$this->actionOnValueChange = true;
		return $this;
	}
	
	function isActionOnValueChange()
	{
		return $this->actionOnValueChange;
	}
	
	function setAfterValueChange($e)//deprecated
	{
		$this->afterValueChange($e);
		return $this;
	}
	
	function afterValueChange($e)//deprecated
	{
		$this->afterValueChangeE = $e;
		return $this;
	}
	
	function afterValueChangeE($e)
	{
		$this->afterValueChangeE = $e;
		return $this;
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
		return $this;
	}
	
	function password()
	{
		$this->password = true;
		return $this;
	}
	
	function multiLine()
	{
		$this->controlType = 2;
		return $this;
	}
}

if (isset($_POST['change'])) 
{
	require_once $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	$s = explode('x',$_POST['change'],4);
	$t = explode(',',$s[3],2);
	$value = $t[1];
	$com = Application::getApplication($s[1])->getComponent($s[2]);
	$value = $com->getFilteredInput($value);
	$com->valueChange($t[0],$value);
	if (!isset($_POST['linkClick']))
	{
		Response::send();
	}
}


if (isset($_POST['linkClick'])) 
{
	require_once $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	$s = explode('x',$_POST['linkClick'],4);
	if (!Application::getApplication($s[1])->getComponent($s[2])->isActionOnValueChange())
	{
		Application::getApplication($s[1])->getComponent($s[2])->click($s[3]);	
	}	
	Response::send();
}
?>