package skylin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class AMContainer 
{
	static ConcurrentHashMap<String, AMContainer> currentContainer = new ConcurrentHashMap<String, AMContainer>();
	
	SkylinClassLoader classLoader;
	ArrayList<AMClass> classes = new ArrayList<AMClass>();
	
	public AM load(String name) throws InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		AMContainer current = currentContainer.get(name);
		if (current != null && current.isClassesUpToDate())
		{
			for (int i = 0; i < current.classes.size();i++)
			{
				if (current.classes.get(i).klas.getName().equals(name))
				{
					return (AM)current.classes.get(i).klas.newInstance();
				}
			}
		}
		classLoader = new SkylinClassLoader(this);
		AM am = (AM) classLoader.loadClass(name).newInstance();
		currentContainer.put(name, this);
		return am;
	}

	private boolean isClassesUpToDate() 
	{
		for (int i = 0; i < classes.size();i++)
		{
			AMClass c = classes.get(i);
			if (new File(c.path).lastModified() > c.lastModifiedDate)
			{
				return false;
			}
		}
		return true;
	}
}
