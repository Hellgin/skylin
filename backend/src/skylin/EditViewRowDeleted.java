package skylin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


import oracle.sql.ROWID;

public class EditViewRowDeleted implements EditViewRowChange 
{

	ROWID key;
	public EditViewRowDeleted(ROWID e) 
	{
		key = e;
	}

	public void apply(EditView view,Connection con)  throws SQLException
	{
		PreparedStatement cs = con.prepareStatement("{call " + view.deleteProcName + "(?,?)}");
		cs.setObject(1, key);
		cs.setString(2, view.getAM().getUsername());
		cs.execute();
        cs.close();
	}

	public Object getObject() 
	{
		return key;
	}

}
