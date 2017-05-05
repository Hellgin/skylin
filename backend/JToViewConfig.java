package skylin;

import java.util.ArrayList;
import java.util.HashMap;

public class JToViewConfig 
{

	
	HashMap<String,Class> viewClasses = new HashMap<String,Class>();
	
	public HashMap<String,Class> getViewClasses()
	{
		return viewClasses;
	}
	
	public void putViewClass(String name, Class c)
	{
		viewClasses.put(name, c);
	}

}
