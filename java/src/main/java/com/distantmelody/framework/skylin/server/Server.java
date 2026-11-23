package com.distantmelody.framework.skylin.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.distantmelody.framework.skylin.SecurityContext;
import com.distantmelody.framework.skylin.SkylinClassLoader;
import com.distantmelody.framework.skylin.bridge.BridgeListener;
import com.distantmelody.framework.skylin.bridge.BridgeSessionHandler;
import com.distantmelody.library.json.J;

public class Server 
{
	private static final Logger LOG = LoggerFactory.getLogger(Server.class);

	public static HashMap<String,String> customParams = new HashMap<String,String>();
	public static HashMap<String,String> frameworkParams = new HashMap<String,String>();
	public static HashMap<String,Object> globalProperties = new HashMap<String,Object>();
	
	public static ArrayList<DataSource> dataSources = new ArrayList<DataSource>();
	
	public static BridgeSessionHandler bridgeSessionHandler = new BridgeSessionHandler();
	
	private static String outPath,errPath;
	
	public static void main(String args[])
	{
		waitForPort(1001);
		long startTime = System.currentTimeMillis();

		Properties props = System.getProperties();
		props.setProperty("java.naming.factory.initial", "com.distantmelody.framework.skylin.server.SkylinContextFactory");
		
		//HashMap<String,String> customParams = new HashMap<String,String>();
		//HashMap<String,String> frameworkParams = new HashMap<String,String>();
		
		String line;
		BufferedReader br = null;
		try
		{
			br = new BufferedReader(new InputStreamReader(new FileInputStream("CustomParams.cnf"), StandardCharsets.UTF_8));
			while ((line = br.readLine()) != null) {
				if (line.trim().length() > 0 && !line.trim().startsWith("#"))
				{
					customParams.put(line.substring(0,line.indexOf(":")), line.substring(line.indexOf(":")+1));
				}
		    }
			br = new BufferedReader(new InputStreamReader(new FileInputStream("FrameworkParams.cnf"), StandardCharsets.UTF_8));
			while ((line = br.readLine()) != null) {
				if (line.trim().length() > 0 && !line.trim().startsWith("#"))
				{
					frameworkParams.put(line.substring(0,line.indexOf(":")), line.substring(line.indexOf(":")+1));
				}
		    }
			String datasourceFile = frameworkParams.get("DATASOURCE_FILE");
			if (datasourceFile == null)
			{
				datasourceFile = "datasource.json";
			}
			if (!"disabled".equals(datasourceFile))
			{
				J datasource = J.fromString(new String(Files.readAllBytes(new File(datasourceFile).toPath()), StandardCharsets.UTF_8));
				for (J ds:datasource.get("datasource").valuesj())
				{
					String type = ds.contains("type") ? ds.getString("type") : null;
					addDataSource(ds.getString("name"), ds.getString("server"), ds.getInt("port"), ds.getString("database"),
							ds.getString("user"), ds.getString("password"), ds.getInt("min"), ds.getInt("max"), type);
				}
			}
			runConfigClasses();
		}
		catch (Exception e){e.printStackTrace();System.exit(0);}

		//The TCP bridge port is only used (and only has to be free) on Windows versions without Unix sockets.
		if (BridgeListener.useTcpTransport()) waitForPort(BridgeListener.TCP_PORT);

		String logPath = frameworkParams.get("LOG_PATH");
		
		if (logPath != null)
		{
			try
			{
				outPath = logPath+"/backend_out "+new SimpleDateFormat("dd-MM-yyyy HH_mm_ss").format(new Date())+".txt";
				errPath = logPath+"/backend_errors "+new SimpleDateFormat("dd-MM-yyyy HH_mm_ss").format(new Date())+".txt";
				
				FileOutputStream file = new FileOutputStream(outPath);
			    SplitPrintStream tee = new SplitPrintStream(file, System.out);
			    System.setOut(tee);
			    
				file = new FileOutputStream(errPath);
			    tee = new SplitPrintStream(file, System.err, true);
			    System.setErr(tee);
			}
			catch (Exception e)
			{
				LOG.error("unable to open log file for writing", e);
			}
		}
		
		SecurityContext.createProviders();
		SkylinClassLoader.setApplicationPaths();
		
		BridgeListener bl = new BridgeListener();
		SkylinServletContext servletContext = new SkylinServletContext();
		servletContext.params = customParams;
		bl.contextInitialized(new ServletContextEvent(servletContext));
		
		boolean isWindows = System.getProperty("os.name") != null && System.getProperty("os.name").toLowerCase().contains("windows");
		// Only manage Apache ourselves when APACHE_EXEC is configured; otherwise the web server runs as a separate process.
		if (isWindows && frameworkParams.containsKey("APACHE_EXEC"))
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
				if (frameworkParams.containsKey("PHP_LOG_PATH"))
				{
					new ApacheOutput(frameworkParams.get("PHP_LOG_PATH"));
				}
				else
				{
					new ApacheOutput("C:/php/logs/error.log");
				}
				String path = "C:\\Apache24\\bin\\httpd.exe";
				if (frameworkParams.containsKey("APACHE_EXEC"))
				{
					path = frameworkParams.get("APACHE_EXEC");
				}
		        String[] cmd = {path};
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
				LOG.error("Unable to start apache. Resolve the issue or run it in a seperate process.", e);
			}
		}
		
		if (frameworkParams.containsKey("STARTUP_THREAD"))
		{
			try
			{
				for (String s:frameworkParams.get("STARTUP_THREAD").split(","))
				{
					Runnable runnable = ((Runnable)Class.forName(s).newInstance());
					Thread thread = new Thread(runnable);
					thread.start();
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		LOG.info("Server is ready. Startup time: " + (System.currentTimeMillis()-startTime)+"ms");
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
	
	public static String getOutPath()
	{
		return outPath;
	}
	
	public static String getErrPath()
	{
		return errPath;
	}

	/**
	 * Runs each {@code CONFIG_CLASS} on this thread, in order, immediately after the datasource file step.
	 * The value is a comma-separated list of {@link Runnable} class names, the same shape as {@code STARTUP_THREAD}.
	 * {@code run()} is called directly, so the bridge does not open until every class has returned.
	 */
	private static void runConfigClasses() throws Exception
	{
		if (!frameworkParams.containsKey("CONFIG_CLASS"))
		{
			return;
		}
		for (String name : frameworkParams.get("CONFIG_CLASS").split(","))
		{
			name = name.trim();
			if (name.isEmpty())
			{
				continue;
			}
			Runnable runnable = (Runnable) Class.forName(name).newInstance();
			runnable.run();
		}
	}

	/**
	 * Creates one pool the same way the datasource JSON loader does and registers it for JNDI lookup.
	 * {@code type} may be null (SkylinDataSource keeps its default).
	 */
	public static void addDataSource(String name, String server, int port, String database, String user, String password, int min, int max, String type)
	{
		SkylinDataSource sds = new SkylinDataSource(name, server, port, database, user, password);
		sds.setMinConnections(min);
		sds.setMaxConnections(max);
		if (type != null)
		{
			sds.setType(type);
		}
		dataSources.add(sds);
	}
	
	private static void waitForPort(int port) {
        boolean portAvailable = false;
        while (!portAvailable)
        {
        	portAvailable = true;
        	ServerSocket socket = null;

        	    try {
        	        socket = new ServerSocket();
        	        socket.setReuseAddress(true);
        	        socket.bind(new InetSocketAddress(port));
        	      } catch (Exception e) {
        	    	  portAvailable = false;
        	    	  LOG.warn("port " + port + " is not available, retrying in 20 seconds");
        	    	  try{Thread.sleep(20*1000);}catch (Exception e2) {}
        	      }
        	    finally
        	    {
        	      if (socket != null) {
        	        try {
        	          socket.close();
        	        } catch (Exception e) {
        	        }
        	      }
        	    }
        }
	}
}