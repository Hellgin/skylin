package skylin.services;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;




import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SimpleServiceContainer extends HttpServlet{
    public HashMap<String,Method> services = new HashMap<String,Method>();
    
    public void service(String name)
    {
    	service(name,name);
    }
    public void service(String serviceName, String methodName)
    {
    	try
    	{
    		services.put(serviceName,getClass().getMethod(methodName, HttpServletRequest.class,HttpServletResponse.class));
    	}
    	catch (Exception e)
    	{
    		throw new RuntimeException(e.getMessage());
    	}
    }
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		try
		{

			MultiReadHttpServletRequest multiReadRequest = new MultiReadHttpServletRequest(request);
			byte[] data = new byte[100];
			multiReadRequest.getInputStream().read(data);
			//make shift... parse the json properly to find api_action at top level. This will break...
			String s = new String(data);
			String key = "\"api_action\":\"";
			int start = s.indexOf(key)+key.length();
			s = s.substring(start);
			s = s.substring(0,s.indexOf("\""));
			if (!services.containsKey(s))
			{
				OutContainer out = new OutContainer();
			    out.status = false;
			    out.error_description = "Service " + s +" does not exist in API ";
			    response.setContentType("text/plain");
			    Gson gson = new GsonBuilder().create();
			    //response.setContentType("application/json");
				response.setContentType("text/plain");
			    response.getOutputStream().write(gson.toJson(out).getBytes());
			    return;
			}
			services.get(s).invoke(this, multiReadRequest,response);
		    //response.setContentType("application/json");
			response.setContentType("text/plain");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			OutContainer out = new OutContainer();
		    out.status = false;
		    StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
		    out.error_description = "error in service: "+ sw;
		    response.setContentType("text/plain");
		    Gson gson = new GsonBuilder().create();
		    //response.setContentType("application/json");
			response.setContentType("text/plain");
		    response.getOutputStream().write(gson.toJson(out).getBytes());
		}
	}
    public class MultiReadHttpServletRequest extends HttpServletRequestWrapper {
  	  private ByteArrayOutputStream cachedBytes;

  	  public MultiReadHttpServletRequest(HttpServletRequest request) {
  	    super(request);
  	  }

  	  @Override
  	  public ServletInputStream getInputStream() throws IOException {
  	    if (cachedBytes == null)
  	      cacheInputStream();

  	      return new CachedServletInputStream();
  	  }

  	  @Override
  	  public BufferedReader getReader() throws IOException{
  	    return new BufferedReader(new InputStreamReader(getInputStream()));
  	  }

  	  private void cacheInputStream() throws IOException {

  	    cachedBytes = new ByteArrayOutputStream();
  	    copy(super.getInputStream(), cachedBytes);
  	    
  	    /*
  	    while (super.getInputStream().available() > 1)
  	    {
  	    	cachedBytes.write(super.getInputStream().read());
  	    }*/
  	  }

  	  /* An inputstream which reads the cached request body */
  	  public class CachedServletInputStream extends ServletInputStream {
  	    private ByteArrayInputStream input;

  	    public CachedServletInputStream() {
  	      /* create a new input stream from the cached request body */
  	      input = new ByteArrayInputStream(cachedBytes.toByteArray());
  	    }

  	    @Override
  	    public int read() throws IOException {
  	      return input.read();
  	    }
  	  }
  	} 
    
    public static long copy(final InputStream input, final OutputStream output) throws IOException 
    {
    	byte[] buffer = new byte[4096];
        long count = 0;
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
}
