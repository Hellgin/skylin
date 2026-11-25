package com.distantmelody.framework.skylin.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServlet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.distantmelody.framework.skylin.SkylinClassLoader;

/**
 * rest.php and restless.php pass the class name from the query string to {@link ServiceContainer#load}.
 */
class ServiceContainerTest
{
	static final AtomicInteger notAServiceInitialized = new AtomicInteger();
	static final AtomicInteger notAServiceCreated = new AtomicInteger();

	public static class NotAService
	{
		static
		{
			notAServiceInitialized.incrementAndGet();
		}

		public NotAService()
		{
			notAServiceCreated.incrementAndGet();
		}
	}

	public static class Service extends HttpServlet
	{
		private static final long serialVersionUID = 1L;
	}

	private String[] applicationPackages;

	@BeforeEach
	void classpathOnly()
	{
		applicationPackages = SkylinClassLoader.applicationPackages;
		SkylinClassLoader.applicationPackages = new String[] {"no.application.package."};
	}

	@AfterEach
	void restore()
	{
		SkylinClassLoader.applicationPackages = applicationPackages;
	}

	@Test
	void aServletIsCreated() throws Exception
	{
		assertInstanceOf(Service.class, new ServiceContainer().load(Service.class.getName()));
	}

	@Test
	void anyOtherClassIsRefusedBeforeItsCodeRuns()
	{
		assertThrows(ClassCastException.class, () -> new ServiceContainer().load(NotAService.class.getName()));
		assertEquals(0, notAServiceInitialized.get(), "static initializer");
		assertEquals(0, notAServiceCreated.get(), "constructor");
	}

	@Test
	void aClassThatStartsAThreadIsRefused()
	{
		assertThrows(ClassCastException.class, () -> new ServiceContainer().load("com.distantmelody.framework.skylin.bridge.ContinuousLogic"));
		assertThrows(ClassCastException.class, () -> new ServiceContainer().load("java.lang.Thread"));
		assertFalse(ServiceContainer.currentContainer.containsKey("java.lang.Thread"), "a refused class is not cached");
	}

	@Test
	void anUnknownClassIsNotFound()
	{
		assertThrows(ClassNotFoundException.class, () -> new ServiceContainer().load("no.such.Service"));
	}
}
