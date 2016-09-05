package skylin;

import java.util.ArrayList;

import skylin.services.J;
import skylin.util.ServiceUtil;
import skylin.util.Util;

public class SecurityContext 
{
	private ArrayList<String> roles;
	private String username;
	
	String id;
	
	public SecurityContext()
	{
		id = Util.getSecureUnsignedNumberAsHexString(32);
	}
	
	public boolean login(String username, String password,String securityInstance)
	{
		this.username = null;
		roles = null;
		
        try
        {
            //J j = ServiceUtil.JService(Util.getFrameworkParam("SECURITY_API")+"/VerifyUser", J.M("username", username,"password",password));
        	J j = ServiceUtil.JService("http://localhost/rest.php?com.pepkor.shared.environment.security.services.VerifyUser", J.M("username", username,"password",password,"instance",securityInstance));
            if (!(boolean)j.get("valid").get(0))
            {
            	return false;
            }
        }
        catch (Exception e) 
        {
        	e.printStackTrace();
        	return false;
        }
        

        //J j = ServiceUtil.JService(Util.getFrameworkParam("SECURITY_API")+"/RolesForUser", J.M("username", username));
        J j = ServiceUtil.JService("http://localhost/rest.php?com.pepkor.shared.environment.security.services.RolesForUser", J.M("username", username,"instance",securityInstance));
        roles = new ArrayList<String>();
        if (j.get("roles") != null)
        {
	        for (Object o:j.get("roles").values())
	        {
	        	roles.add((String)o);
	        }
        }
        this.username = username.toUpperCase();
		return true;
	}
	
	public String getUsername()
	{
		return username;
	}
	
	public boolean userInRole(String role)
	{
		if (roles == null) return true;
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
	
}
