package skylin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import oracle.sql.ROWID;
import skylin.util.SqlUtil;
import skylin.util.Util;


public class EditView extends View {

	public EditView(AM am) 
	{
		super(am);
		setRowTypeLater = EditViewRow.class;
	}

	private ArrayList<EditViewRowChange> changes = new ArrayList<EditViewRowChange>();
	public ArrayList<ViewRow> modifiedRows = new ArrayList<ViewRow>()
			{
				public boolean add(ViewRow e)
				{
					changes.add(new EditViewRowModified(e));
					return super.add(e);
				}
				
				public boolean addAll(Collection<? extends ViewRow> c)
				{
					for (ViewRow e:c)
					{
						changes.add(new EditViewRowModified(e));
					}
					return super.addAll(c);
				}
				
				public void clear()
				{
					for (int i = 0; i < changes.size();i++)
					{
						if (changes.get(i) instanceof EditViewRowModified)
						{
							changes.remove(i);
							i--;
						}
					}
					super.clear();
				}
				
				public boolean remove(Object e)
				{
					loop:
					for (int i = 0; i < changes.size();i++)
					{
						if (changes.get(i).getObject() == e)
						{
							changes.remove(i);
							break loop;
						}
					}
					return super.remove(e);
				}
				
				public ViewRow remove(int index)
				{
					ViewRow e = get(index);
					loop:
					for (int i = 0; i < changes.size();i++)
					{
						if (changes.get(i).getObject() == e)
						{
							changes.remove(i);
							break loop;
						}
					}
					return super.remove(index);
				}
			};
	public ArrayList<ROWID> deletedKeys = new ArrayList<ROWID>()
			{
				public boolean add(ROWID e)
				{
					changes.add(new EditViewRowDeleted(e));
					return true;
				}
				public boolean addAll(Collection<? extends ROWID> c)
				{
					for (ROWID e:c)
					{
						changes.add(new EditViewRowDeleted(e));
					}
					return super.addAll(c);
				}
				
				public void clear()
				{
					for (int i = 0; i < changes.size();i++)
					{
						if (changes.get(i) instanceof EditViewRowDeleted)
						{
							changes.remove(i);
							i--;
						}
					}
					super.clear();
				}
				
				public boolean remove(Object e)
				{
					loop:
					for (int i = 0; i < changes.size();i++)
					{
						if (changes.get(i).getObject() == e)
						{
							changes.remove(i);
							break loop;
						}
					}
					return super.remove(e);
				}
				
				public ROWID remove(int index)
				{
					ROWID e = get(index);
					loop:
					for (int i = 0; i < changes.size();i++)
					{
						if (changes.get(i).getObject() == e)
						{
							changes.remove(i);
							break loop;
						}
					}
					return super.remove(index);
				}
			};
	public String updateProcName;
	public String updateTypeName;
	public String[] updateArgs;
	public String[] excluded;
	public String deleteProcName;
	public boolean focusOnAddedRow = true;

	public static int VIEW_MODE = 0;
	public static int ADD_MODE = 1;
	public static int EDIT_MODE = 2;

	protected int mode = EDIT_MODE;

	private String idName = "id";
	//private String deleteIdName = "id";


	protected boolean stopSaveOnError = false;

	protected boolean lastSaveErrorFree;

	int messageCode = -1;
	String message = "";

	public void setIdName(String idName) {
		this.idName = idName;
		//this.deleteIdName = idName;
	}
/*
	public void setDeleteIdName(String idName) {
		this.deleteIdName = idName;
	}
*/
	public String getIdName() {
		return idName;
	}

	public boolean lastSaveErrorFree() {
		return lastSaveErrorFree;
	}

	public void deleteCurrentRow() 
	{
		ViewRow r = getCurrentRow();
		if (r != null)
		{
			delete(r);
		}
	}
	
	public void delete() {
		//deletedKeys.add((ROWID)getCurrentRow().getValue(deleteIdName));
		deletedKeys.add((ROWID)getCurrentRow().getValue(idName));
		modifiedRows.remove(getCurrentRow());
		removeCurrentRow();
	}
	
	public void delete(ViewRow row) 
	{
		if (row.view != this)
		{
			throw new SkylinException(getAM(),"CANNOT DELETE ROW: the row is not contained by the view");
		}
		//deletedKeys.add((ROWID)row.getValue(deleteIdName));
		ROWID id = (ROWID)row.getValue(idName); 
		if (id != null)
		{
			deletedKeys.add(id);
		}
		modifiedRows.remove(row);
		remove(row);
	}

	public int save()
	{
		Connection con = am.getNewConnection();
		try
		{
			int r = save(con);
			try
			{
				con.commit();
			}
			catch (SQLException e)
			{
				getAM().error(e);
			}
			return r;
		}
		finally
		{
			SqlUtil.safeClose(null,null,con);
		}
				
	}
	
	public int save(Connection con) {
		
		lastSaveErrorFree = true;
		for (EditViewRowChange change:changes)
		{
			try
			{
				change.apply(this,con);
			}
			catch (SQLException e) {
				getAM().error(e);
				if (e.getErrorCode() != 20901 && e.getErrorCode() != 20902) {
					lastSaveErrorFree = false;
					if (stopSaveOnError) {
						return -1;
					}
				}
			}
		}

		if (lastSaveErrorFree) return 0;
		return -1;
	}

	public void executeQuery(Connection con) 
	{
		super.executeQuery(con);
		modifiedRows.clear();
		deletedKeys.clear();
	}


	public void viewMode() {
		mode = VIEW_MODE;
	}

	public void addMode() {
		mode = ADD_MODE;
	}

	public void editMode() {
		mode = EDIT_MODE;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public int getMode() {
		return mode;
	}

	public void clearMessage() {
		message = "";
		messageCode = -1;
	}

	protected void setMessage(String message, int messageCode) {
		this.message = message;
		this.messageCode = messageCode;
	}

	public int getMessageCode() {
		return messageCode;
	}

	public String getMessage() {
		return message;
	}

	public boolean isMode(int m)
	{
		if (mode == m)
		{
			return true;
		}
		return false;
	}
	public boolean isMode(int a, int b)
	{
		if (mode == a || mode == b)
		{
			return true;
		}
		return false;
	}
	
	public void setRowType(Class type)
	{
		if (!EditViewRow.class.isAssignableFrom(type))
		{
			new SkylinException(getAM(),"CANNOT SET ROW TYPE: " + getClass().getSimpleName() + " is a sub class of EditView. A sub class of EditView needs a sub class of EditViewRow as its rowtype. "+type.getName() + " is not an subclass of EditViewRow");
		}
		super.setRowType(type);
	}
	
	public void clear() 
	{
		modifiedRows.clear();
		deletedKeys.clear();
		super.clear();
	}
	
	
	public void updateProc(String name, String... args)
	{
		updateProcName = name;
		updateArgs = args;
	}
	
	public void deleteProc(String name)
	{
		deleteProcName = name;
	}
}
