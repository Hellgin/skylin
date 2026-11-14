package com.distantmelody.framework.skylin;

import java.util.ArrayList;

public class ViewToJConfig 
{
	private ArrayList<String> globalExcludeList = new ArrayList<String>();
	private boolean followLinks = true;
	
	public void globalExclude(String n)
	{
		globalExcludeList.add(n);
	}
	
	boolean included(String n)
	{
		return !globalExcludeList.contains(n);
	}
	
	boolean isFollowLinks()
	{
		return followLinks;
	}
	
	public void dontFollowLinks()
	{
		followLinks = false;
	}
}
