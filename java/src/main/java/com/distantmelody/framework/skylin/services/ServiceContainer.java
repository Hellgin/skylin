package com.distantmelody.framework.skylin.services;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServlet;

public class ServiceContainer 
{
	static ConcurrentHashMap<String, ServiceContainer> currentContainer = new ConcurrentHashMap<String, ServiceContainer>();
	
	ServiceClassLoader classLoader;
	ArrayList<ServiceClass> classes = new ArrayList<ServiceClass>();
	
	/**
	 * The name comes from the request, so a class that is not an {@link HttpServlet} is refused (ClassCastException)
	 * before anything creates it: its static initializer and constructor never run.
	 */
	public HttpServlet load(String name) throws ReflectiveOperationException
	{
		ServiceContainer current = currentContainer.get(name);
		if (current != null && current.isClassesUpToDate())
		{
			for (int i = 0; i < current.classes.size();i++)
			{
				if (current.classes.get(i).klas.getName().equals(name))
				{
					return current.classes.get(i).klas.asSubclass(HttpServlet.class).getDeclaredConstructor().newInstance();
				}
			}
		}
		classLoader = new ServiceClassLoader(this);
		Class<? extends HttpServlet> service = classLoader.loadClass(name).asSubclass(HttpServlet.class);
		HttpServlet servlet = service.getDeclaredConstructor().newInstance();
		currentContainer.put(name, this);
		return servlet;
	}

	private boolean isClassesUpToDate() 
	{
		for (int i = 0; i < classes.size();i++)
		{
			ServiceClass c = classes.get(i);
			if (new File(c.path).lastModified() > c.lastModifiedDate)
			{
				return false;
			}
		}
		return true;
	}
}
