<?php

class Component
{
	private $context = array();
	private $id;
	private $currentRow;//refactor out asap
	private $blockAction;
	private $style;
	private $styleE;
	private $extraClasses = '';
	private $titleE;
	private $customProperties = array();
	private $dynamicCustomProperties = array();
	private $registered = false;
	private $renderedE = 'return true;';
	private $rowVarName = 'row';
	private $afterRenderE;
	
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
			return '<div id="'.$this->getFullId().'" style="display:none"></div>';
		}
	}
	
	function refresh()
	{
		Response::addMessage('setdiv',$this->getFullId().'-'.$this->renderInContext($this->getContext()));
	}
	
	function refreshInRow($row)
	{
		if ($row == null)
		{
			return null;
		}
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
		if (strpos($id,'$') !== false)
		{
		    throw new Exception('Cannot use $ character in compoment id');
		}
		$this->id = $id;
		return $this;
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
		$this->renderedE = Expression::toExpression($r);
		return $this;
	}

	function setRendered($r)
	{
		$this->renderedE = 'return '.$r.';';
		return $this;
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
		return $this;
	}
	
	function setStyleE($s)
	{
		$this->styleE = Expression::toExpression($s);
		return $this;
	}

	/**
	 * Adds a CSS class (or several, separated by spaces) to the element the component renders, next to the framework's
	 * own class (`group`, `label`, `RoTextField`, `button`, ...). It is the hook for a look that the root
	 * application's stylesheet (setCss on the home screen) gives to a kind of element, instead of an inline style on every component. Class names are developer
	 * constants; anything but letters, digits, `_` and `-` is dropped, so the attribute stays safe. Used by Group,
	 * Label, TextField, Button, Table and TabStrip.
	 */
	function addClass($c)
	{
		$c = trim(preg_replace('/[^A-Za-z0-9_\- ]/', '', (string) $c));
		if ($c !== '')
		{
			$this->extraClasses = trim($this->extraClasses.' '.$c);
		}
		return $this;
	}

	/** The value of the class attribute: the component's own class $base followed by the added ones. */
	function getClasses($base = '')
	{
		return trim($base.' '.$this->extraClasses);
	}
	
	function addStyle($s)
	{
		$this->style = $this->style.';'.$s;
		return $this;
	}
	
	function getStyle()
	{
		if ($this->styleE != null)
		{
			return eval($this->styleE);
		}
		return $this->style;
	}
	
	/** The style set with setStyle()/addStyle(), without evaluating setStyleE() (which needs a row). */
	function getFixedStyle()
	{
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
		return $this;
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
		return 'title="'.self::escapeHtml(eval($this->titleE)).'"';
	}

	/**
	 * Text for HTML output: markup characters become entities (htmlspecialchars, UTF-8); other characters are
	 * written as they are. Same rule as the default of TextField.
	 */
	static function escapeHtml($value)
	{
		return htmlspecialchars((string) $value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
	}
	
	function setTitleE($t)
	{
		$this->titleE = Expression::toExpression($t);
		return $this;
	}
	
	function setProp($k,$p)
	{
		$this->customProperties[$k] = $p;
		$this->context[$k] = $p;
		return $this;
	}
	
	function setPropE($k,$p)
	{
		$this->dynamicCustomProperties[$k] = Expression::toExpression($p);	
		return $this;
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
		return $this;
	}
	
	function s()
	{
		$sec = $this->c('sec');
		if ($sec == null)
		{
			$sec = new java('com.distantmelody.framework.skylin.SecurityContext');
			/*
			$_SESSION[$sec->getId()] = $sec;
			if (function_exists('apc_store'))
			{
				apc_store($sec->getId().'',session_id());
			}
			*/
			$sec->setExtra($this);
			$this->setProp('sec', $sec);
			return $sec;
		}
		return $sec;
	}
	
	function forceNewSecurityContext()
	{
		$sec = new java('com.distantmelody.framework.skylin.SecurityContext');
		$sec->setExtra($this);
		$this->setProp('sec', $sec);
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
		$sec = $this->customProperties['sec'];
		$this->customProperties = array();
		$this->customProperties['sec'] = $sec;
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
		$this->afterRenderE = Expression::toExpression($e);
	}
	
	function com($c)
	{
		return $this->a()->com($c);
	}
	
	function getAM($n = null)
	{
		return $this->a()->getAM($n);
	}
	
	/*
	//deprecated. use getView
	function getViewObject($o)
	{
		return $this->a()->getAM()->getViewObject($o);
	}
	
	function getView($o)
	{
		return $this->a()->getAM()->getView($o);
	}*/
	
	function __call($name, $arg)
	{
	    //return $this->a()->getAM()->__call($name,$arg);
	    return $this->a()->__call($name,$arg);
	}
	
	function __get($name)
	{
	   return $this->a()->__get($name);
	}
	
	function refreshComs($coms)
	{
		$this->a()->refreshComs($coms);	
	}
	
	function refreshComsInRow($coms,$row)
	{
		$this->a()->refreshComsInRow($coms,$row);
	}
	
	function destroy()
	{
		
	}
	
	function scrollToBottom()
	{
		Response::addMessage('scrolltobottom',$this->getFullId());
	}
	
	function requestFullscreen()
	{
	    Response::addMessage('requestFullscreen',$this->getFullId());
	}
	
	function toggleFullscreen()
	{
	    Response::addMessage('toggleFullscreen',$this->getFullId());
	}
	
	function autoDetermineTableCol()
	{
		if ($this->c("skylin_col") != null && $this->c("col") != null && $this->c('skylin_isTableContent'))
		{
			$this->c("skylin_col")->setAutoDeterminedViewCol($this->c("col"));
		}
	}
}


?>