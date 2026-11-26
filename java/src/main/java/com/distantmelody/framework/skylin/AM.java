package com.distantmelody.framework.skylin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.distantmelody.framework.skylin.bridge.BackendEvalToken;
import com.distantmelody.framework.skylin.bridge.BridgeConnection;
import com.distantmelody.framework.skylin.bridge.BridgeSession;
import com.distantmelody.framework.skylin.bridge.ByteReader;
import com.distantmelody.framework.skylin.bridge.SkylinBridgeVirtualMethods;
import com.distantmelody.framework.skylin.security.QuickRoleLookup;
import com.distantmelody.framework.skylin.server.SkylinDataSource;
import com.distantmelody.library.json.J;
import com.distantmelody.framework.skylin.util.ConnectionLookup;
import com.distantmelody.library.service.ServiceUtil;
import com.distantmelody.framework.skylin.websocket.SessionSocket;

public class AM implements SkylinBridgeVirtualMethods
{
	private static final Logger LOG = LoggerFactory.getLogger(AM.class);

	protected ArrayList<View> views = new ArrayList<View>();
	private String[] viewNames;

	
	private String datasourceName = null;

	
	String username;
	String password;
	
	String testConnectionUrl;
	
	Map viewMap = new ViewMap(this);	
	String lastViewObjectRetrievedName = "";
	View lastViewObjectRetrieved;	

	public boolean initialized;
	public boolean initializedFinal;
	//private int nextBlobId;
	//private ArrayList<Object[]> blobPointers = new ArrayList<Object[]>();
	public String nextName;
	ArrayList<String> messages = new ArrayList<String>();
	SecurityContext securityContext;
	
	public int frontEndApplicationId=-1;
	public int topFrontEndApplicationId;
	private int objectId;
	
	boolean active;
	public BridgeSession session;
	
	public static final int THREAD_NEVER = 0;
	public static final int THREAD_IF_NEEDED = 1;
	public static final int THREAD_ALWAYS = 2;
	
	public void setFrontEndApplicationId(int id)
	{
		frontEndApplicationId = id;
	}
	
	public void setTopFrontEndApplicationId(int id)
	{
		topFrontEndApplicationId = id;
	}
	
	//deprecated use getEditView
	public EditView getEditViewObject(String name)
	{
		return (EditView)getViewObject(name);
	}
	
	public EditView getEditView(String name)
	{
		return (EditView)getView(name);
	}
	
	//deprecated use getView
	public View getViewObject(String name)
	{
		if (name.equals(lastViewObjectRetrievedName))
		{
			return lastViewObjectRetrieved;
		}
		int index = Arrays.binarySearch(viewNames, name);
		if (index < 0)
		{
			throw new SkylinException(this,"Cannot find view object " + name + " in application module " + getClass().getSimpleName());
		}
		lastViewObjectRetrieved = views.get(index);
		lastViewObjectRetrievedName = viewNames[index];
		return lastViewObjectRetrieved;
	}
	
	public View getView(String name)
	{
		if (name.equals(lastViewObjectRetrievedName))
		{
			return lastViewObjectRetrieved;
		}
		int index = Arrays.binarySearch(viewNames, name);
		if (index < 0)
		{
			throw new SkylinException(this,"Cannot find view object " + name + " in application module " + getClass().getSimpleName());
		}
		lastViewObjectRetrieved = views.get(index);
		lastViewObjectRetrievedName = viewNames[index];
		return lastViewObjectRetrieved;
	}

	
	private void initInternal()
	{
		//Util.setSessionObject(getClass().getName(),this);
		viewNames = new String[views.size()];
		for (int i = 0; i < viewNames.length;i++)
		{
			viewNames[i] = views.get(i).getName();
		}
		
		Arrays.sort(viewNames);
		
		ArrayList<View> newViews = new ArrayList<View>();
		
		for (int i = 0; i < viewNames.length;i++)
		{
			loop:
			for (int j = 0; j < views.size();j++)
			{
				if (views.get(j).getName().equals(viewNames[i]))
				{
					newViews.add(views.get(j));
					views.remove(j);
					break loop;
				}
			}
		}

		initialized = true;
		
		views = newViews;
		for (int i = 0; i < views.size();i++)
		{
			//viewMap.put(views.get(i).getClass().getSimpleName(), views.get(i));
			views.get(i).initInternal();
			views.get(i).init();
		}
		

	}
	
	

