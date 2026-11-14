<?php

require_once "Component.php";
class TextField extends Component
{
	private $valueE = 'return $this->r()->getValue($this->c("col"));';
	private $valueUpdate = 'return $this->r()->setValueFromFrontEnd($this->c("col"),$this->getCurrentValue());';
	private $editableE = 'return $this->r()->isUpdateable($this->c("col"));';
	private $currentValue;
	private $type = 'return $this->r()->getHtmlInputType($this->c("col"));';
	private $action;
	private $controlType = 0;
	protected $rowE;
	private $actionOnValueChange = false;
	private $inputFilterE = 'return $this->c("INPUT_VALUE");';
	private $afterValueChangeE;
	private $password;
	private $minDecE = 'return -1;';
	private $placeholder;
	private $placeValueCommas;
	private $escape = true;
	
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
		$this->valueE = Expression::toExpression($value);	
		$this->type = 'if ($value !== null && !is_string($value) && is_numeric($value)) return "number";return "text";';
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
		$this->type = Expression::toExpression($value);	
		return $this;
	}
	
	function getType($value)
	{
		return eval($this->type);
	}	
	
	function setEditableE($value)
	{
		$this->editableE = Expression::toExpression($value);
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
		$this->sourceCheckE = Expression::toExpression($value);
		return $this;
	}
	
	function getSourceCheck()
	{
		return eval($this->sourceCheckE);
	}
	
	function setMinDecE($value)
	{
		$this->minDecE = Expression::toExpression($value);
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
		$this->valueUpdate = Expression::toExpression($v);
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

	/**
	 * Turns the output escaping of this field off, for the rare field whose value is HTML that was written by the
	 * developer (never for data from the database or from users).
	 *
	 * By default a TextField escapes its value with htmlspecialchars(ENT_QUOTES), so a value such as "<b>x</b>" is
	 * shown as text. Characters outside ASCII are written as they are (UTF-8, see "Unicode" in
	 * building-a-screen.md). It applies to every place the value is written (read-only text, link, textarea, the
	 * value attribute of an input).
	 */
	function noEscape()
	{
		$this->escape = false;
		return $this;
	}

	// The value as written into the markup: escaped, unless noEscape() was called.
	private function markup($value)
	{
		if (!$this->escape)
		{
			return $value;
		}
		return htmlspecialchars((string) $value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
	}

	function setPlaceholder($value)
	{
		$this->placeholder = $value;
	}
	
	function getPlaceholder()
	{
		return $this->placeholder;
	}
	
	// While validate() is active (the screen calls it before drawing the page again after a failed save), the text of
	// the error the row has for this field, as safe markup for an attribute; null when there is none or validate() is
	// not active. Used by every editable branch of the TextField and by the Date.
	protected function getValidationError()
	{
		if ($_REQUEST["SKYLIN_VALIDATE"])
		{
			$col = $this->c("col");
			if ($col != null)
			{
				$error = $this->r()->getCurrentValueValidation($col);
				if ($error != null)
				{
					return htmlspecialchars((string) $error, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
				}
			}
		}
		return null;
	}

	// The tip around an editable field: the red frame with the error as tooltip, or the plain tip.
	protected function getTipMarkup($id, $error)
	{
		if ($error != null)
		{
			return '<div id = "'.$id.'_tip" class = "TextField_tip error" data-tip="'.$error.'">';
		}
		return '<div id = "'.$id.'_tip" class = "TextField_tip_disabled">';
	}

	/** Class for a number field's alignment. The stylesheet decides the side; a theme can override it. */
	private function alignClass($type)
	{
		if ($type == 'number')
		{
			return 'number-align';
		}
		if ($type == 'number_center')
		{
			return 'number-align-center';
		}
		return '';
	}

	function render()
	{
		$this->autoDetermineTableCol();
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
		if ($this->getPlaceValueCommas() != null)
		{
		    $placeValueCommas = ' place_value_commas="'.$this->getPlaceValueCommas().'"';
		    $type = $this->getType($dummy);
		    if ($type != 'number' && $type != 'number_center')
		    {
		        $this->type = 'return "number";';
		    }
		}
		if ($this->controlType == 1)
		{
			$value = $this->getValue();
			if ($this->getEditable())
			{
				return '<div id="'.$id.'" class = "'.$this->getClasses('LinkTextField').'" '.$this->getStyleFull().'>'.$this->markup($value).'</div>';
			}
			else 
			{
				return '<div id="'.$id.'" class = "'.$this->getClasses('LinkTextField').'" '.$this->getStyleFull().' disabled>'.$this->markup($value).'</div>';
			}		
		}
		else if ($this->controlType == 2)
		{
			if ($this->getEditable())
			{
				$value = $this->getValue();
				$type = $this->getType($value);
				return $this->getTipMarkup($id, $this->getValidationError()).'<textarea class="'.$this->getClasses('TextFieldMulti').'" id = "'.$id.'" sk_type="'.$type.'" name="fname" '.$this->getStyleFull().' '.$draggableContainer.$placeValueCommas.'>'.$this->markup($value).'</textarea></div>';
			}
			else
			{
				//$value = $this->getValue();
				//return '<div class = "'.$this->getClasses('RoTextField').'" id = "'.$id.'" '.$this->getStyleFull().'>'.$value.'</div>';
				$value = $this->getValue();
				$type = $this->getType($value);
				return '<div id = "'.$id.'_tip" class = "TextField_tip_disabled"><textarea readonly class="'.$this->getClasses('TextFieldMulti').'" id = "'.$id.'" sk_type="'.$type.'" name="fname" '.$this->getStyleFull().'>'.$this->markup($value).'</textarea></div>';
			}
		}
		if ($this->getEditable())
		{
			$value = $this->getValue();
			$type = $this->getType($value);
			$align = $this->alignClass($type);
			$style = $this->getStyle();
			$placeholder = $this->getPlaceholder();
			
			if ($type == 'number')
			{
				$minDec = $this->getMinDecE();
				if ($minDec >= 0) 
				{
					$value = number_format ($value,$minDec,'.','');
				}
				if ($this->getPlaceValueCommas() != null)$value = $this->decimalFormat($value,$this->getPlaceValueCommas());
			}
			else if ($type == 'number_center')
			{
				$minDec = $this->getMinDecE();
				if ($minDec >= 0)
				{
					$value = number_format ($value,$minDec,'.','');
				}
				if ($this->getPlaceValueCommas() != null)$value = $this->decimalFormat($value,$this->getPlaceValueCommas());
				$type = 'number';
			}
			$classes = $this->getClasses("TextField ".$align);
			if($this->c("skylin_focusEdit"))
			{
				$classes = $classes.' TextField_focusedit';
			}
			
			return $this->getTipMarkup($id, $this->getValidationError()).'<input id = "'.$id.'" type="'.$htmlType.'" class="'.$classes.'" sk_type="'.$type.'" name="fname" value = "'.$this->markup($value).'" style="'.$style.'" '.'" placeholder="'.$placeholder.'" '.$draggableContainer.$placeValueCommas.'></div>';
		}
		else
		{
			$value = $this->getValue();
			$type = $this->getType($value);
			$align = $this->alignClass($type);
			$style = $this->getStyle();
			if ($type == 'number')
			{
				$minDec = $this->getMinDecE();
				if ($minDec >= 0)
				{
					$value = number_format ($value,$minDec,'.','');
				}
				if ($this->getPlaceValueCommas() != null)$value = $this->decimalFormat($value,$this->getPlaceValueCommas());
			}
			else if ($type == 'number_center')
			{
				$minDec = $this->getMinDecE();
				if ($minDec >= 0)
				{
					$value = number_format ($value,$minDec,'.','');
				}
				if ($this->getPlaceValueCommas() != null)$value = $this->decimalFormat($value,$this->getPlaceValueCommas());
				$type = 'number';
			}
			return '<div class = "'.$this->getClasses('RoTextField '.$align).'" id = "'.$id.'" style="'.$style.'">'.$this->markup($value).'</div>';
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
	    if (strlen($value) == 0)$value=null;
		if ($this->valueChange_step1($linkId,$value))
		{
			$this->valueChange_step2($linkId,$value);
			
			$tableCol = $this->c('skylin_col');
			if ($tableCol != null)
			{
				$footer = $tableCol->getFooter();
				if ($footer->getFooter() != null)
				{
					$footer->refresh();
				}	
			}
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
		$this->setError($result[1],$id);
		$this->setContextParam('NEW_VALUE',$this->getValue());
		
		return $result[1] == null || $result[0];
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
		return $this->setActionE($a);
	}
	
	function setActionE($a)
	{
		return $this->afterEnterKey(Expression::toExpression($a));
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
		$this->afterValueChangeE = Expression::toExpression($e);
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
		$this->inputFilterE = Expression::toExpression($e);
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
	
	function getPlaceValueCommas()
	{
	    return $this->placeValueCommas;
	}
	
	function setPlaceValueCommas($amount)
	{
	    $this->placeValueCommas = $amount;
	    return $this;
	}
	
	function decimalFormat($numberString,$amount)
	{
	    if (strpos($numberString, '.') !== false)
	    {
	        $v = explode('.',$numberString);
	        $numberString = $this->decimalFormatInternal($v[0],$amount).".".$v[1];
	    }
	    else
	    {
	       $numberString = $this->decimalFormatInternal($numberString,$amount);
	    }
	    return $numberString;
	}
	
	function decimalFormatInternal($numberString,$amount)
	{
	    $count = 0;
	    for ($i = strlen($numberString)-1;$i > 0;$i = $i - 1)
	    {
	        $count = $count + 1;
	        if ($count == $amount)
	        {
	            $count = 0;
	            $numberString = substr($numberString,0, $i) . "," . substr($numberString, $i);
	        }
	    }
	    return $numberString;
	}
}

if (isset($_POST['change'])) 
{
	require_once "InitRequest.php"; 
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
	require_once "InitRequest.php";
	$s = explode('x',$_POST['linkClick'],4);
	if (!Application::getApplication($s[1])->getComponent($s[2])->isActionOnValueChange())
	{
		Application::getApplication($s[1])->getComponent($s[2])->click($s[3]);	
	}	
	Response::send();
}
?>