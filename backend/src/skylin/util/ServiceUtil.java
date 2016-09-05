package skylin.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;



import skylin.services.J;


public class ServiceUtil 
{
	
	public static InputStream streamService(String url,String postData,String proxyAddress, int proxyPort)
	{
		try
		{
			URL obj = new URL(url);
			
			HttpURLConnection con = null;
			if (proxyAddress != null)
			{
				Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyAddress, proxyPort));
				con = (HttpURLConnection) obj.openConnection(proxy);
			}
			else
			{
				con = (HttpURLConnection) obj.openConnection();
			}
		
			con.setRequestProperty("Content-Type","application/json");
			//con.setRequestProperty("Content-Type","text/plain");
	 
			con.setRequestMethod("POST");
	 
			String urlParameters=postData;
	 
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();
			
	 
			int responseCode = con.getResponseCode();
			if (responseCode != 200)
			{
				return standardErrorResponse("Error in service: " + responseCode);
			}
	 
			return con.getInputStream();
		}
		catch(MalformedURLException e)
		{
			return standardErrorResponse("Malformed URL: " + url);
		}
		catch(IOException e)
		{
			return standardErrorResponse("Connection error: " + e.getMessage());
		}
		catch(Exception e)
		{
			return standardErrorResponse("Unknown error: " + e.getMessage());
		}

	}

	private static InputStream standardErrorResponse(String error) 
	{
		String json = "{\"status\":\"false\",\"error_description\":\""+error+"\"}";
		return new ByteArrayInputStream(json.getBytes());
	}
	
	public static byte[] dataService(String url,String postData,String proxyAddress, int proxyPort)
	{
		InputStream is = streamService(url,postData,proxyAddress,proxyPort);
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[32];

		try
		{
			while ((nRead = is.read(data, 0, data.length)) != -1) {
				  buffer.write(data, 0, nRead);
			}
	
			buffer.flush();
		}
		catch(IOException e)
		{
			e.printStackTrace();//should not be possible
		}

		return buffer.toByteArray();	
	}
	
	public static String stringService(String url,String postData)
	{
		return new String(dataService(url,postData,null,0));
	}
	
	public static String stringService(String url,String postData,String proxyAddress, int proxyPort)
	{
		return new String(dataService(url,postData,proxyAddress,proxyPort));
	}
	
	public static J JService(String url,J j)
	{
		return J.fromString(stringService(url,j+""));
	}
	

	public static J JService(String url, J j, String proxyAddress, int proxyPort) {
		return J.fromString(stringService(url,j+"",proxyAddress,proxyPort));
	}
	/*
	public static OutContainer gsonService(String url,InContainer in)
	{
		Gson gson = new GsonBuilder().create();
		return (OutContainer) gson.fromJson(new InputStreamReader(streamService(url,gson.toJson(in))), in.outClass);
	}
	
	public static InContainer getIn (HttpServletRequest request,Class inContainer) throws IOException
	{
		Gson gson = new GsonBuilder().create();
		return (InContainer) gson.fromJson(new InputStreamReader(request.getInputStream()), inContainer);
	}
	
	public static byte[] toJson(Object o)
	{
		Gson gson = new GsonBuilder().create();
		return gson.toJson(o).getBytes();
	}*/

	
}
