package skylin;

import java.sql.Connection;
import java.sql.SQLException;

public interface EditViewRowChange 
{
	public void apply(EditView view,Connection con) throws SQLException;
	public Object getObject();
}
