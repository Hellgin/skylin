package com.distantmelody.framework.skylin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JToViewConfig 
{

	
	HashMap<String,Class> viewClasses = new HashMap<String,Class>();
	ArrayList<StringDateToMilisConverter> stringDateToMilisConverters = new ArrayList<StringDateToMilisConverter>();
	
	public HashMap<String,Class> getViewClasses()
	{
		return viewClasses;
	}
	
	public void putViewClass(String name, Class c)
	{
		viewClasses.put(name, c);
	}

	public List<StringDateToMilisConverter> getStringDateToMilisConverters() {
		return stringDateToMilisConverters;
	}
	
	public void addStringDateToMilisConverter(StringDateToMilisConverter converter)
	{
		stringDateToMilisConverters.add(converter);
	}

}
