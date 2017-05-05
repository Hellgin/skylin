package skylin;

import java.util.ArrayList;

public class ViewToJConfig 
{
	private ArrayList<String> globalExcludeList = new ArrayList<String>();
	
	public void globalExclude(String n)
	{
		globalExcludeList.add(n);
	}
	
	boolean included(String n)
	{
		return !globalExcludeList.contains(n);
	}
}
