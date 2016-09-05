package skylin.server;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import skylin.bridge.BridgeListener;
import skylin.services.J;

public class Server 
{
	public static HashMap<String,String> customParams = new HashMap<String,String>();
	public static HashMap<String,String> frameworkParams = new HashMap<String,String>();
	
	public static void main(String args[])
	{
		long startTime = System.currentTimeMillis();
		Properties props = System.getProperties();
		props.setProperty("java.naming.factory.initial", "skylin.server.SkylinContextFactory");
		
		//HashMap<String,String> customParams = new HashMap<String,String>();
		//HashMap<String,String> frameworkParams = new HashMap<String,String>();
		
		String line;
		BufferedReader br = null;
		try
		{
			br = new BufferedReader(new InputStreamReader(new FileInputStream("CustomParams.cnf")));
			while ((line = br.readLine()) != null) {
				if (line.trim().length() > 0 && !line.trim().startsWith("#"))
				{
					customParams.put(line.substring(0,line.indexOf(":")), line.substring(line.indexOf(":")+1));
				}
		    }
			br = new BufferedReader(new InputStreamReader(new FileInputStream("FrameworkParams.cnf")));
			while ((line = br.readLine()) != null) {
				if (line.trim().length() > 0 && !line.trim().startsWith("#"))
				{
					frameworkParams.put(line.substring(0,line.indexOf(":")), line.substring(line.indexOf(":")+1));
				}
		    }
			J datasource = J.fromString(new String(Files.readAllBytes(new File("datasource.json").toPath())));
			for (J ds:datasource.get("datasource").valuesj())
			{
				SkylinDataSource sds = new SkylinDataSource(ds.getString("name"),ds.getString("server"),ds.getInt("port"),ds.getString("database"),ds.getString("user"),ds.getString("password"));
				sds.setMinConnections(ds.getInt("min"));
				sds.setMaxConnections(ds.getInt("max"));
			}
		}
		catch (Exception e){e.printStackTrace();System.exit(0);}

		
		BridgeListener bl = new BridgeListener();
		SkylinServletContext servletContext = new SkylinServletContext();
		servletContext.params = customParams;
		bl.contextInitialized(new ServletContextEvent(servletContext));
		/*
		if (System.getProperty("os.name") != null && System.getProperty("os.name").toLowerCase().contains("windows"))
		{
			try
			{
				if (isProcessRunning("httpd.exe"))
				{
					killProcess("httpd.exe");
					while(isProcessRunning("httpd.exe"))
					{
						Thread.yield();
					}
				}
				new ApacheOutput("C:/php/logs/error.log");
		        String[] cmd = { "C:\\Apache24\\bin\\httpd.exe"};
		        final Process httpd = Runtime.getRuntime().exec(cmd);
		        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
		            @Override
		            public void run() {
		            	httpd.destroy();
		            }
		        }));
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to start apache. Resolve the issue or run it in a seperate process.");
			}
		}
		else
		{
			try //Quick solution to add mising fonts. Think of something better...
			{
				GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(Font.createFont(Font.TRUETYPE_FONT, new FileInputStream("lib/fonts/arial.ttf")));
			} 
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		*/
		System.out.println("Server is ready. Startup time: " + (System.currentTimeMillis()-startTime)+"ms");
	}
	
	private static final String TASKLIST = "tasklist";
	private static final String KILL = "taskkill /F /IM ";

	public static boolean isProcessRunning(String serviceName) throws Exception {

	 Process p = Runtime.getRuntime().exec(TASKLIST);
	 BufferedReader reader = new BufferedReader(new InputStreamReader(
	   p.getInputStream()));
	 String line;
	 while ((line = reader.readLine()) != null) {

	  //System.out.println(line);
	  if (line.contains(serviceName)) {
	   return true;
	  }
	 }

	 return false;

	}

	public static void killProcess(String serviceName) throws Exception {

	  Runtime.getRuntime().exec(KILL + serviceName);

	 }
}