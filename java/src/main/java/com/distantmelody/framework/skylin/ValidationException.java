package com.distantmelody.framework.skylin;

public class ValidationException extends Exception 
{
	String message;
	public ValidationException(String message)
	{
		this.message = message;
	}
}
