package com.distantmelody.framework.skylin.security;

public class LoginRequiredPrivilege extends LoginOption{
	String loginRequiredPrivilege;
	boolean isReasonForFailure;
	public LoginRequiredPrivilege(String loginRequiredPrivilege)
	{
		this.loginRequiredPrivilege = loginRequiredPrivilege;
	}
	
	public String getRequiredPrivilege()
	{
		return loginRequiredPrivilege;
	}

	public void setIsReasonForFailure(boolean b) {
		isReasonForFailure = true;
	}
	
	public boolean isReasonForFailure()
	{
		return isReasonForFailure;
	}
}
