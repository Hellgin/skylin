package com.distantmelody.framework.skylin.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.distantmelody.framework.skylin.server.Server;


public class ConnectionLookup 
{
	private static final Logger LOG = LoggerFactory.getLogger(ConnectionLookup.class);

	static private String datasourceRef = "java:comp/env/jdbc/";
	private static InitialContext initialContext;
	
	static
	{
		try 
		{
			initialContext = new InitialContext();
		} catch (NamingException e) 
		{
			LOG.error("Could not create the JNDI context for datasource lookup", e);
		}
	}
	
    public static Connection getNewConnection(String datasource) throws SQLException, NamingException
    {		
			DataSource ds = (DataSource) initialContext.lookup(datasourceRef + datasource);
			Connection con = ds.getConnection();
			con.setAutoCommit(false);
			return con;
    }
    
    public static DataSource getDataSource(String datasource) throws NamingException
    {
    	return (DataSource) initialContext.lookup(datasourceRef + datasource);
    }
    
    public static List<DataSource> getAllDatasources() throws NamingException
    {
    	return Server.dataSources;
    }
}
