package com.distantmelody.framework.skylin.security;

public class LoginDomain extends LoginOption{
	String domain;
	public LoginDomain(String domain)
	{
		this.domain = domain;
	}
	public String getDomain() {
		return domain;
	}
}
