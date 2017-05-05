<?php
	require_once "Component.php";	
	class Image extends Component
	{
		private $sourceE;
		private $noLoad;
		private $onClick;

		public function __construct($sourceE = null) 
		{
			parent::__construct();
			if (strpos($sourceE, ' ') !== false) {
				$this->sourceE = $sourceE;
				$this->setProp('col',null);
			}
			else 
			{
				$this->setProp('col',$sourceE);
			}
		}
	
		function setSourceE($s)
		{
			$this->sourceE = $s;
			return $this;
		}
		
		function setSourceRel($image)
		{
			$this->sourceE = 'return $this->a()->getDir()."/'.$image.'";';
			return $this;
		}
		
		function getSource()
		{
			if ($this->c('col') != null)
			{
				return eval('return $this->r()->getValue($this->c("col"))->getUrl();');
			}
			return eval($this->sourceE);
		}
		
		function getRendered()
		{
			if ($this->c('col') != null && $this->r() == $_SESSION['DEFAULT_ROW'])
			{
				return false;
			}
			if ($this->c('col') != null && eval('return $this->r()->getValue($this->c("col"))->size() == 0;'))
			{
				return false;
			}
			return parent::getRendered();
		}
		
		function noLoad()
		{
			$this->noLoad = true;
			return $this;
		}
		
		
		function render()
		{
			/*
			if (java_is_null($this->c('row')))
			{
				$this->setContextParam('row',$this->c('defaultRow'));
			}*/
			$source = $this->getSource();
			if (strlen($source) == 0)
			{
				return '';
			}
			if ($this->noLoad)
			{
				$id = $this->getFullId();
				$style = $this->getStyle();
				return '<img id="'.$id.'" src="'.$source.'" style="'.$style.'"class="image" '.$this->getTitleFull().'>';	
			}
			else
			{
				$id = $this->getFullId();
				$style = $this->getStyle();
				$styleHidden = $this->getStyle().';display: none;';
				return '<img id="'.$id.'_l" src="skylin/images/loading.gif" style="'.$style.'" '.$this->getTitleFull().'>
						<img id="'.$id.'" src="'.$source.'" style="'.$styleHidden.'"class="image" '.$this->getTitleFull().'>';
			}
		}
		
		function onClick($code)//deprecated
		{
			$this->onClick = $code;
			return $this;
		}
		
		function onClickE($code)
		{
			$this->onClick = $code;
			return $this;
		}
		
		function click($id)
		{
			if ($this->onClick == null)
			{
				return;
			}
			$this->setContextParam('row',$this->r()->getView()->getRowByLinkId($id));
			eval($this->onClick);
		}
	}
	
if (isset($_POST['imageClick']))
{
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	$s = explode('x',$_POST['imageClick'],4);
	Application::getApplication($s[1])->getComponent($s[2])->click($s[3]);
	Response::send();
}

?>