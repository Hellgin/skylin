<?php
class Template extends Component
{
	private $html;
	private $facets = array();
	private $names = array();
	private $types = array();
	//0 - component
	//1 - string
	//2 - string Expression
	
	public function __construct($htmlPath = null)
	{
		parent::__construct();
		if($htmlPath != null)
		{
			$this->html = file_get_contents($_SERVER["DOCUMENT_ROOT"].'/'.$htmlPath);
		}
	}

	function setCom($name,$f)
	{
		array_push($this->names,$name);
		array_push($this->facets,$f);
		array_push($this->types,0);
	}
	
	function setString($name,$f)
	{
		array_push($this->names,$name);
		array_push($this->facets,$f);
		array_push($this->types,1);
	}
	
	function setStringE($name,$f)
	{
		array_push($this->names,$name);
		array_push($this->facets,$f);
		array_push($this->types,2);
	}
	
	function render()
	{
		$ret = $this->html;
		
		for ($i = 0;$i < sizeof($this->facets);$i = $i + 1)
		{
			$t = $this->types[$i];
			$f = "";
			$obj = $this->facets[$i];
			if ($t == 0)
			{
				$f = $obj->renderInContext($this->getContext());
			}
			else if ($t == 1)
			{
				$f = $obj;
			}
			else if ($t == 2)
			{
				$f = eval($obj);
			}
			$ret = str_replace('<skylin_'.$this->names[$i].'>',$f,$ret);
		}
		return '<div id="'.$this->getFullId().'">
				'.$ret.'
				</div>';
	}
}
