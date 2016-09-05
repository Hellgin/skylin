<?php

	$qs = '';
	if (isset($_SERVER['QUERY_STRING']))
	{
		$qs = $_SERVER['QUERY_STRING'];
	}
	if (substr($qs,-1) == '!')
	{
		$clear = true;
		$qs = substr($qs,0,-1);
	}
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	$rootApp = Application::getRootApplication();

	if (strlen($qs) > 0)
	{
		if ($clear)
		{
			$rootApp->replace($rootApp->replaceChildApplication($qs));
			
		}
		else 
		{
			$rootApp->replace($rootApp->getChildApplication($qs));
		}
	}
	else
	{	
		$defaultApp = $_SESSION['UTIL']->getFrameworkParam('DEFAULT_APPLICATION');
		if ($defaultApp != null)
		{
			if ($clear)
			{
				$rootApp->replace($rootApp->replaceChildApplication($defaultApp));
			}
			else 
			{
				$rootApp->replace($rootApp->getChildApplication($defaultApp));
			}		
		}
		else 
		{
			$rootApp->replace(no_compromise());
		}
	}
	
	unset($_SESSION['STATIC_TITLE']);
	$content = $rootApp->render();
	BridgeConnection.jvmCall('6');
?>
<html>
	<head>
		<title><?php echo $_SESSION['STATIC_TITLE']; ?></title>
		<script type="text/javascript" src="skylin/jquery-2.2.4.min.js"></script>
		<script type="text/javascript" src="skylin/skylin.js"></script>
		<link rel="stylesheet" type="text/css" href="skylin/skylin.css"/>
		<link rel="shortcut icon" href="skylin/images/dna.png">
		
		<link rel="stylesheet" href="skylin/dependencies/pickaday/pikaday.css">
	    <script src="skylin/dependencies/pickaday/moment.min.js"></script>
        <script src="skylin/dependencies/pickaday/pikaday.js"></script>
	</head>
	<body>
		<?php echo $content; ?>
		<div id = "hover_info" style="z-index: 1000;"></div>
		<iframe id="download_iframe" style="display:none;"></iframe>
	</body>
</html>