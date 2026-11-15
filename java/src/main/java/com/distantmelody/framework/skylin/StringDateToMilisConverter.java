package com.distantmelody.framework.skylin;

import java.text.ParseException;

public interface StringDateToMilisConverter 
{
	public Long convert(String date) throws ParseException;
}
