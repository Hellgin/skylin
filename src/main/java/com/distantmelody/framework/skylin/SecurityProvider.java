package com.distantmelody.framework.skylin;

import java.util.List;

import com.distantmelody.framework.skylin.security.LoginOption;

public interface SecurityProvider 
{
	public boolean authenticate(String username, LoginOption... options);
	public List<String> authorize(String username,LoginOption... options);
	public String getTrueUsername(String username,LoginOption... options);
}
