package skylin.services;

public class ServiceClass
{
	public ServiceClass(Class c, long date,String p)
	{
		klas = c;
		lastModifiedDate = date;
		path = p;
	}
	
	Class klas;
	long lastModifiedDate;
	String path;
}
