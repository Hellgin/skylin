package skylin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


import java.util.List;

import oracle.sql.ROWID;

public class EditViewRowDeleted implements EditViewRowChange 
{

	Object key;
	public EditViewRowDeleted(Object e) 
	{
		key = e;
	}

	public void apply(EditView view,Connection con)  throws SQLException
	{

		List keyItems = (List) key;
		String n = "{call " + view.deleteProcName + "(?";
		for (int i = 0; i < keyItems.size() - 1; i++) {
			n += ",?";
		}

		n += ")}";
		PreparedStatement cs = con.prepareStatement(n);
			
		for (int i = 0; i < keyItems.size();i++)
		{
			cs.setObject(i+1, keyItems.get(i));
		}
		cs.execute();
	    cs.close();

	    /*
		PreparedStatement cs = con.prepareStatement("{call " + view.deleteProcName + "(?,?)}");
		cs.setObject(1, key);
		cs.setString(2, view.getAM().getUsername());
		cs.execute();
	    cs.close();
	    */
		
	}

	public Object getObject() 
	{
		return key;
	}

}
