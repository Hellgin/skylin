package com.distantmelody.framework.skylin.security;

public class LoginPassword extends LoginOption{
	String loginPassword;
	public LoginPassword(String loginPassword)
	{
		this.loginPassword = loginPassword;
	}
	public String getPassword() {
		return loginPassword;
	}
}
