<?php
require_once "Component.php";
class AnchorPanel extends Component
{
	private $root;
	private $rendering = false;
	public function __construct()
	{
		parent::__construct();
		$this->root = new Group();
	}
	
	function render()
	{
		if (!$this->rendering)
		{
			return '<div id="'.$this->getFullId().'"></div>';
		}
		$this->rendering = false;
		//$close = '<button class="anchor_panel_close"><img src="'.$_SESSION['WEB_APP'].'/skylin/images/tab_close.png"></button>';
		$close = '<button class="anchor_panel_close"><img src="skylin/images/tab_close.png"></button>';
		return '<div id="'.$this->getFullId().'" class="AnchorPanel" '.$this->getStyleFull().'>'.$this->root->renderInContext($this->getContext()).$close.'</div>';
	}
	
	function refreshInRow($row)
	{
		$this->rendering = true;
		return parent::refreshInRow($row);
	}
	
	function refresh()
	{
		$this->rendering = true;
		return parent::refresh();
	}
	
	function closeInRow($row)
	{
		return parent::refreshInRow($row);
	}
	
	function close()
	{
		return parent::refresh();
	}
	
	function setInnerMargin($m)
	{
		$this->root->setInnerMargin($m);
	}
	
	function add($m)
	{
		return $this->root->add($m);
	}
}
?>