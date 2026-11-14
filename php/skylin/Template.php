<?php
require_once "Component.php";
class Template extends Component
{
	private $html;
	private $facets = array();
	private $names = array();
	private $types = array();
	//0 - component
	//1 - string
	//2 - string Expression

	private $eventNames = array();
	private $events = array();
	
	public function __construct($htmlPath = null)
	{
		parent::__construct();
		if($htmlPath != null)
		{
			$this->html = file_get_contents($_SERVER["DOCUMENT_ROOT"].$_SESSION["UTIL"]->getFrameworkParam('SKYLIN_ROOT').'/'.$htmlPath);
		}
		$this->setStringE("amId",'return $this->getAM()->_getJavaObjectId();');
		$this->setStringE("comId",'return $this->getFullId();');
	}

	function setCom($name,$f)
	{
		array_push($this->names,$name);
		array_push($this->facets,$f);
		array_push($this->types,0);
		return $f;
	}
	
	function setString($name,$f)
	{
		array_push($this->names,$name);
		array_push($this->facets,$f);
		array_push($this->types,1);
		return $this;
	}
	
	function setStringE($name,$f)
	{
		array_push($this->names,$name);
		array_push($this->facets,Expression::toExpression($f));
		array_push($this->types,2);
		return $this;
	}
	
	function setEventE($name,$e)
	{
	    array_push($this->eventNames,$name);
	    array_push($this->events,Expression::toExpression($e));
	    return $this;
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
		/*
		return '<div id="'.$this->getFullId().'">
				'.$ret.'
				</div>';
		*/
		return $ret;
	}
    function event($id,$name,$data)
    {   
        $i  = array_search($name,$this->eventNames);
        if ($i !== false)
        {
            $this->setContextParam('row',$this->r()->getView()->getRowByLinkId($id));
            $this->setContextParam('eventData',$data);
            eval($this->events[$i]);
        }
    }
}

if (isset($_POST['templateEvent']))
{
    require "InitRequest.php";
    $s = explode('x',$_POST['templateEvent'],4);
    Application::getApplication($s[1])->getComponent($s[2])->event($s[3],$_POST['eventName'],$_POST['eventData']);
    Response::send();
}
