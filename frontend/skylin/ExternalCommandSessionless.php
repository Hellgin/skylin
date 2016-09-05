<?php
	$query = $_SERVER['QUERY_STRING'];
	$ret = apc_fetch($query);
	apc_delete($query);
	echo $ret;
?>