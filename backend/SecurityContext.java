package skylin;

import java.util.ArrayList;
import java.util.List;

import skylin.util.Util;

public class SecurityContext 
{
	private ArrayList<String> roles = new ArrayList<String>();
	private String username;
	
	public static SecurityProvider[] providers;
	
	String id;
	
	public SecurityContext()
	{
		id = Util.getSecureUnsignedNumberAsHexString(32);
	}
	
	public boolean login(String username, String password,String securityInstance)
	{	
		if (isLoggedIn())
		{
			logout();
		}
		boolean success = false;
		for (SecurityProvider sp:providers)
		{
			if(sp.authenticate(username, password, securityInstance))
			{
				if (this.username == null)
				{
					this.username = sp.getTrueUsername(username,securityInstance);
				}
				List<String> rolesToAdd = sp.authorize(username, securityInstance);
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
		for (SecurityProvider sp:providers)
		{
			if (this.username == null)
			{
				this.username = sp.getTrueUsername(username,securityInstance);
			}
			List<String> rolesToAdd = sp.authorize(username, securityInstance);
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
		return roles.contains(role);
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
			//--------------------------------------------- temp code until SECURITY_PROVIDERS parameter is added to all environments
			try
			{
				providers = new SecurityProvider[]{(SecurityProvider) Class.forName("com.pepkor.shared.environment.security.providers.PepkorLdap").newInstance()};
			}
			catch (Exception e){e.printStackTrace();};
			//----------------------------------------------
			
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
	
}
