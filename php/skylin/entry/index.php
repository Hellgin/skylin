<?php
    $allow_get = true;
    require "skylin/InitRequest.php";
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
	$rootApplication = '';

	if (strlen($qs) > 0)
	{
		$rootApplication = $qs;
	    $only = strtoupper($_SESSION['UTIL']->getFrameworkParam('ALLOW_NON_DEFAULT_APPLICATION_HOST'));
		$exceptions = $_SESSION['UTIL']->getFrameworkParam('NON_DEFAULT_EXCEPTIONS');
		//$exceptions = 'pep/plm/sizespec/portalquote/main,pep/plm/sizespec/portalsample/main,pep/plm/sizespec/portalsample/temp_fix_for_seperation/quote_entry,pep/plm/sizespec/portalsample/temp_fix_for_seperation/purchase_order_entry';
		$exceptions = explode(',',$exceptions);
		
		$onlys = explode(',',$only);
		
		
		//if ($only != null && strtoupper($only) != strtoupper($_SERVER["HTTP_HOST"]) && ($exceptions == null || !in_array($qs,$exceptions)))
		if(($only != null && !in_array(strtoupper($_SERVER["HTTP_HOST"]),$onlys)) && ($exceptions == null || !in_array($qs,$exceptions)))
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
		$rootApplication = $defaultApp == null ? '' : $defaultApp;
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
	unset($_SESSION['STATIC_ICON']);
	unset($_SESSION['STATIC_CSS']);
	unset($_SESSION['STATIC_HEAD']);
	$content = $rootApp->render();
	$mainCss = '';
	if (isset($_SESSION['STATIC_CSS']) && $_SESSION['STATIC_CSS'] != '')
	{
		$mainCssPath = ltrim($_SESSION['STATIC_CSS'], '/');
		$mainCssFile = $_SERVER['DOCUMENT_ROOT'].'/'.$mainCssPath;
		$mainCssVersion = is_file($mainCssFile) ? filemtime($mainCssFile) : 1;
		$mainCss = '<link rel="stylesheet" type="text/css" href="'.htmlspecialchars($mainCssPath, ENT_QUOTES).'?v='.$mainCssVersion.'"/>';
	}
	$iconHref = 'skylin/images/skylin.png';
	if (isset($_SESSION['STATIC_ICON']) && $_SESSION['STATIC_ICON'] != '')
	{
		$iconHref = ltrim($_SESSION['STATIC_ICON'], '/');
	}
	$iconFile = $_SERVER['DOCUMENT_ROOT'].'/'.$iconHref;
	$iconHref = htmlspecialchars($iconHref, ENT_QUOTES).'?v='.(is_file($iconFile) ? filemtime($iconFile) : 1);
	//BridgeConnection.jvmCall('6');

	
?>
<html>
	<head>
		<meta charset="utf-8">
		<title><?php echo $_SESSION['STATIC_TITLE']; ?></title>
		<?php echo $_SESSION['STATIC_HEAD']; ?>
		<script type="text/javascript" src="skylin/jquery-3.2.1.min.js"></script>
		<?php echo $webSocketPortOveride; echo $topAppId?>
		<link rel="stylesheet" type="text/css" href="skylin/skylin.css?v=1"/>
		<?php echo $mainCss; ?>
		<link rel="shortcut icon" href="<?php echo $iconHref; ?>">
		<link rel="stylesheet" href="skylin/dependencies/pickaday/pikaday.css">
	    <script src="skylin/dependencies/pickaday/moment.min.js"></script>
        <script src="skylin/dependencies/pickaday/pikaday.js"></script>
        <script type="text/javascript" src="skylin/skylin-precontent.js?v=5" charset="UTF-8"></script>
        <?php echo $extraJS; ?>
	</head>
	<body style="overscroll-behavior: contain">
		<?php echo $content; ?>
		<div id = "hover_info" style="z-index: 1000;"></div>
		<iframe id="download_iframe" style="display:none;"></iframe>
		<script type="text/javascript" src="skylin/skylin-postcontent.js?v=1" charset="UTF-8"></script>
	</body>
</html>