	public Connection getNewConnection()
	{
		Connection currentConnection;
		if (datasourceName != null)
		{
			try
			{			
				currentConnection = ConnectionLookup.getNewConnection(datasourceName);
			}
			catch (Exception e)
			{
				String message = "Could not get connection from datasource " + datasourceName;
				fatal(logDatabaseError(message, e));
				throw new SkylinException(message, e);
			}
			
		}
		else if (testConnectionUrl != null)
		{
			try 
			{
				currentConnection = DriverManager.getConnection(testConnectionUrl, username, password);
				currentConnection.setAutoCommit(false);
			} 
			catch (Exception e) 
			{
				String message = "Could not create test connection";
				fatal(logDatabaseError(message, e));
				throw new SkylinException(message, e);
			}
		}
		else
		{
			throw new SkylinException(this,"No way to get connections has been set in Application Module " + getClass().getSimpleName());
		}
		return currentConnection;
	}
	
	
	public void setTestConnectionSource(String host, int port,String dbName, String username, String password)
	{
		try {
			 
			Class.forName("oracle.jdbc.driver.OracleDriver");

			testConnectionUrl = "jdbc:oracle:thin:@"+host+":"+port+":"+dbName;
			this.username = username;
			this.password = password;
		}
		catch (Exception e)
		{
			throw new SkylinException(this,"Could not set test connection source: " + e.getMessage());
		}
		initInternal();
		init();
		if (frontEndApplicationId == -1)
		{
			finalInit();
		}
		initializedFinal = true;
	}
	
	public void setDataSource(String datasourceName) 
	{
		this.datasourceName = datasourceName;
		initInternal();
		init();
		if (frontEndApplicationId == -1)
		{
			finalInit();
		}
		initializedFinal = true;
	}
	
	public String getDataSource()
	{
		return datasourceName;
	}
	
	public void finalize()
	{
		try
		{
			//currentStatement.close();
			//currentConnection.close();
		}
		catch (Exception e)
		{
			
		}
	}

	@Deprecated
	public void setCurrentStatement(Statement stmt) 
	{
		
	}
	
	public Map getViews()
	{
		return viewMap;
	}
	
	private class ViewMap extends HashMap
	{
		AM am;
		public ViewMap(AM am)
		{
			this.am = am;
		}
		
		public Object get(Object key)
		{
			return am.getView((String)key);
		}
	}
	
    public boolean userInRole(String role)
    {
    	if (securityContext == null)
    	{
    		return false;
    	}
    	return securityContext.userInRole(role);
    }	
	
	public void init()
	{
		
	}
	
	public void finalInit()
	{
		
	}

/*
	int nextBlobId() 
	{
		nextBlobId++;
		return nextBlobId-1;
	}


	void addBlobPointer(View view, Object key, int column) 
	{
		blobPointers.add(new Object[]{view,key,column});
	}
	
	public byte[] getBlobData(int id)
	{
		Object[] bp = blobPointers.get(id);
		return (byte[]) ((SkylinBlob)((View)bp[0]).getRow(((View)bp[0]).indexOf(bp[1])).getValue((Integer)bp[2])).data;
	}
	*/
	
	public void sendMessage(String type,String data)
	{
		messages.add(type +","+data);
	}
	
	public ArrayList<String> getMessages()
	{
		ArrayList<String> ret = messages;
		messages = new ArrayList<String>();
		return ret;
	}
	
	public void info(String message)
	{
		sendMessage("displaymsg","info,"+message);
	}
	public void warn(String message)
	{
		sendMessage("displaymsg","warn,"+message);
	}
	public void error(String message)
	{
		if (printErrors)
		{
			LOG.error(message);
		}
		sendMessage("displaymsg","error,"+message);
	}
	  
	public void fatal(String message)
	{
		if (printErrors)
		{
			LOG.error(message);
		}
		sendMessage("displaymsg","fatal,"+message);	  
	}
	
