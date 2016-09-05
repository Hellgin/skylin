package skylin.server;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class SkylinDataSource implements DataSource{

	String name;
	String host;
	int port;
	String dbName;
	String username;
	String password;
	
	ConcurrentLinkedQueue<SkylinConnection> pool = new ConcurrentLinkedQueue<SkylinConnection>();
	int currentSize;
	
	//int maxSize = 50;
	//int minSize = 40;
	int maxSize = 12;
	int minSize = 10;
	
	DatabaseConnector connector;
	
	public SkylinDataSource(String name,String host, int port,String dbName, String username, String password)
	{
		this.name = name;
		this.host = host;
		this.port = port;
		this.dbName = dbName;
		this.username = username;
		this.password= password;
		try 
		{
			new InitialContext().bind("java:comp/env/jdbc/"+name, this);
		} 
		catch (NamingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		connector = new DatabaseConnector(this);
		
	}
	
	@Override
	public PrintWriter getLogWriter() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setLogWriter(PrintWriter arg0) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLoginTimeout(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isWrapperFor(Class<?> arg0) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Connection getConnection() throws SQLException {
		// TODO Auto-generated method stub
		SkylinConnection ret = null;
		outer:
		while (true)
		{
			ret = pool.poll();
			if (ret != null)
			{
				try
				{
					ret.inPool = false;
					if (!ret.isValid(10))
					{
						try{ret.kill();}catch (Exception e){};
						continue outer;
					}
					return ret;
				}
				catch (SQLException e)
				{
					try{ret.kill();}catch (Exception e2){};
					continue outer;
				}
			}
			else
			{
				System.out.println("WARNING: Datasource " + name + " is out of available connections. Thread is being blocked.");
				try{Thread.sleep(5000);}catch (InterruptedException e){e.printStackTrace();}
			}
		}
	}

	@Override
	public Connection getConnection(String username, String password)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setMinConnections(int min) {
		minSize = min;
	}

	public void setMaxConnections(int max) {
		maxSize = max;
	}

}
