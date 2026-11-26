package com.distantmelody.framework.skylin.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import com.distantmelody.framework.skylin.AM;
import com.distantmelody.framework.skylin.server.SkylinConnection;

public class SqlUtil 
{
	public static void safeClose(ResultSet rs,Statement s, Connection con)
	{
		try{rs.close();}catch(Exception e){};
		try{s.close();}catch(Exception e){};
		try{con.rollback();}catch(Exception e){};
		try{con.close();}catch(Exception e){};
	}
	
    public static long nextVal(String sequenceName, AM am, Connection con)
    {
        PreparedStatement stmt = null;
		ResultSet rs = null;
         String query;
         if (con instanceof SkylinConnection && ((SkylinConnection)con).getDataSource().getType().equals("postgresql"))
         {
        	 query = "select nextval('"+sequenceName+"');";
         }
         else
         {
        	 query = "select "+sequenceName+".nextval from dual";
         }
           try {
               stmt = con.prepareStatement(query);
               stmt.execute();
               rs = stmt.getResultSet();
               rs.next();
               return rs.getLong(1);
           }
           catch (SQLException e) 
           {
        	   String what = "Error retrieving nextval for sequence " + sequenceName;
        	   if (am != null)am.fatal(am.logDatabaseError(what, e));
        	   throw new RuntimeException(what, e);
               //return -1;
           }
           finally
           {
        	   SqlUtil.safeClose(rs, stmt, null);
           }
    }
    
    public static long nextVal(String sequenceName, AM am)
    {
    	Connection con = null;
    	try
    	{
    		con = am.getNewConnection();
    		return nextVal(sequenceName,am,con);
    	}
    	finally
    	{
    		safeClose(null, null, con);
    	}
    }
    
    public static Timestamp getCurrentTimestamp()
    {
    	java.util.Date today = new java.util.Date();
        return new java.sql.Timestamp(today.getTime());
    }
}
