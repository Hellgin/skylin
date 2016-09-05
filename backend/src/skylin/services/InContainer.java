package skylin.services;

public class InContainer 
{
	public String api_action;
	public transient Class outClass;
	
	public InContainer()
	{
		api_action = getClass().getSimpleName().substring(getClass().getSimpleName().indexOf("_")+1);
		try 
		{
			outClass = getClass().forName(getClass().getPackage().getName()+".Out_"+api_action);
		} 
		catch (ClassNotFoundException e) 
		{
			e.printStackTrace();
		}
	}
}
