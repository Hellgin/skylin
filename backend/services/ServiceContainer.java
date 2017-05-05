package skylin.services;

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
	
	public HttpServlet load(String name) throws InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		ServiceContainer current = currentContainer.get(name);
		if (current != null && current.isClassesUpToDate())
		{
			for (int i = 0; i < current.classes.size();i++)
			{
				if (current.classes.get(i).klas.getName().equals(name))
				{
					return (HttpServlet)current.classes.get(i).klas.newInstance();
				}
			}
		}
		classLoader = new ServiceClassLoader(this);
		HttpServlet am = (HttpServlet) classLoader.loadClass(name).newInstance();
		currentContainer.put(name, this);
		return am;
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
