package skylin.server;

import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class SkylinServletConfig implements ServletConfig{

	ServletContext context;
	String servletName;
	
	public void setServletContext(ServletContext c)
	{
		context = c;
	}
	
	public void setServletName(String name)
	{
		servletName = name;
	}
	
	
	@Override
	public String getInitParameter(String arg0) {
		return context.getInitParameter(arg0);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return context.getInitParameterNames();
	}

	@Override
	public ServletContext getServletContext() 
	{
		return context;
	}

	@Override
	public String getServletName() 
	{
		return servletName;
	}

}
