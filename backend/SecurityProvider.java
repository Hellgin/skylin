package skylin;

import java.util.List;

public interface SecurityProvider 
{
	public boolean authenticate(String username, String password,String domain);
	public List<String> authorize(String username,String domain);
	public String getTrueUsername(String username,String domain);
}
