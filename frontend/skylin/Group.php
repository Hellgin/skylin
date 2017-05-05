<?php
	require_once "Component.php";
	class Group extends Component
	{
		private $children = array();
		private $text;
		private $innerMargin;
		private $type = 'div';
		private $afterClickE;
		private $clickable;
		
		private $otherContext;
		
		function renderInOtherContext($c)
		{
			$this->otherContext = $c;
			return $this->renderGroup();
		}
		
		function render()
		{
			return $this->renderGroup();
		}
		
		function renderGroup()
		{
			if (is_null($this->otherContext))
			{
				$context = $this->getContext();
			}
			else
			{
				$context = $this->otherContext;
				$this->otherContext = null;
			}
			/*
			if (function_exists ('java_is_null') && java_is_null($this->c('row')))
			{
				$this->setContextParam('row',$this->c('defaultRow'));
			}
			*/
			$context['parent'] = $this;
			if (!is_null($this->getStyle()))
			{
				$extra = $extra.'style="'.$this->getStyle().'"';
			}
			if ($this->clickable)
			{
				$clickable = ' clickable';
			}
			$ret = '<'.$this->type.' class="group" id="'.$this->getRenderId().'" '.$extra.$clickable.'>';
			if (!is_null($this->innerMargin))
			{
				$ret = $ret.'<div style="margin: '.$this->innerMargin.';">';
			}
			if (!is_null($this->text))
			{
				$ret = $ret.'<div class="group_heading">'.$this->text.'</div>';
			}
			foreach($this->children as $child)
			{
				$ret = $ret.$child->renderInContext($context);
			}
			if (!is_null($this->innerMargin))
			{
				$ret = $ret.'</div>';
			}
			$ret = $ret.'</'.$this->type.'>';
			return $ret;
		}
		
		function getRenderId()
		{
			return $this->getFullId();
		}
		
		function add(&$com)
		{
			array_push($this->children,$com);
			return $com;
		}
		
		function dropChildren()
		{
			$this->children = array();
		}
		
		function replace(&$com)
		{
			if (is_array($com))
			{
				$this->children = $com;
			}
			else
			{
				$this->dropChildren();
				$this->add($com);
				return $com;
			}
		}
		
		function getChildren()
		{
			return $this->children;
		}
		
		function setText($t)
		{
			$this->text = $t;
			return $this;
		}
		
		function setInnerMargin($m)
		{
			
			if (is_numeric($m))
			{
				$m = $m.'px';
			}
			$this->innerMargin = $m;
			return $this;
		}
		
		function span()
		{
			$this->type = 'span';
			return $this;
		}
		
		function remove($id)
		{
			for ($i = 0; $i < sizeof($this->children);$i++)
			{
				if ($this->children[$i]->getId() == $id)
				{
					$this->children[$i]->a()->removeComponant($id);
					unset($this->children[$i]);
					$this->children = array_values($this->children);
					return;
				}
			}
		}
		/*
		function replaceComponent($com,$newCom)
		{
			$i = 0;
			foreach($this->children as $child)
			{
				if ($child == $com)
				{
					$this->children[$i] = $newCom;
					$this->refresh();
					return;
				}
				$i = $i + 1;
			}
		}*/
		function onClickE($actionE)
		{
			$this->clickable = true;
			$this->afterClickE = $actionE;
			return $this;
		}
		
		function click($id)
		{
			if ($this->clickable)
			{
				$this->setContextParam('row',$this->r()->getView()->getRowByLinkId($id));
				eval($this->afterClickE);
			}
		}
		
	}
	
	if (isset($_POST['clickGroup']))
	{
		require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
		$s = explode('x',$_POST['clickGroup'],4);
		Application::getApplication($s[1])->getComponent($s[2])->click($s[3]);
		Response::send();
	}

?>