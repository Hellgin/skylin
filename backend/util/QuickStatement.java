package skylin.util;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import skylin.AM;

public class QuickStatement
{
	Connection con;
	CallableStatement ps;
	boolean isFunction;
	boolean closeWhenDone;
	boolean commitWhenDone;
	
	public QuickStatement(AM am,String s) throws SQLException
	{
		con = am.getNewConnection();
		ps = con.prepareCall(s);
		closeWhenDone = true;
	}
	
	public QuickStatement(Connection con,String s) throws SQLException
	{
		this.con = con;
		ps = con.prepareCall(s);
	}
	
	
	public void setObject(int index, Object value) throws SQLException
	{
		ps.setObject(index, value);
	}
	public void registerReturnParameter(int returnType) throws SQLException
	{
		ps.registerOutParameter(1,returnType);
		isFunction = true;
	}
	
	public void commitWhenDone()
	{
		commitWhenDone = true;
	}
	
	public Object execute() throws SQLException
	{
		try
		{
			ps.execute();
			if (isFunction)
			{
				return ps.getObject(1);
			}
			return null;
		}
		finally
		{
			if (commitWhenDone)
			{
				 try{con.commit();}catch(Exception e){e.printStackTrace();}; 
			}
            try{ps.close();}catch(Exception e){}; 
            if (closeWhenDone)
            {
            	try{con.close();}catch(Exception e){};
            }
		}
	}
}