    /**
     * Logs a database error under a short reference and returns what the user is told instead: that a database error
     * occurred, with the reference to find it in the log. The driver's text can hold SQL, table and column names and
     * data, so it never goes to the browser.
     */
    public String logDatabaseError(String what, Throwable e)
    {
      String reference = String.format("%08x", Integer.valueOf(ThreadLocalRandom.current().nextInt()));
      LOG.error("{} (reference {})", what, reference, e);
      return "A database error occurred (reference " + reference + ").";
    }

    /** The type of this AM's datasource ({@code oracle}, {@code mysql}, {@code postgresql}), or "" when unknown. */
    protected String getDataSourceType()
    {
      try
      {
    	  return ((SkylinDataSource)ConnectionLookup.getDataSource(datasourceName)).getType();
      }
      catch (Exception e)
      {
    	  LOG.error("Could not read the datasource type for {}", datasourceName, e);
    	  return "";
      }
    }

    /**
     * Shows a message the database raised for the user on purpose: Oracle application errors 20900, 20901 and 20902
     * as error, warning and info, and a PostgreSQL {@code RAISE EXCEPTION} (SQLSTATE P0001) as error. Any other
     * database error is logged and the user sees only {@link #logDatabaseError}'s text.
     */
    public void error(SQLException e)
    {
      String type = getDataSourceType();
      if (type.equals("oracle"))
      {
	      if (e.getErrorCode() == 20900) 
	      {
	        String m = e.getMessage();
	        m = m.substring(0,m.indexOf("\n"));
	        m = m.substring(m.lastIndexOf("ORA-"),m.length());
	        m = m.substring(m.indexOf(" "), m.length());
	        error(m);
	      }
	      else if (e.getErrorCode() == 20901) {
	          String m = e.getMessage();
	          m = m.substring(0,m.indexOf("\n"));
	          m = m.substring(m.lastIndexOf("ORA-"),m.length());
	          m = m.substring(m.indexOf(" "), m.length());
	          warn(m);
	      }
	      else if (e.getErrorCode() == 20902) {
	          String m = e.getMessage();
	          m = m.substring(0,m.indexOf("\n"));
	          m = m.substring(m.lastIndexOf("ORA-"),m.length());
	          m = m.substring(m.indexOf(" "), m.length());
	          info(m); 
	      }
	      else {
	        fatal(logDatabaseError("SQL error " + e.getErrorCode() + " on " + getClass().getSimpleName(), e));
	      }
      }
      else if (type.equals("postgresql") && "P0001".equals(e.getSQLState()))
      {
    	  String m = e.getMessage();
    	  try
    	  {
    		  m = m.substring(m.indexOf("ERROR: ")+"ERROR: ".length(),m.indexOf("Where:"));
    	  }
    	  catch (Exception e2)
    	  {
    		  LOG.error("Could not parse the Postgres error message", e2);
    	  }
    	  error(m);
      }
      else
      {
    	  fatal(logDatabaseError("SQL error " + e.getErrorCode() + " (" + e.getSQLState() + ") on " + getClass().getSimpleName(), e));
      }
      
    }
    
    public String getUsername()
    {
    	if (securityContext == null)
    	{
    		return "TEST2";//remove this code once were sure the bug regarding null securityContext's is resolved.
    	}
    	return securityContext.getUsername();
    }
    
    public void setSecurityContext(SecurityContext sc)
    {
    	securityContext = sc; 
    }
    
    public SecurityContext getSecurityContext()
    {
    	return securityContext;
    }

	public boolean isActive() 
	{
		return active;
	}

	public void active()
	{
		active = true;
	}
	
	public void update() 
	{

	}
	
	@Deprecated /*threadMode param not needed anymore.*/
	public Object eval(String code,int threadMode)
	{
		return eval(code);
	}
	
	public Object eval(String code)
	{
		return eval(code,false);
	}
	
	public Object eval(String code, boolean forceNewRequest)
	{
		try
		{
			if (forceNewRequest || !BridgeConnection.isSameRequestEvalSupported())
			{
				if (BridgeConnection.isBridgeConnection())
				{
					new Thread()
					{
						public void run()
						{
							evalNewRequest(code);
						}
					}.start();
					return null;
				}
				else
				{
					return evalNewRequest(code);
				}
			}
			else
			{
				try
				{
					String ret = BridgeConnection.getBridgeConnection().sendAndReceive(("1"+frontEndApplicationId+","+code).getBytes(StandardCharsets.UTF_8));
					//return session.stringToObject(ret);
					return ret;
				}
				catch (IOException e)
				{
					LOG.error("Bridge eval failed for application {}", Integer.valueOf(frontEndApplicationId), e);
					return null;
				}	
			}
		}
		catch (Exception e)
		{
			LOG.error("Eval failed on {}", getClass().getSimpleName(), e);
			return null;
		}
	}

