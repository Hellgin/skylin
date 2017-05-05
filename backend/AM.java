package skylin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.java_websocket.exceptions.WebsocketNotConnectedException;

import skylin.bridge.BridgeConnection;
import skylin.bridge.BridgeSession;
import skylin.services.J;
import skylin.util.ConnectionLookup;
import skylin.util.ServiceUtil;
import skylin.websocket.SessionSocket;

public class AM
{
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
	public boolean initializedStep2;
	//private int nextBlobId;
	//private ArrayList<Object[]> blobPointers = new ArrayList<Object[]>();
	public String nextName;
	ArrayList<String> messages = new ArrayList<String>();
	SecurityContext securityContext;
	
	public int frontEndApplicationId;
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
				throw new SkylinException(this,"Could not get connection from datasource " + datasourceName + ": " + e.getMessage());
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
				throw new SkylinException(this,"Could not create test connection: " + e.getMessage());
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
		initializedStep2 = true;
	}
	
	public void setDataSource(String datasourceName) 
	{
		this.datasourceName = datasourceName;
		initInternal();
		init();
		initializedStep2 = true;
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
		sendMessage("displaymsg","error,"+message);
	}
	  
	public void fatal(String message)
	{
		sendMessage("displaymsg","fatal,"+message);	  
	}
	
    public void error(SQLException e)
    {
      System.out.println(e.getErrorCode());
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
        fatal(e.getMessage());
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
	
	@Deprecated
	public void websocketRefreshComponent(String id)
	{
		System.out.println("websocketRefreshComponent has been removed from AM. Use eval instead to refresh components via websockets.");
	}
	
	public Object eval(String code)
	{
		return eval(code,THREAD_IF_NEEDED);
	}
	
	public Object eval(final String code,int threadMode)
	{
		if (threadMode == THREAD_NEVER)
		{
			return evalInternal(code);
		}
		else if (threadMode == THREAD_IF_NEEDED)
		{
			if (BridgeConnection.isBridgeConnection())
			{
				new Thread()
				{
					public void run()
					{
						evalInternal(code);
					}
				}.start();
				return null;
			}
			else
			{
				return evalInternal(code);
			}
		}
		else if (threadMode == THREAD_ALWAYS)
		{
			new Thread()
			{
				public void run()
				{
					evalInternal(code);
				}
			}.start();
			return null;
		}
		System.out.println("Nonexistant threadmode selected. Eval did nothing.");
		return null;
	}
	
	private Object evalInternal(String code)
	{
		if (!isWebsocketAvailable()) return null;
		
		
		//String base = "http://localhost"+session.getContext().getContextPath()+"/skylin/websocket/BackendEval.php";
		String base = "http://localhost/skylin/websocket/BackendEval.php";
		String input = session.getId()+","+frontEndApplicationId+","+code;
			
		String[] ret = ServiceUtil.stringService(base, input).split(",",2);
		
		int aLength = Integer.parseInt(ret[0]); 
		String a = ret[1].substring(0, aLength);
		String b = ret[1].substring(aLength, ret[1].length());
		
		session.sendWebSocketText(topFrontEndApplicationId, b);

		
		return session.stringToObject(a);
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

	public final void receiveMessage(J in) {
		receiveMessage(in,"DEFAULT");
	}
	
	public void receiveMessage(J in, String channel) {
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
	
	public void customMessage(String message,String channel)
	{
		String m = "custom,"+objectId+","+channel+","+message;
		m = "1,"+m.length()+"-"+m;
		//session.getSessionSocket().sendText(m);
		session.sendWebSocketText(topFrontEndApplicationId, m);
	}
	
	public void setObjectId(int id)
	{
		objectId = id;
	}
	
	public int getObjectId() {
		return objectId;
	}
	
}
