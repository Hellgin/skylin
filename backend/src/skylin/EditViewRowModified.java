package skylin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class EditViewRowModified implements EditViewRowChange
{
	EditViewRow row;
	public EditViewRowModified(ViewRow e) 
	{
		row = (EditViewRow) e;
	}

	public void apply(EditView view,Connection con) throws SQLException
	{
		String n = "{call " + view.updateProcName + "(?";
		for (int i = 0; i < view.updateArgs.length - 1; i++) {
			n += ",?";
		}

		n += ")}";
		
			PreparedStatement ps = con.prepareStatement(n);
			for (int j = 0; j < view.updateArgs.length; j++) {
        
				if (!view.updateArgs[j].startsWith("#")) {
					ps.setObject(j + 1, row.getValue(view.updateArgs[j]));
				} else {
					if (view.updateArgs[j].startsWith("#user")) {
						ps.setObject(
								j + 1,
								view.getAM().getUsername());
					}
				}
			}
			ps.execute();
            ps.close();

            
            //this part might need some reworking. or more...
            if (row.viewLinks != null)
            {
	            for (ViewLink vl:row.viewLinks)
	            {
	            	if (vl.linkedView instanceof EditView)
	            	{
	            		int ret = ((EditView)vl.linkedView).save(con);
	            		if (ret != 0)
	            		{
	            			row.getEditView().lastSaveErrorFree = false;
	            		}
	            	}
	            }
            }
            
	}

	public Object getObject() 
	{
		return row;
	}

}
