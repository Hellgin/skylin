package skylin;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletContext;

public class ContextParams 
{
	static HashMap<String,String> map;
	public static void load(ServletContext sc)
	{
		if (map != null)
		{
			return;
		}
		List<String> names = Collections.list(sc.getInitParameterNames());
		map = new HashMap<String,String>();
		for (String name:names)
		{
			map.put(name, sc.getInitParameter(name));
		}
	}
	
	public static String get(String key)
	{
		return map.get(key);
	}
}
