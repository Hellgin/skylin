package skylin.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import skylin.AM;

public class SqlUtil 
{
	public static void safeClose(ResultSet rs,Statement s, Connection con)
	{
		try{rs.close();}catch(Exception e){};
		try{s.close();}catch(Exception e){};
		try{con.rollback();}catch(Exception e){};
		try{con.close();}catch(Exception e){};
	}
	
    public static int nextVal(String sequenceName, AM am, Connection con)
    {
        PreparedStatement stmt = null;
           String query = "select "+sequenceName+".nextval from dual";
           try {
               stmt = con.prepareStatement(query);
               stmt.execute();
               ResultSet rs = stmt.getResultSet();
               rs.next();
               return rs.getInt(1);
           }
           catch (SQLException e) 
           {
        	   am.fatal(e.getMessage());
               return -1;
           }
    }
    
    public static int nextVal(String sequenceName, AM am)
    {
    	Connection con = am.getNewConnection();
    	int ret = nextVal(sequenceName,am,con);
    	safeClose(null, null, con);
    	return ret;
    }
    
    public static Timestamp getCurrentTimestamp()
    {
    	java.util.Date today = new java.util.Date();
        return new java.sql.Timestamp(today.getTime());
    }
}
