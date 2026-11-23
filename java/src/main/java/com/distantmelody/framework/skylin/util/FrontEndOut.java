package com.distantmelody.framework.skylin.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrontEndOut 
{
	private static final Logger LOG = LoggerFactory.getLogger(FrontEndOut.class);

	public void println(Object o)
	{
		LOG.info(String.valueOf(o));
	}
}
