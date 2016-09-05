<?php
require_once "Component.php";
class Splitter extends Component
{
	private $groupLeft;
	private $groupRight;
	private $closedE = 'return false;';
	private $reverse = false;
	
	public function __construct($name = null,$parentApp = null) 
	{
		parent::__construct();
		$this->groupLeft = new Group();
		$this->groupRight = new Group();
	}
	
	function setClosedE($c)
	{
		$this->closedE  = $c;
	}
	
	function setClosed($c)
	{
		$this->closedE = 'return '.$c.';';
	}
	
	function refreshState()
	{
		$this->refresh();//stub. Doing a full refresh of the splitter is complete overkill. Replace with proper class updates.
	}
	
	function toggle()
	{
		$this->setClosed(!$this->getClosed());
	}
	
	function getClosed()
	{
		return eval($this->closedE);
	}
	
	function render()
	{

		
		
		if (!$this->reverse)
		{
			/*
			$splitter = 'splitter';
			$bar = 'splitter_bar';
			$right = 'splitter_right';
			$button = 'splitter_button';
			if ($this->getClosed())
			{
				$splitter = $splitter.' splitter_closed';
				$bar = $bar.' splitter_bar_closed';
				$right = $right.' splitter_right_closed';
				$button = $button.' splitter_button_closed';
			}
			
			$id = $this->getFullId();
			return '<div class="'.$splitter.'" id="'.$id.'" '.$this->getStyleFull().'>
						<div class="splitter_left">'.$this->groupLeft->renderInContext($this->getContext()).'</div>
						<div class="'.$bar.'" id="'.$id.'_bar">
							<div class = "'.$button.'" id="'.$id.'_button" style="background-image: url(skylin/images/splitter_arrow.png)"></div>
						</div>
						<div class="'.$right.'" id="'.$id.'_right">'.$this->groupRight->renderInContext($this->getContext()).'</div>
					</div>';
			*/
			
			
			 $right = 'splitter_right';
			 $left = 'splitter_left';
			 $bar = 'splitter_bar';
			 $button = 'splitter_button';
			 if ($this->getClosed())
			 {
			 	 $right = $right.' splitter_right_closed';
			 	 $left = $left.' splitter_left_closed';
				 $bar = $bar.' splitter_bar_closed';
				 $button = $button.' splitter_button_closed';
			 }
			
			 $id = $this->getFullId();
			 
			return '<div class="new_splitter" id="'.$id.'">
										
    <div class="'.$left.'" id="'.$id.'_left">
	'.$this->groupLeft->renderInContext($this->getContext()).'								
    </div>
			
	<div class="'.$bar.'" id="'.$id.'_bar">
	<div class = "'.$button.'" id="'.$id.'_button" style="background-image: url(skylin/images/splitter_arrow.png)"></div>
	</div>		
			
    <div class="'.$right.'" id="'.$id.'_right">	
	'.$this->groupRight->renderInContext($this->getContext()).'				
    </div>
			
</div>
			
</div>';
		}
		else
		{
			
			$splitter = 'splitter_reverse';
			$bar = 'splitter_bar_reverse';
			$left = 'splitter_left_reverse';
			$button = 'splitter_button_reverse';
			$right = 'splitter_right_reverse';
			if ($this->getClosed())
			{
				$splitter = $splitter.' splitter_closed_reverse';
				$bar = $bar.' splitter_bar_closed_reverse';
				$left = $left.' splitter_left_closed_reverse';
				$button = $button.' splitter_button_closed_reverse';
				$right = $right.' splitter_right_closed_reverse';
			}
			
			$id = $this->getFullId();
			return '<div class="'.$splitter.'" id="'.$id.'" '.$this->getStyleFull().'>
						<div class="'.$left.'" id="'.$id.'_left">'.$this->groupLeft->renderInContext($this->getContext()).'</div>
						<div class="'.$bar.'" id="'.$id.'_bar">
							<div class = "'.$button.'" id="'.$id.'_button_reverse" style="background-image: url(skylin/images/splitter_arrow.png)"></div>
						</div>
						<div class="'.$right.'" id="'.$id.'_right">'.$this->groupRight->renderInContext($this->getContext()).'</div>
					</div>';		
			
			//return $this->groupRight->renderInContext($this->getContext());			
		}
	}
	
	function getLeft()
	{
		return $this->groupLeft;
	}
	
	function getRight()
	{
		return $this->groupRight;
	}
	
	function reverse()
	{
		$this->reverse = true;
	}
}

if (isset($_POST['toggle'])) 
{
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	$s = explode('x',$_POST['toggle'],5);
	//$s[3] not used yet.
	Application::getApplication($s[1])->getComponent($s[2])->toggle();
	//Response::info(Application::getApplication($s[1])->getComponent($s[2])->getFullId());
	Response::send();
}
?>