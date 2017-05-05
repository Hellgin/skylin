<?php
	require_once "Group.php";
	class Application extends Group
	{
		private $childApplications;
		private $applicationModules;
		private $firstAm;
		private $name;
		private $instanceName;
		private $components = array();
		private $nextComponentId = 0;
		private $applicationId;
		private $dir;
		private $title;
		private $externalCommandHandler;
		private $above;
		private $below;
		private $functions;
		//private $destroyed;
	
		public function __construct($name = null,$parentApp = null,$instanceName = null) 
		{
			parent::__construct();
			$this->childApplications = array();
			$this->applicationModules = array();
			$this->applicationId = self::getNextApplicationId();
			$_SESSION['APPLICATIONS'][$this->applicationId] = $this;
			$this->name = $name;
			$this->instanceName = $instanceName;
			//Response::info($name.'  '.spl_object_hash($this));
			if (!is_null($parentApp))
			{
				$parentApp->setChildApplication($this);
			}	
			if (is_null($name))
			{
				$this->created = true;
			}
		}
	
		static function getRootApplication()
		{
			if (!isset($_SESSION['ROOT_APPLICATION']))
			{
				$root = realpath($_SERVER["DOCUMENT_ROOT"]);
				if (file_exists("$root/root_application_init.php"))
				{
					include 'root_application_init.php';//check that no application relies on this then get rid of it!!!
				}
				$_SESSION['UTIL'] = java('skylin.util.Util');
				$_SESSION['CONTEXT_PARAMS'] = java('skylin.ContextParams');
				//$_SESSION['CONTEXT_PARAMS']->load(java_context()->getServlet()->getServletContext());
				$_SESSION['CONTEXT_PARAMS']->load(java_servlet_context());
				$_SESSION['ROOT_APPLICATION'] = new Application();			
				$_SESSION['DEFAULT_ROW'] = new DefaultRow();
				$_SESSION['ROOT_APPLICATION']->setContextParam('application',$_SESSION['ROOT_APPLICATION']);
				$uri = $_SERVER['REQUEST_URI'];
				$loc = strpos($uri,'?');
				if ($loc == false)
				{
					$_SESSION['WEB_APP'] = $uri;
				}
				else 
				{
					$_SESSION['WEB_APP'] = substr($uri,0,strpos($uri,'?'));
				}
			}
			return $_SESSION['ROOT_APPLICATION'];
			

		}
		
		static function getNextApplicationId()
		{
			if (!isset($_SESSION['NEXT_APPLICATION_ID']))
			{
				$_SESSION['NEXT_APPLICATION_ID'] = 0;
			}
			$ret = $_SESSION['NEXT_APPLICATION_ID'];
			$_SESSION['NEXT_APPLICATION_ID'] = $_SESSION['NEXT_APPLICATION_ID'] + 1;
			return $ret;
		}
		
		function getTopApplication()
		{
			if ($this->a() == self::getRootApplication())
			{
				return $this;
			}
			return $this->a()->getTopApplication();
		}
		

		function getChildApplication($name,$instanceName = null)
		{
			if (is_null($instanceName))
			{
				$instanceName = $name;
			}
			if ($this->childApplications[$instanceName] == null)
			{
				return new Application($name,$this,$instanceName);
			}
			return $this->childApplications[$instanceName];
		}
		
		function newChildApplication($name,$instanceName = null)
		{
			if (is_null($instanceName))
			{
				$instanceName = $name;
			}
			$c = 1;
			$instanceName_o = $instanceName;
			while ($this->childApplications[$instanceName] != null)
			{
				$instanceName = $instanceName_o.'#'.$c;
				$c = $c + 1;
			}
			return new Application($name,$this,$instanceName);
		}
		
		function replaceChildApplication($name,$instanceName = null)
		{
			if (is_null($instanceName))
			{
				$instanceName = $name;
			}
			if ($this->childApplications[$instanceName] != null)
			{
				//$this->dropChildApplication($this->childApplications[$instanceName]);
				$this->childApplications[$instanceName]->destroy();
			}
			return new Application($name,$this,$instanceName);
		}
		
		

		function setChildApplication($app)
		{
			$this->childApplications[$app->getInstanceName()] = $app;
		}

		
		function addAM($name,$className,$dataSource = null)
		{	
			if ($this->applicationModules[$name] == null)
			{
				$am = (new java('skylin.AMContainer'))->load($className);
				$this->applicationModules[$name] = $am;
				$this->setProp('sec',$this->s());//hmmm. consider this plz. (i think it has been considered and its good?)
				$am->setSecurityContext($this->s());
				$am->setFrontEndApplicationId($this->getApplicationId());
				$am->setTopFrontEndApplicationId($this->getTopApplication()->getApplicationId());
				$am->setObjectId($am->_getJavaObjectId());
				$am->setBridgeSession(java_bridge_session());
				if (!is_null($dataSource))
				{
					$am->setDataSource($dataSource);
				}
				if (is_null($this->firstAm))
				{
					$this->firstAm = $am;
				}
			}
		}
		
		function getName()
		{
			return $this->name;
		}

		function getInstanceName()
		{
			return $this->instanceName;
		}
		
		function render()
		{
			if ($this->above != null)
			{
				$ret = '<div id='.$this->getFullId().'>';
				$ret = $ret.$this->above->renderInContext($this->c());
				$ret = $ret.'</div>';
				return $ret;
			}
			if (!$this->created)
			{
				$this->create();
			}

			$c = $this->getContext();
			$c['application'] = $this;

			if (!isset($_SESSION['STATIC_TITLE']))
			{
				$_SESSION['STATIC_TITLE'] = $this->title;
			}
			
			//return parent::render($c);
			return $this->renderInOtherContext($c);
		}
		
		
		function create()
		{
			if ($this->created)
			{
				$this->dropChildApplications();
				$this->dropChildren();
				$this->applicationModules = array();
				$this->firstAm = null;
				$this->components = array();
				$this->nextComponentId = 0;
				$this->dropAllProperties();
			}
			$root = realpath($_SERVER["DOCUMENT_ROOT"]);
			if (file_exists("$root/applications/".$this->name.".php"))
			{
				$t = 'applications/'.$this->name;
				$lastIndex = strlen($t) - 1 - strpos(strrev($t),'/');
				$this->dir = substr($t,0,$lastIndex);
				unset($t);
				unset($lastIndex);
				$this->created = true;
				include "$root/applications/".$this->name.".php";
			}
			else
			{
				ClassLoader::load('java');
				ClassLoader::load('TextField');
				$t = new TextField();
				$t->setStyle('margin: 5px');
				$t->setValue('Cannot find application '.$this->name);
				$this->replace($t);
				$this->created = true;
			}		
		}
		
		function getAM($name = null)
		{
			if ($name == null)
			{
				return $this->firstAm;
			}
			return $this->applicationModules[$name];
		}
		
		function getNextComponentId()
		{
			$next = $this->nextComponentId;
			$this->nextComponentId = $this->nextComponentId + 1;
			return $next;
		}
		
		function setComponent($id,$com)
		{	
			$this->components[$id] = $com;
		}
		
		function getApplicationId()
		{
			return $this->applicationId;
		}
		
		static function getApplication($id)
		{
			$app = $_SESSION['APPLICATIONS'][$id];
			if ($app == null)
			{
				Response::reload();
				Response::send(false);
				exit();
			}
			return $app;
		}
		
		function getComponent($id)
		{
			return $this->components[$id];
		}
		
		
		function com($id)
		{
			return $this->components[$id];
		}
		
		function getChildApplications()
		{
			return $this->childApplications;
		}	

		function destroy()
		{
			
			foreach ($this->childApplications as $a)
			{
				$a->destroy();
			}
			
			//$this->dropChildApplications();
			if ($this->a() != null)
			{
				$this->a()->dropChildApplication($this);
			}

			if ($this->above != null)
			{
				$this->above->destroy();
				$this->above = null;
			}
			

		}
		
		function dropChildApplication($a)
		{
			//Response::info('before: '.sizeof($_SESSION['APPLICATIONS']).' - '.sizeof($this->components).' - '.sizeof($this->childApplications));
			if(($key = array_search($a, $_SESSION['APPLICATIONS'])) !== false) {
				unset($_SESSION['APPLICATIONS'][$key]);
			}
			if(($key = array_search($a, $this->components)) !== false) {
				unset($this->components[$key]);
			}	
			
			if(($key = array_search($a, $this->childApplications)) !== false) {
				unset($this->childApplications[$key]);
			}
			//$a->markDestroyed();
			//Response::info('after:  '.sizeof($_SESSION['APPLICATIONS']).' - '.sizeof($this->components).' - '.sizeof($this->childApplications));
		}
		
		function dropChildApplications()
		{
			foreach ($this->childApplications as $a)
			{
				$a->dropChildApplications();
				if(($key = array_search($a, $_SESSION['APPLICATIONS'])) !== false) {
					unset($_SESSION['APPLICATIONS'][$key]);
				}
				if(($key = array_search($a, $this->components)) !== false) {
					unset($this->components[$key]);
				}
				//$a->markDestroyed();
			}
			$this->childApplications = array();
		}
		/*
		function markDestroyed()
		{
			$this->destroyed = true;
		}
		
		function isDestroyed()
		{
			return $this->destroyed;
		}
		*/
				
		function getApplicationModules()
		{
			return $this->applicationModules;
		}
		
		function getDir()
		{
			return $this->dir;
		}
		
		function setTitle($title)
		{
			$this->title = $title;
		}
		
		function overrideTitle($title)
		{
			$this->title = $title;
			Response::setTitle($title);
		}
		
		function refreshComs($comArray)
		{
			foreach ($comArray as $com)
			{
				$this->getComponent($com)->refresh();
			}
		}
		function refreshComsInRow($comArray,$row)
		{
			foreach ($comArray as $com)
			{
				$this->getComponent($com)->refreshInRow($row);
			}
		}
		
		function push($app)
		{
			//out::println("! ".$this->name);
			$app->setAbove($this);
			$this->below = $app;
			$app->refresh();
		}
		
		function pop()
		{
			//$this->below->setAbove(null);
			//$this->destroy();
			
			if ($this->a() != null)
			{
				if ($this->below != null)
				{
					$this->below->setAbove(null);
					$this->below->refresh();
					$this->destroy();
					return;
				}
				$this->a()->pop();
			}
		}
		
		function setAbove($a)
		{
			$this->above = $a;
		}
		
		function getAbove()
		{
			return $this->above;
		}
		
		function isOnStack()
		{
			//out::println("!! ".$this->name);
			if ($this->a() != null)
			{
				if ($this->below != null)
				{
					return true;
				}
				if ($this->a() != $this)
				{
					return $this->a()->isOnStack();
				}
			}
			return false;
		}
		
		function setExternalCommandHandler($handler)
		{
			$this->externalCommandHandler = $handler;
		}
		function handleExternalCommand($command)
		{

			if (isset($this->externalCommandHandler))
			{
				$this->setContextParam("externalCommand",$command);
				eval($this->externalCommandHandler);
			}
			else 
			{
				$this->passExternalCommand($command);
			}
		}
		function passExternalCommand($command)
		{
			foreach ($this->childApplications as $a)
			{
				$a->handleExternalCommand($command);
			}
		}
		function backendEval($code)
		{
			return eval($code);
		}
		
		function register($n,$f)
		{
			$this->functions[$n] = $f;
		}
		
		public function __call($name, $arg)
		{	
			return eval($this->functions[$name]);
		}
		
		function removeComponant($id)
		{
			unset($this->components[$id]);
		}
		
		//deprecated. use getView
		function getViewObject($o)
		{
			return $this->getAM()->getViewObject($o);
		}
		
		function getView($o)
		{
			return $this->getAM()->getView($o);
		}
	}
	
	class DefaultRow
	{
		private $view;
		
		function __construct()
		{
			$this->view = new DefaultView($this);
		}
		function getLinkId()
		{
			return 0;
		}
		
		function getView()
		{
			return $this->view;
		}
		
		function getLinkedView($id)
		{
			return $this->view;
		}
		
		function getHtmlInputType()
		{
			return '"type="text" class="TextField"';
		}
		
		function getValue()
		{
			return '';
		}
		
		function isUpdateable()
		{
			return false;
		}
		
		public function __call($name, $arguments)
		{	
		}
	}
	
	class DefaultView
	{
		private $row;
		function __construct($r)
		{
			$this->row = $r;
		}
		function getRowByLinkId($id)
		{
			return $this->row;
		}
		function getRows()
		{
			return array();
		}
		
		public function __call($name, $arguments)
		{
		}
	}

?>