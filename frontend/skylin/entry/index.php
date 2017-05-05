<?php
    require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	$qs = '';
	if (isset($_SERVER['QUERY_STRING']))
	{
		$qs = $_SERVER['QUERY_STRING'];
	}
	if (strpos($qs, '=') !== false) 
	{
		$qs = $_GET['application'];
		$token = $_GET['token'];
		if ($token != null)
		{
			$oldToken = Application::getRootApplication()->getChildApplication($qs)->c('skylin_token');
			
			if ($oldToken != $token)
			{
				$clear = true;
			}
		}
	}
	else if (substr($qs,-1) == '!')
	{
		$clear = true;
		$qs = substr($qs,0,-1);
	}

	$rootApp = Application::getRootApplication();
	$app = null;

	if (strlen($qs) > 0)
	{
		$only = $_SESSION['UTIL']->getFrameworkParam('ALLOW_NON_DEFAULT_APPLICATION_HOST');
		if ($only != null && strtoupper($only) != strtoupper($_SERVER["HTTP_HOST"]))
		{
			$rootApp->replace(new Label('Not allowed on this host'));
		}
		else if ($clear)
		{
			$app = $rootApp->replace($rootApp->replaceChildApplication($qs));
		}
		else 
		{
			$app = $rootApp->replace($rootApp->getChildApplication($qs));
		}
		if ($token != null)
		{
			$rootApp->getChildApplication($qs)->setProp('skylin_token',$token);
		}
	}
	else
	{	
		$defaultApp = $_SESSION['UTIL']->getFrameworkParam('DEFAULT_APPLICATION');
		$a = explode(",",$defaultApp);
		$size = sizeof($a);
		$host = $_SERVER["HTTP_HOST"];
		if (substr($host,0,4) == 'www.')
		{
			$host = substr($host,4);
		}
		if ($size > 1)
		{
			for ($i = 0; $i < $size;$i = $i + 1)
			{
				if ($host == $a[$i])
				{
					$defaultApp = $a[$i+1];
				}
			}
		}
		if ($defaultApp != null)
		{
			if ($clear)
			{
				$app = $rootApp->replace($rootApp->replaceChildApplication($defaultApp));
			}
			else 
			{
				$app = $rootApp->replace($rootApp->getChildApplication($defaultApp));
			}		
		}
		else 
		{
			$rootApp->replace(no_compromise());
		}
	}
	
	if ($_SESSION['UTIL']->getFrameworkParam('WEB_SOCKET_ALT_PORT') != null && $_SESSION['UTIL']->getFrameworkParam('WEB_SOCKET_ALT_PORT') == "true")
	{
		$webSocketPortOveride = '<script type="text/javascript">webSocketPortOverride = 1001;</script>';
	}
	
	if ($_SESSION['UTIL']->getFrameworkParam('EXTRA_JS') != null)
	{
		$ar = explode(",",$_SESSION['UTIL']->getFrameworkParam('EXTRA_JS'));
		foreach($ar as $a)
		{
			$extraJS = $extraJS.'<script type="text/javascript" src="'.$a.'"></script>';
		}
	}
	
	if ($app != null)
	{
		$topAppId = '<script type="text/javascript">topAppId = '.$app->getApplicationId().';</script>';
	}
	
	unset($_SESSION['STATIC_TITLE']);
	$content = $rootApp->render();
	BridgeConnection.jvmCall('6');
?>
<html>
	<head>
		<title><?php echo $_SESSION['STATIC_TITLE']; ?></title>
		<script type="text/javascript" src="skylin/jquery-2.2.4.min.js"></script>
		<?php echo $webSocketPortOveride; echo $topAppId?>
		<script type="text/javascript" src="skylin/skylin.js"></script>
		<link rel="stylesheet" type="text/css" href="skylin/skylin.css"/>
		<link rel="shortcut icon" href="skylin/images/dna.png">
		<link rel="stylesheet" href="skylin/dependencies/pickaday/pikaday.css">
	    <script src="skylin/dependencies/pickaday/moment.min.js"></script>
        <script src="skylin/dependencies/pickaday/pikaday.js"></script>
        <?php echo $extraJS; ?>
	</head>
	<body>
		<?php echo $content; ?>
		<div id = "hover_info" style="z-index: 1000;"></div>
		<iframe id="download_iframe" style="display:none;"></iframe>
	</body>
</html>