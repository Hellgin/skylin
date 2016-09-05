<?php

class Component
{
	protected $context = array();
	protected $id;
	protected $currentRow;//refactor out asap
	protected $blockAction;
	protected $style;
	protected $styleE;
	protected $titleE;
	protected $customProperties = array();
	protected $dynamicCustomProperties = array();
	protected $registered = false;
	protected $renderedE = 'return true;';
	protected $rowVarName = 'row';
	protected $afterRenderE;
	
	public function __construct() 
	{

	}
	
	function render()
	{

	}
	
	function renderInContext($context)
	{
		$this->context = $context;
		foreach($this->dynamicCustomProperties as $key => $dp)
		{
			$this->customProperties[$key] = eval($dp);
		}
		if (!empty($this->customProperties))
		{
			$this->context = array_merge($context,$this->customProperties);	
		}
		if (!is_null($this->blockAction))
		{
			$this->context['block_action'] = $this->blockAction;
		}
		if (!$this->registered)
		{
			if ($this->id  == null)
			{
				$this->id = $this->a()->getNextComponentId();
			}
			$this->a()->setComponent($this->id,$this);
			$this->registered = true;
		}
		if ($this->getRendered())
		{
			$ret = $this->render();
			if ($this->afterRenderE != null)
			{
				eval($this->afterRenderE);
			}
			return $ret;
		}
		else
		{
			return '<div id="'.$this->getFullId().'"></div>';
		}
	}
	
	function refresh()
	{
		Response::addMessage('setdiv',$this->getFullId().'-'.$this->renderInContext($this->getContext()));
	}
	
	function refreshInRow($row)
	{
		$this->setContextParam('row',$row);
		$this->refresh();
	}
	
	function getLinkId()
	{
		return $this->r()->getLinkId();
	}
	
	function getFullId()
	{
		return 'x'.$this->a()->getApplicationId().'x'.$this->getId().'x'.$this->getLinkId();
	}
	
	
	function setId($id)
	{
		if (strpos($id,'x') !== false || strpos($id,'X') !== false)
		{
			throw new Exception('The letter x in id\'s is reserved by skylin');
		}
		$this->id = $id;
	}
	
	function getId()
	{
		return $this->id;
	}
	
	function setCurrentRow($c)
	{
		$this->currentRow = $c;
	}
	
	function getCurrentRow()
	{
		return $this->currentRow;
	}
	
	
	function copyContextParam($from,$to)
	{
		$this->context[$to] = $this->context[$from];
	}
	
	function setContextParam($key,$value)
	{
		$this->context[$key] = $value;
	}
	
	function setContext($c)
	{
		$this->context = $c;
	}
	
	function setRenderedE($r)
	{
		$this->renderedE = $r;
	}
	
	function getRendered()
	{
		return eval($this->renderedE);
	}
	
	function getContext()
	{
		return $this->context;
	}

	
	function c($k = null)
	{
		if ($k == null)
		{
			return $this->context;
		}
		return $this->context[$k];
	}
	
	function r()
	{
		$ret = $this->context[$this->rowVarName];
		if (java_is_null($ret))
		{
			//return $this->c('defaultRow');
			return $_SESSION['DEFAULT_ROW'];
		}
		return $ret;
	}
	
	function a()
	{
		return $this->context['application'];
	}
	
	
	function __destruct() 
	{
		unset($_SESSION['COMPONANTS'][$this->getId()]);
    }
	
	function setBlockAction($value)
	{
		$this->blockAction = $value;
		$this->context['block_action'] = $value;
	}
	
	function setStyle($s)
	{
		$this->style = $s;
	}
	
	function setStyleE($s)
	{
		$this->styleE = $s;
	}
	
	function addStyle($s)
	{
		$this->style = $this->style.';'.$s;
	}
	
	function getStyle()
	{
		if ($this->styleE != null)
		{
			return eval($this->styleE);
		}
		return $this->style;
	}
	
	function getStyleFull()
	{
		if (is_null($this->getStyle()))
		{
			return '';
		}
		return 'style="'.$this->getStyle().'"';
	}
	
	function refreshStyle()
	{
		Response::addMessage('setattr',$this->getFullId().',style,'.$this->getStyle());
	}
	
	function refreshStyleInRow($row)
	{
		$this->setContextParam('row',$row);
		$this->refreshStyle();
	}
	
	function setTitle($t)
	{
		$this->titleE = 'return "'.$t.'";';
	}
	
	function getTitle()
	{
		if ($this->titleE == null)
		{
			return '';
		}
		return eval($this->titleE);
	}
	
	function getTitleFull()
	{
		if ($this->titleE == null)
		{
			return '';
		}
		return 'title="'.eval($this->titleE).'"';
	}
	
	function setTitleE($t)
	{
		$this->titleE = $t;
	}
	
	function setProp($k,$p)
	{
		$this->customProperties[$k] = $p;
		$this->context[$k] = $p;
	}
	
	function setPropE($k,$p)
	{
		$this->dynamicCustomProperties[$k] = $p;	
	}
	
	/*
	function swapWith($com)
	{
		$this->c('parent')->replaceComponent($this,$com);
	}
	*/
	
	function getRowVarName()
	{
		return $this->rowVarName;
	}
	
	function setRowVarName($name)
	{
		$this->rowVarName = $name;
	}
	
	function s()
	{
		$sec = $this->c('sec');
		if ($sec == null)
		{
			$sec = new java('skylin.SecurityContext');
			$_SESSION[$sec->getId()] = $sec;
			if (function_exists('apc_store'))
			{
				apc_store($sec->getId().'',session_id());
			}
			$sec->setExtra($this);
			$this->setProp('sec', $sec);
			//$_SESSION['LATEST_SECURITY_CONTEXT'] = $this->c('sec');
			return $sec;
		}
		return $sec;
	}
	function genToken()
	{
		$secureToken = $_SESSION['UTIL']->getSecureUnsignedNumberAsHexString(32);
		apc_store($secureToken, $this->s()->getId());
		return $_SESSION['UTIL']->getFrameworkParam('HOST').":".$_SESSION['UTIL']->getFrameworkParam('PORT').'?'.$secureToken.'?'.$this->c('TAB_STRIP_TAB_NO');
	}
	
	function handleExternalCommand($command)
	{
		$this->a()->handleExternalCommand($command);
	}
	
	function dropAllProperties()
	{
		$this->customProperties = array();
		$this->dynamicCustomProperties = array();
	}
	
	
	function focus()
	{
		Response::addMessage('focus',$this->getFullId());
	}
	
	function focusInRow($row)
	{
		$this->setContextParam('row',$row);
		$this->focus();
	}
	
	function afterRenderE($e)
	{
		$this->afterRenderE = $e;
	}
}


?>