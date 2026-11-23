package com.distantmelody.framework.skylin.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static logger for callers that do not keep their own, including PHP
 * ({@code java('com.distantmelody.framework.skylin.util.Log')->error($message)}).
 */
public final class Log
{
	private static final Logger LOG = LoggerFactory.getLogger(Log.class);

	private Log()
	{
	}

	public static void debug(String message)
	{
		LOG.debug(message);
	}

	public static void info(String message)
	{
		LOG.info(message);
	}

	public static void warn(String message)
	{
		LOG.warn(message);
	}

	public static void error(String message)
	{
		LOG.error(message);
	}
}