	private Object evalNewRequest(String code)
	{
		//String base = "http://localhost"+session.getContext().getContextPath()+"/skylin/websocket/BackendEval.php";
		String base = "http://localhost/skylin/websocket/BackendEval.php";
		String input = BackendEvalToken.get()+","+session.getId()+","+frontEndApplicationId+","+code;
			
		//The answer is <byte length of the result>,<the encoded result><the browser messages>. The length counts
		//bytes (PHP's strlen), so the answer is cut as bytes and only the two parts are decoded.
		ByteReader ret = new ByteReader(ServiceUtil.dataService(base, input.getBytes(StandardCharsets.UTF_8)));
		
		int aLength = ret.integer(); 
		ByteReader a = ret.slice(aLength);
		
		if (isWebsocketAvailable())
		{
			String b = ret.string();
			session.sendWebSocketText(topFrontEndApplicationId, b);
		}
		return session.bytesToObject(a);
	}
	
	public void setBridgeSession(BridgeSession s)
	{
		session = s;
	}
	
	public boolean isWebsocketAvailable()
	{
		return session.isWebsocketAvailable(topFrontEndApplicationId);
	}
	
	public long websocketUnavailableFor()
	{
		return session.websocketUnavailableFor(topFrontEndApplicationId);
	}
	
	public void resetWebSocketUnavailableCount()
	{
		session.resetWebSocketUnavailableCount(topFrontEndApplicationId);
	}

	public final byte[] receiveMessage(J in) {
		return receiveMessage(in,"DEFAULT");
	}
	
	public byte[] receiveMessage(J in, String channel) {
		return null;
	}
	
	public byte[] receiveBinaryMessage(byte[] message, String channel) {
		return null;
	}
	
	public void customMessage(J message)
	{
		customMessage(message.toString(),"DEFAULT");
	}
	
	public void customMessage(J message,String channel)
	{
		customMessage(message.toString(),channel);
	}
	public void customMessage(String message)
	{
		customMessage(message,"DEFAULT");
	}
	
	public void setWsCookie(String name,String value,long date)
	{
		String message = "setCookie,"+name+";"+value+";"+date;
		message = "1,"+message.length()+"-"+message;//the browser counts UTF-16 code units, which is what String.length() counts
		session.sendWebSocketText(topFrontEndApplicationId, message);
	}

	public void customMessage(String message,String channel)
	{
		String m = "custom,"+objectId+","+channel+","+message;
		m = "1,"+m.length()+"-"+m;//the browser counts UTF-16 code units, which is what String.length() counts
		//session.getSessionSocket().sendText(m);
		session.sendWebSocketText(topFrontEndApplicationId, m);
	}
	
	public void customBinaryMessage(byte[] message,String channel)
	{
		byte[] header = (objectId+","+channel).getBytes(StandardCharsets.UTF_8);
		byte[] m = new byte[1 + header.length + message.length]; 
		m[0] = (byte) header.length;
		System.arraycopy(header, 0, m, 1, header.length);
		System.arraycopy(message, 0, m, 1+header.length, message.length);
		
		session.sendWebSocketBinary(topFrontEndApplicationId, m);
	}

	public void setObjectId(int id)
	{
		objectId = id;
	}
	
	public int getObjectId() {
		return objectId;
	}
	
	public void webSocketForceRefresh()
	{
		session.sendWebSocketText(topFrontEndApplicationId, "FORCE_REFRESH");
	}

	@Override
	public Object handleVirtualMethodCall(String name, Object[] params, boolean[] matchFound) {
		if (params.length == 0)
		for (View v:views)
		{
			if (name.equals(v.getName()))
			{
				matchFound[0] = true;
				return v;
			}
		}
		return null;
	}
	
	public QuickRoleLookup role()
	{
		return securityContext.role();
	}
	
	boolean printErrors;
	public void printErrors()
	{
		printErrors = true;
	}
}
