package skylin;

import java.io.ByteArrayInputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;


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
		
		int info = -1;
		int warn = -1;
		int error = -1;
		int out = -1;
		
			CallableStatement ps = con.prepareCall(n);
			for (int j = 0; j < view.updateArgs.length; j++) {
        
				if (!view.updateArgs[j].startsWith("#")) {
				Object value = row.getValue(view.updateArgs[j]);
				if (value instanceof SkylinBlob)
				{
					SkylinBlob b = (SkylinBlob) value;
					ps.setBinaryStream(j+1, new ByteArrayInputStream(b.getData()), b.getData().length);
				}
				else
				{
					ps.setObject(j + 1, value);
				}
				} 
				else 
				{
					if (view.updateArgs[j].startsWith("#user")) {
						ps.setObject(
								j + 1,
								view.getAM().getUsername());
					}
					else if (view.updateArgs[j].startsWith("#info")) {
						ps.registerOutParameter(
								j + 1,
								Types.VARCHAR);
						info = j+1;
					}
					else if (view.updateArgs[j].startsWith("#warn")) {
						ps.registerOutParameter(
								j + 1,
								Types.VARCHAR);
						warn = j+1;
					}
					else if (view.updateArgs[j].startsWith("#error")) {
						ps.registerOutParameter(
								j + 1,
								Types.VARCHAR);
						error = j+1;
					}
					else if (view.updateArgs[j].startsWith("#out")) {
						ps.registerOutParameter(
								j + 1,
								Types.VARCHAR);
						out = j+1;
					}
				}
			}
			ps.execute();
			if (info != -1)
			{
				String msg = ps.getString(info);
				if (msg != null)
				{
					row.getAM().info(msg);
				}
			}
			if (warn != -1)
			{
				String msg = ps.getString(warn);
				if (msg != null)
				{
					row.getAM().warn(msg);
				}
			}
			if (error != -1)
			{
				String msg = ps.getString(error);
				if (msg != null)
				{
					row.getAM().error(msg);
				}
			}
			if (out != -1)
			{
				String msg = ps.getString(out);
				if (msg != null)
				{
					row.getEditView().addOut(msg);
				}
			}
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
