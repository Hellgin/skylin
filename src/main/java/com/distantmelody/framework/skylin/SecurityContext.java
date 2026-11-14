package com.distantmelody.framework.skylin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.distantmelody.framework.skylin.security.LoginDomain;
import com.distantmelody.framework.skylin.security.LoginOption;
import com.distantmelody.framework.skylin.security.LoginPassword;
import com.distantmelody.framework.skylin.security.LoginRequiredPrivilege;
import com.distantmelody.framework.skylin.security.QuickRoleLookup;
import com.distantmelody.framework.skylin.util.Util;

public class SecurityContext 
{
	private ArrayList<String> roles = new ArrayList<String>();
	private String username;
	private String systemSeperator = "_";
	private String domain;
	
	public static SecurityProvider[] providers;
	
	private HashMap<String,String> systemSubstitutes = new HashMap<String,String>();
	
	String id;
	
	QuickRoleLookup quickRoleLookup = new QuickRoleLookup(this);
	
	public SecurityContext()
	{
		id = Util.getSecureUnsignedNumberAsHexString(32);
	}
	
	public boolean login(String username,LoginOption... options)
	{	
		LoginPassword loginPassword = null;
		String loginDomain = null; 
		LoginRequiredPrivilege requiredPrivilege = null;
		for (LoginOption o:options)
		{
			if (o instanceof LoginPassword)
			{
				loginPassword = (LoginPassword) o;
			}
			if (o instanceof LoginDomain)
			{
				loginDomain = ((LoginDomain)o).getDomain();
			}
			if (o instanceof LoginRequiredPrivilege)
			{
				requiredPrivilege = (LoginRequiredPrivilege)o;
			}
		}
		
		if (isLoggedIn())
		{
			logout();
		}
		boolean success = false;
		for (SecurityProvider sp:providers)
		{
			boolean hasAuthenticated;
			if (loginPassword != null)
			{
				hasAuthenticated = sp.authenticate(username, options);
			}
			else
			{
				hasAuthenticated = true;
			}
			if(hasAuthenticated)
			{
				List<String> rolesToAdd = sp.authorize(username, options);
				if (requiredPrivilege != null && (rolesToAdd == null || !userInRole(requiredPrivilege.getRequiredPrivilege(),rolesToAdd)))
				{
					requiredPrivilege.setIsReasonForFailure(true);
					return false;
				}
				if (this.username == null)
				{
					this.username = sp.getTrueUsername(username,options);
				}
				domain = loginDomain;

				if (rolesToAdd != null)
				{
					roles.addAll(rolesToAdd);
				}
				success = true;
			}
		}
		return success;
	}	
	
	
	public boolean login(String username, String password,String securityInstance)
	{	
		if (isLoggedIn())
		{
			logout();
		}
		boolean success = false;
		LoginOption[] options = new LoginOption[] {new LoginPassword(password),new LoginDomain(securityInstance)};
		for (SecurityProvider sp:providers)
		{
			if(sp.authenticate(username, options))
			{
				if (this.username == null)
				{
					this.username = sp.getTrueUsername(username,options);
				}
				domain = securityInstance;
				List<String> rolesToAdd = sp.authorize(username, options);
				if (rolesToAdd != null)
				{
					roles.addAll(rolesToAdd);
				}
				success = true;
			}
		}
		return success;
	}
	

	public boolean login(String username, String securityInstance)
	{
		if (isLoggedIn())
		{
			logout();
		}
		LoginOption[] options = new LoginOption[] {new LoginDomain(securityInstance)};
		for (SecurityProvider sp:providers)
		{
			if (this.username == null)
			{
				this.username = sp.getTrueUsername(username,options);
			}
			domain = securityInstance;
			List<String> rolesToAdd = sp.authorize(username, options);
			if (rolesToAdd != null)
			{
				roles.addAll(rolesToAdd);
			}
		}
		return true;
	}
	
	public void logout()
	{
		username = null;
		roles = new ArrayList<String>();
	}
	
	public String getUsername()
	{
		if (!isLoggedIn())
		{
			return "TEST";
		}
		return username;
	}
	
	
	public boolean userInRole(String role)
	{
		return userInRole(role,roles);
	}
	
	private boolean userInRole(String role,List<String> r)
	{
		role = getSubstitutedRole(role);
		return r.contains(role);
	}
	
	public String getSubstitutedRole(String role)
	{
		if (role != null && role.contains(systemSeperator))
		{
			String system = role.substring(0, role.indexOf(systemSeperator));
			String replace = systemSubstitutes.get(system);
			if (replace != null)
			{
				role = role.replaceFirst(system, replace);
			}
		}
		return role;
	}
	
	public boolean isLoggedIn()
	{
		return username != null;
	}
	
	
	public ArrayList<String> getRoles(){
		return (ArrayList<String>) roles.clone();
	}

	
	public String getId()
	{
		return id;
	}

	public static void createProviders() {
		String prov = Util.getFrameworkParam("SECURITY_PROVIDERS");
		if (prov == null)
		{
			/*
			//--------------------------------------------- temp code until SECURITY_PROVIDERS parameter is added to all environments
			try
			{
				providers = new SecurityProvider[]{(SecurityProvider) Class.forName("com.pepkor.shared.environment.security.providers.PepkorLdap").newInstance()};
			}
			catch (Exception e){e.printStackTrace();};
			//----------------------------------------------
			*/
			return;
		}
		
		String[] provs = prov.split(",");
		providers = new SecurityProvider[provs.length];
		
		for (int i = 0; i < provs.length;i++)
		{
			try
			{
				providers[i] = (SecurityProvider) Class.forName(provs[i]).newInstance();
			}
			catch (Exception e){e.printStackTrace();};
		}
	}
	
	public void substituteSystem(String replace, String with)
	{
		systemSubstitutes.put(replace, with);
	}
	
	public void setSystemSeperator(String seperator)
	{
		systemSeperator = "_";
	}
	
	public String getDomain()
	{
		return domain;
	}
	
	public QuickRoleLookup role()
	{
		return quickRoleLookup;
	}
	
}
