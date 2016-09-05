package skylin;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;



import skylin.util.SqlUtil;

import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;

public class View 
{
	private String query;
	private String registeredQuery;
	protected Class rowType = null;
	//private Class rowAccessType;
	protected ArrayList<ViewRow> rows = new ArrayList<ViewRow>();
	public String columnNames[] = {};
	public String columnSqlNames[];
	public Class columnTypes[] = {};
	//public Class[] lovs;
	//public String[][]extraMappingOverides;
	protected boolean columnEditable[] = null;
	protected int[] keyFields = {0};	
	//CM cm;
	public AM am;
	protected int currentRowIndex = -1;
	
	public static Class number = BigDecimal.class;
	public static Class date = Timestamp.class;
	public static Class string = String.class;
	
	protected ArrayList<String> bindingNames = new ArrayList<String>();
	private ArrayList<Object> bindingValues = new ArrayList<Object>();
	//private ArrayList<Class> bindingTypes = new ArrayList<Class>();
	private String[] setKeyLater;
	protected Class setRowTypeLater;
	private String[] setEditableColumnsLater;
	boolean reverseEditableColumnsLater;
	
	private ArrayList<String> checkBoxes = new ArrayList<String>();
	private ArrayList<String> yes = new ArrayList<String>();
	private ArrayList<String> no = new ArrayList<String>();
	
	boolean autoValidateAllLovs = true;
	private String filterSql;
	private ArrayList filterSqlvalues;
	private boolean initialized;
	
	private String name;
	
	private int currentColumnIndex = -1;
	
	int nextRowLinkId = 1;
	int linkId;
	
	private boolean readOnly;
	
	public ViewRow owner;
	
	public ArrayList<String> defaultLinkCol;
	public ArrayList<String> defaultLinkBindVar;
	//public ArrayList<Class> defaultLinkBindVarType;
	
	public ArrayList<Class> defaultChildLinkClass;
	public ArrayList<String> defaultChildLinkClassInstanceName;
	
	//private static ConcurrentHashMap<Class,ViewGenCache> genCache = new ConcurrentHashMap<Class,ViewGenCache>();
	//private boolean noGenCache = false;
	
	public View(AM am)
	{
		this.am = am;
		if (am.nextName != null)
		{
			name = am.nextName;
			am.nextName = null;
		}
		else
		{
			name = getClass().getSimpleName();
		}
		if (!am.initialized)
		{
			am.views.add(this);
		}
	}
	
	public void executeQuery()
	{
		Connection con = am.getNewConnection();
		executeQuery(con);
		SqlUtil.safeClose(null,null,con);
	}
	
	public void executeQuery(Connection con)
	{
		if (!initialized && am.views.contains(this))
		{
			throw new SkylinException(getAM(),"View " + getClass().getSimpleName() + " is not yet initialized");
		}
		clear();
		if (query != null)
		{
					String registeredQuery = this.registeredQuery;
					if (filterSql != null)
					{
						registeredQuery = "select * from ("+registeredQuery+") where 1 = 1 " + filterSql;
					}
					PreparedStatement stmt = null;
					ResultSet rs = null;
				    try 
				    {
				    	//System.out.println(registeredQuery);
				    	//Connection con = am.getNewConnection();
				        stmt = con.prepareStatement(registeredQuery);
				        am.setCurrentStatement(stmt);
				        for (int i = 0; i < bindingNames.size();i++)
				        {
				        	//Class type = bindingTypes.get(i);
				        	Object value = bindingValues.get(i);
				        	//if (type == null)
				        	{
				        		//throw new SkylinException(getAM(),"Bind varuable " + bindingNames.get(i) + " does not have its type set in view object " + getClass().getSimpleName());
				        		
				        	}
				        	if (value != null)
				        	{
				        		stmt.setObject(i+1, value);
				        	}
				        	else //There must be a way to determine the sqlType from the given java type...
				        	{
				        		/*
				        		if (type == number)
				        		{
				        			stmt.setNull(i+1, Types.NUMERIC);
				        		}
				        		else if (type == string)
				        		{
				        			stmt.setNull(i+1, Types.VARCHAR);
				        		}
				        		else if (type == date)
				        		{
				        			stmt.setNull(i+1, Types.DATE);
				        		}
				        		else
				        		{
				        			throw new SkylinException(getAM(),"Unsuported Bind var type: " + bindingTypes.get(i).getName());//not realy unsuported... just wont work with null values, see above comment
				        		}*/
				        		stmt.setNull(i+1, Types.VARCHAR);
				        	}
				        }
				        if (filterSqlvalues != null)
				        {
					        for (int i = 0; i < filterSqlvalues.size();i++)
					        {
					        	stmt.setObject(i+1+bindingNames.size(), filterSqlvalues.get(i));
					        }
				        }
				        rs = stmt.executeQuery();
				       // ArrayList<Integer> newBlobColumnIndex = new ArrayList<Integer>();
				        while (rs.next()) 
				        {
				        	//newBlobColumnIndex.clear();
				        	Class r = rowType;
				        	if (r == null)
				        	{
				        		r = ViewRow.class;
				        	}
				        	ViewRow row = (ViewRow) r.getDeclaredConstructor(View.class).newInstance(this);
				        	for (int i = 0; i < columnNames.length;i++)
				        	{
				        		if (columnTypes[i] == SkylinBlob.class)
				        		{
				        			ByteArrayOutputStream b = new ByteArrayOutputStream();
				        		    InputStream in = rs.getBinaryStream(i+1);
				        		    SkylinBlob sb = (SkylinBlob)row.getValueInternal(i);
				        		    if (in != null)
				        		    {
					        		    byte[] buffer = new byte[16384];
					        		    int len;
					        		    while ((len = in.read(buffer)) != -1) 
					        		    {
					        		    	b.write(buffer, 0, len);
					        		    }
					        		    sb.data = b.toByteArray();
				        		    }
				        		    //sb.id = am.nextBlobId();
				        		    //sb.am = am;
				        		    sb.session = am.session;
				        		   // newBlobColumnIndex.add(i);
				        		    row.values[i] = sb;
				        		    b.close();
				        		}
				        		else
				        		{
				        			row.values[i] = rs.getObject(i+1);
				        		}
				        	}
				        	/*
				        	for (int i = 0; i < newBlobColumnIndex.size();i++)
				        	{
				        		am.addBlobPointer(this,row.getKey(),newBlobColumnIndex.get(i));
				        	}
				        	*/
				        }
				    }
				    catch (SQLException e ) 
				    {
				        throw new SkylinException(getAM(),"sql error in view " + getClass().getSimpleName() +": "+ e.getMessage());
				    } 
				    catch (Exception e)
				    {
				    	e.printStackTrace();
				    }
				    finally 
				    {
				        	try 
					        {
						        rs.close();    
							} 
					        catch (Exception e) 
					        {
	
							} 
				        	try 
				        	{
								stmt.close();
							} 
				        	catch (Exception e) {

							}
				        
				    }
				
		}
	}
	
	public ViewRow getCurrentRow()
	{
		if (currentRowIndex == -1 || currentRowIndex >= rows.size())
		{
			return null;
		}
		return rows.get(currentRowIndex); 
	}
	
	public Object getCurrentRowKey()
	{
		if (getCurrentRow() != null)
		{
			return getCurrentRow().getKey();
		}
		return null;
	}
	
	public ViewRow getRowAtIndex(int i)
	{
		return rows.get(i); 
	}
	
	public void setKey(String[] keyColumns)
	{
		if (columnNames.length == 0)
		{
			setKeyLater = keyColumns;
			return;
		}
		keyFields = new int[keyColumns.length];
		loop:
		for (int i = 0; i < keyFields.length;i++)
		{
			for (int j = 0; j < columnNames.length;j++)
			{
				if (columnNames[j].equals(keyColumns[i]))
				{
					keyFields[i] = j;
					continue loop;
				}
			}
			throw new SkylinException(getAM(),"CANNOT SET KEY: Field " + keyColumns[i] + " does not exist in view " + getClass().getSimpleName());
		}
	}
	
	public void setLov(String columnName, Class lovClass, String... extraMappingOverides)
	{
		if (defaultChildLinkClass == null)
		{
			defaultChildLinkClass = new ArrayList<Class>();
			defaultChildLinkClassInstanceName = new ArrayList<String>();
		}
		defaultChildLinkClass.add(lovClass);
		defaultChildLinkClassInstanceName.add(columnName+"_lov");
		
	}

	public int getSize() {
		return rows.size();
	}
	
    public int indexOf(Object rowKey) {
        if (rowKey == null)
        {
        	return -1;
        }
        for (int i = 0; i < rows.size();i++)
        {
            if (rowKey.equals(rows.get(i).getKey())) 
            {
                return i;
            }
        }
        return -1;
    }


	public void setCurrentRow(int i) 
	{
		currentRowIndex = i;
	}

	public void init()
	{
		
	}
	
	public void genColumns()
	{
		if (rowType == null && setRowTypeLater == null)
		{
			setRowType(ViewRow.class);
		}
		if (query == null ||columnNames.length > 0 || columnTypes.length > 0)
		{
			return;
		}
		String q = "select * from ("+registeredQuery.replaceAll(Pattern.quote("?"), "null")+") where 1 = 2";
		ResultSet t = null;
		Connection con = null;
		PreparedStatement stmt = null;
		if (registeredQuery != null)
		{
			//ViewGenCache cacheForThisClass = genCache.get(getClass());
			//if (cacheForThisClass == null || noGenCache)
			{
				try
				{
		
						con = am.getNewConnection();
						stmt = con.prepareStatement(q);
						am.setCurrentStatement(stmt);
						t = stmt.executeQuery();
						int count = t.getMetaData().getColumnCount();
						columnNames = new String[count];
						columnTypes = new Class[count];
						for (int i = 0; i < count; i++)
						{
						
							columnTypes[i] = Class.forName(t.getMetaData().getColumnClassName(i+1));
							if (Blob.class.isAssignableFrom(columnTypes[i]))
							{
								columnTypes[i] = SkylinBlob.class;
							}
							String name = t.getMetaData().getColumnName(i+1).toLowerCase();
							name = camelType(name);	
							columnNames[i] = name;
							
							if (!validAttributeName(name))
							{
								throw new SkylinException(getAM(),"Cannot generate a attribute for column " + name + " in view object " + getClass().getSimpleName() + ". Use an alias to simplify the column name or take out illiegal characters");
							}
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
						throw new SkylinException(getAM(),"Error generating columns in View " + getClass().getSimpleName());	
					}
					finally
					{
						SqlUtil.safeClose(t,stmt,con);
					}
					ViewGenCache c = new ViewGenCache();
					c.columnNames = columnNames;
					c.columnTypes = columnTypes;
					//genCache.put(getClass(), c);
				}
			/*
				else
				{
					columnNames = cacheForThisClass.columnNames;
					columnTypes = cacheForThisClass.columnTypes;
				}*/
			}
			if (setKeyLater != null)
			{
				setKey(setKeyLater);
				setKeyLater = null;
			}
			if (setEditableColumnsLater != null)
			{
				if (reverseEditableColumnsLater)
				{
					setNotEditableColumns(setEditableColumnsLater);
				}
				else
				{
					setEditableColumns(setEditableColumnsLater);
				}
				setEditableColumnsLater = null;
			}
			if (rowType == null)
			{
				setRowType(setRowTypeLater);
			}
			setRowTypeLater = null;

		


		
		
	}

	private boolean validAttributeName(String name) 
	{
		name = name.toLowerCase();
		String legal = "abcdefghijklmnopqrstuvwxyz1234567890_";
		for (int i = 0; i < name.length();i++)
		{
			if (!legal.contains(name.charAt(i)+""))
			{
				return false;
			}
		}
		return true;
	}

	private String camelType(String name) 
	{
		name = name.toLowerCase();
		for (int c = 0; c < name.length();c++)
		{
			if (name.charAt(c) == '_')
			{
				name = name.replaceFirst("_", "");
				name = name.substring(0,c) + (name.charAt(c)+"").toUpperCase() +name.substring(c+1,name.length());
			}
		}
		return name;
	}

	public void clear() 
	{
		rows.clear();
		if (currentRowIndex != -1)
		{
			setCurrentRow(-1);
		}
	}
	
	public void setQuery(String q)
	{
		bindingNames.clear();
		bindingValues.clear();
		//bindingTypes.clear();
		query = q;
		boolean inText = false;
		for (int c = 0; c < q.length();c++)
		{
			if (q.charAt(c) == '\'')
			{
				inText = !inText;
				continue;
			}
			if (inText)
			{
				continue;
			}
			if (q.charAt(c) == ':')
			{
				int i = c+1;
				loop:
				for (; i < q.length();i++)
				{
					if (" ),|\n\r\t".contains(q.charAt(i)+""))
					{
						break loop;
					}				
				}
				String binding = q.substring(c+1, i);
				
				bindingNames.add(camelType(binding));
				bindingValues.add(null);
				//bindingTypes.add(null);
				
				q = q.replaceFirst(":"+binding, "?");
			}
		}
		registeredQuery = q;
	}
	
	public String getQuery()
	{
		return query;
	}
	
	public void setBindVar(String name,Class type, Object value)
	{
		boolean found = false;
		for (int i = 0; i < bindingNames.size();i++)
		{
			if (bindingNames.get(i).equals(name))
			{
				//bindingTypes.set(i, type);
				bindingValues.set(i, value);
				found = true;
			}
		}
		if (!found)
		{
			throw new SkylinException(getAM(),"No bind var named " + name + " exists in view object " + getClass().getSimpleName());
		}
	}
	
	public void setBindVarArray(String name, String type, Object value, Connection con)
	{
		/*
        ARRAY insertParameter = null;
        try 
        {
            insertParameter = new ARRAY(ArrayDescriptor.createDescriptor("UTIL.PLSTABN", con), con, value);
        } 
        catch (SQLException e)
        {
            getAM().error(e.getMessage());
        }
        setBindVar(name,ARRAY.class,insertParameter);*/
		
        ARRAY insertParameter = null;
        try 
        {
        	//Connection con2 = ((org.apache.tomcat.dbcp.dbcp.DelegatingConnection)con).getInnermostDelegate();
        	Method m = con.getClass().getMethod("getInnermostDelegate", new Class[]{});
        	m.setAccessible(true);
        	Connection con2 = (Connection) m.invoke(con, new Object[]{});
            insertParameter = new ARRAY(ArrayDescriptor.createDescriptor("UTIL.PLSTABN", con2), con2, value);
        } 
        catch (Exception e)
        {
            am.error(e.getMessage());
        }
        setBindVar(name,ARRAY.class,insertParameter);
	}
	
	public void setBindVar(String name, Class type) 
	{
		boolean found = false;
		for (int i = 0; i < bindingNames.size();i++)
		{
			if (bindingNames.get(i).equals(name))
			{
				//bindingTypes.set(i, type);
				bindingValues.set(i, null);
				found = true;
			}
		}
		if (!found)
		{
			throw new SkylinException(getAM(),"No bind var named " + name + " exists in view object " + getClass().getSimpleName());
		}
	}
	
	
	public void setBindVar(String name, Object value)
	{
		boolean found = false;
		for (int i = 0; i < bindingNames.size();i++)
		{
			if (bindingNames.get(i).equals(name))
			{
				bindingValues.set(i, value);
				found = true;
			}
		}
		if (!found)
		{
			throw new SkylinException(getAM(),"No bind var named " + name + " exists in view object " + getClass().getSimpleName());
		}
	}
	
	
	public Object getBindVar(String name)
	{
		for (int i = 0; i < bindingNames.size();i++)
		{
			if (bindingNames.get(i).equals(name))
			{
				return bindingValues.get(i);
			}
		}
		throw new SkylinException(getAM(),"No bind var named " + name + " exists in view object " + getClass().getSimpleName());
	}
	
	/*
	public Class getBindVarType(String name)
	{
		for (int i = 0; i < bindingNames.size();i++)
		{
			if (bindingNames.get(i).equals(name))
			{
				return bindingTypes.get(i);
			}
		}
		throw new SkylinException(getAM(),"No bind var named " + name + " exists in view object " + getClass().getSimpleName());
	}*/
	
	public boolean hasBindVar(String name)
	{
		for (int i = 0; i < bindingNames.size();i++)
		{
			if (bindingNames.get(i).equals(name))
			{
				return true;
			}
		}
		return false;
	}
	
	public void setBindVarsFromViewRow(ViewRow row)
	{
		for (int i = 0; i < bindingNames.size();i++)
		{
			loop:
			for (int j = 0; j < row.view.columnNames.length;j++)
			{
				//System.out.println("comparing " + bindingNames.get(i) + " to " + row.view.columnNames[j]);
				if (bindingNames.get(i).equals(row.view.columnNames[j]))
				{
					//bindingTypes.set(i, row.view.columnTypes[j]);
					bindingValues.set(i, row.values[j]);
					//System.out.println("setting " + bindingNames.get(i) + " from view row");
					break loop;
				}
			}
		}
	}
	
	public void setBindVarsFromView(View view)
	{
		for (int i = 0; i < bindingNames.size();i++)
		{
			loop:
			for (int j = 0; j < view.bindingNames.size();j++)
			{
				if (bindingNames.get(i).equals(view.bindingNames.get(j)))
				{
					bindingValues.set(i, view.bindingValues.get(j));
					break loop;
				}
			}
		}
	}
	
	public void setRowType(Class type)
	{
		if (!ViewRow.class.isAssignableFrom(type))
		{
			new SkylinException(getAM(),"CANNOT SET ROW TYPE: " + getClass().getSimpleName() + " is a sub class of View. A sub class of View needs a sub class of ViewRow as its rowtype. "+type.getName() + " is not an subclass of ViewRow");
		}
		if (columnNames.length == 0)
		{
			setRowTypeLater = type;
			return;
		}
		
		rowType = type;
		
		
		/*
		if (checkBoxes.size() > 0)
		{
			throw new SkylinException(getAM(),"CANNOT SET CHECKBOX: view " + getClass().getSimpleName() + " does not contain a field named " + checkBoxes.get(0));
		}
		*/
	}

	
	public void setEditableColumns(String... c)
	{
		if (columnNames.length == 0)
		{
			setEditableColumnsLater = c;
			return;
		}
		ArrayList<String> cc = new ArrayList<String>();
		for (String s:c)
		{
			cc.add(s);
		}
		columnEditable = new boolean[columnNames.length];
		for(int i = 0; i < columnNames.length;i++)
		{
			loop:
			for (int j = 0; j < cc.size();j++)
			{
				if (columnNames[i].equals(cc.get(j)))
				{
					columnEditable[i] = true;
					cc.remove(j);
					break loop;
				}
			}
		}
		if (cc.size() > 0)
		{

			new SkylinException(getAM(),"CANNOT SET EDITABLE COLUMN: " + " no field named " + cc.get(0) + " exists in view " + getClass().getSimpleName());
		}
	}
	
	public void setNotEditableColumns(String... c)
	{
		if (columnNames.length == 0)
		{
			reverseEditableColumnsLater = true;
			setEditableColumnsLater = c;
			return;
		}
		ArrayList<String> cc = new ArrayList<String>();
		for (String s:c)
		{
			cc.add(s);
		}
		columnEditable = new boolean[columnNames.length];
		for(int i = 0; i < columnNames.length;i++)
		{
			columnEditable[i] = true;
			loop:
			for (int j = 0; j < cc.size();j++)
			{
				if (columnNames[i].equals(cc.get(j)))
				{
					columnEditable[i] = false;
					cc.remove(j);
					break loop;
				}
			}
		}
		if (cc.size() > 0)
		{

			new SkylinException(getAM(),"CANNOT SET NOT EDITABLE COLUMN: " + " no field named " + cc.get(0) + " exists in view " + getClass().getSimpleName());
		}
	}
	
	public Class getRowType()
	{
		if (rowType != null)
		{
			return rowType;
		}
		return ViewRow.class;
	}
	
	/*
	public Class getRowAccessType()
	{
		return rowAccessType;
	}
	*/

	
	public int getColumnIndex(String name)
	{
		for (int i = 0; i < columnNames.length;i++)
		{
			if (columnNames[i].equals(name))
			{
				return i;
			}
		}
		return -1;
	}

	public List<ViewRow> getRows()
	{
		return rows;
	}
	
	public List<ViewRow> getRowsBetween(int start, int end)
	{
		ArrayList<ViewRow> ret = new ArrayList<ViewRow>();
		if (start > end || start > rows.size() - 1)
		{
			return ret;
		}
		if (end >= rows.size())
		{
			end = rows.size();
		}
		for (int i = start; i < end;i++)
		{
			ret.add(rows.get(i));
		}
		return ret;
	}

	public void removeCurrentRow()
	{
		if (currentRowIndex != -1)
		{
			rows.remove(currentRowIndex);
			currentRowIndex = -1;
		}
	}
	
	public void remove(ViewRow row)
	{
		if (getCurrentRow() == row)
		{
			currentRowIndex = -1;
		}
		boolean shift = currentRowIndex > rows.indexOf(row);
		rows.remove(row);
		if (shift && currentRowIndex != -1)
		{
			currentRowIndex--;
		}
	}

	public boolean hasColumn(String name) 
	{
		for (int i = 0; i < columnNames.length;i++)
		{
			if (name.equals(columnNames[i]))
			{
				return true;
			}
		}
		return false;
	}


	public final void initInternal() {
		genColumns();
		//createValidators();
		createSqlColumnNames();
		initialized = true;
	}

	private void createSqlColumnNames() 
	{
		columnSqlNames = new String[columnNames.length];
		for (int i = 0; i < columnNames.length;i++)
		{
			String name = columnNames[i];
			String sqlName = "";
			for (int j = 0; j < name.length();j++)
			{
				if (Character.isUpperCase(name.charAt(j)))
				{
					sqlName += "_";
				}
				sqlName += name.charAt(j);
			}
			columnSqlNames[i] = sqlName.toUpperCase();
		}
	}

	public void setCheckBox(String column, String yes,String no)
	{
		checkBoxes.add(column);
		this.yes.add(yes);
		this.no.add(no);

	}
	public void setCheckbox(String column, String yes,String no)
	{
		setCheckBox(column,yes,no);
	}
	
	public View getViewObject(String name)
	{
		return am.getViewObject(name);
	}

	public void setFilterSql(String sqlString, ArrayList values) 
	{
		filterSql = sqlString;
		filterSqlvalues = values;
		executeQuery();
	}

	public String getSqlName(String name) 
	{
		for (int i = 0;i < columnSqlNames.length; i++)
		{
			if(columnNames[i].equals(name))
			{
				return columnSqlNames[i];
			}
		}
		return null;
	}
	
	public ViewRow newRow()
	{
		try
		{
			ViewRow r = (ViewRow) getRowType().getDeclaredConstructor(View.class).newInstance(this);
			r.init();
			return r;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public ViewRow newRow(int index)
	{
		try
		{
			ViewRow r = newRow();
			rows.remove(rows.size()-1);
			rows.add(index, r);
			if (index <= currentRowIndex)
			{
				currentRowIndex++;
			}
			return r;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public void pivot()
	{
		while (rows.size() < columnNames.length)
		{
			try
			{
				getRowType().getDeclaredConstructor(View.class).newInstance(this);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		ArrayList<Object[]> values = new ArrayList<Object[]>();
		for (ViewRow r:rows)
		{
			Object[] v = new Object[r.values.length];
			System.arraycopy(r.values, 0, v, 0, v.length);
			values.add(v);
		}

		

		for (int i = 0; i < columnNames.length;i++)
		{
			for (int j = 0; j < Math.min(columnNames.length, rows.size());j++)
			{
				rows.get(j).setValueInternal(i, values.get(i)[j]);
			}
		}

	}
	
	public ViewRow getRow(int index)
	{
		if (index > -1 && index < rows.size())
		{
			return rows.get(index);
		}
		return null;
	}
	
	@Deprecated
	/*
	 * use getFrist() instead
	 */
	public ViewRow first()
	{
		return getRow(0);
	}
	
	public ViewRow getFirst()
	{
		return getRow(0);
	}
	
	public void setCurrentRow(ViewRow row)
	{
		setCurrentRow(row.getIndex());
	}
	
	public void setCurrentRowByKey(Object rowKey)
	{
        for (int i = 0; i < rows.size();i++)
        {
            if (rows.get(i).getKey().equals(rowKey)) 
            {
        		setCurrentRow(i);
                return;
            }
        }
        setCurrentRow(-1);
	}
	
	public ViewRow getRow(Object rowKey)
	{
        for (int i = 0; i < rows.size();i++)
        {
            if (rowKey.equals(rows.get(i).getKey())) 
            {
                return rows.get(i);
            }
        }
        return null;
	}
	
	public ArrayList<ViewRow> getRowsWhere(Object a, Object b)//for calling from php
	{
		return getRowsWhere(new Object[]{a, b});
	}
	
	public ArrayList<ViewRow> getRowsWhere(Object a, Object b,Object c, Object d)//for calling from php
	{
		return getRowsWhere(new Object[]{a, b,c,d});
	}
	
	public ArrayList<ViewRow> getRowsWhere(Object a, Object b,Object c, Object d,Object e, Object f)//for calling from php
	{
		return getRowsWhere(new Object[]{a, b,c,d,e,f});
	}
	
	public ArrayList<ViewRow> getRowsWhere(Object... v)
	{
		ArrayList<ViewRow> ret = new ArrayList<ViewRow>();
		
		loop:
		for (ViewRow r:rows)
		{
			for (int i = 0; i < v.length/2;i+=2)
			{
				if (!v[i+1].equals(r.getValue((String)v[i])))
				{
					continue loop;
				}
			}
			ret.add(r);
		}
		return ret;
	}
	
	public AM getAM()
	{
		return am;
	}
	
	public String getName()
	{
		return name;
	}

/*
	public void setCurrentColumn(String c) 
	{
		if(c != null)
		{
			currentColumnIndex = getColumnIndex(c);
		}
		else
		{
			currentColumnIndex = -1;
		}
	}*/
	
	
	public void setCurrentColumn(int i) 
	{

		currentColumnIndex = i;
	}
	
	public void filldown()
	{
		System.out.println("t1: " + currentRowIndex + ", " + currentColumnIndex);
		if (currentRowIndex == -1 || currentColumnIndex == -1)
		{
			return;
		}
		Object value = rows.get(currentRowIndex).getValue(currentColumnIndex);
		if (value == null)
		{
			return;
		}
		block:
		for (int i = currentRowIndex + 1; i < rows.size();i++)
		{
			ViewRow r = rows.get(i);
			if (r.getValue(currentColumnIndex) != null || !r.isUpdateable(currentColumnIndex))
			{
				continue block;
			}	
			try
			{
				r.validate(i, value);
				r.setValue(currentColumnIndex, value);
			}
			catch (Exception e)
			{
				
			}
			
		}
	}

	
	public ViewRow getLocalRowByLinkId(long id)
	{
		//replace with binary tree search maybe
		for (int i = 0; i < rows.size();i++)
		{
			if (rows.get(i).getLocalLinkId() == id)
			{
				return rows.get(i);
			}
		}	
		return null;
	}
/*
	public ViewRow getRowByLinkId(String fullId)
	{
		long id = Integer.parseInt(fullId);
		//replace with binary tree search
		for (int i = 0; i < rows.size();i++)
		{
			if (rows.get(i).getLocalLinkId() == id)
			{
				return rows.get(i);
			}
		}
		return null;
	}
	*/
	
	public ViewRow getRowByLinkId(String fullId)
	{
		LinkedList<Integer> chain = new LinkedList<Integer>();
		String[] s = fullId.split("x");
		for (String id:s)
		{
			chain.add(Integer.parseInt(id));
		}
		return getRootView().getRowByLinkChain(chain);
	}
	
	public ViewRow getRowByLinkChain(LinkedList<Integer> chain)
	{
		ViewRow row = getLocalRowByLinkId(chain.poll());
		if (chain.isEmpty())
		{
			return row;
		}
		return row.getLocalViewByLinkId(chain.poll()).getRowByLinkChain(chain);
	}
	
	public View getRootView()
	{
		if (owner == null)
		{
			return this;
		}
		return owner.getView().getRootView();
	}
	
	public boolean isMode(int m)
	{
		if (0 == m)
		{
			return true;
		}
		return false;
	}
	public boolean isMode(int a, int b)
	{
		if (0 == a || 0 == b)
		{
			return true;
		}
		return false;
	}
	
	public String debug_printAllBindVars()
	{
		String ret = "";
		for (int i = 0; i < bindingNames.size();i++)
		{
			ret += bindingNames.get(i) + "   " + bindingValues.get(i);
		}
		return ret;
	}
	
	public void setReadOnly(boolean r)
	{
		readOnly = r;
	}
	
	public boolean getReadOnly()
	{
		return readOnly;
	}
	
	public View getLinkedView(Class viewClass,String... args)
	{
		ViewRow cur = getCurrentRow();
		if (cur == null)
		{
			return null;
		}
		return cur.getLinkedView(viewClass, args);
	}
	
	public void linkChain(ArrayList<Integer> chain)
	{
		if (owner != null)
		{
			owner.linkChain(chain);
			chain.add(linkId);
		}
	}
	
	public int getLocalLinkId()
	{
		return linkId;
	}
	
	public void addDefaultLink(String col, String var)//,Class type remove comments when sure that type is no longer needed.
	{
		if (defaultLinkCol == null)
		{
			defaultLinkCol = new ArrayList<String>();
			defaultLinkBindVar = new ArrayList<String>();
			//defaultLinkBindVarType = new ArrayList<Class>();
		}
		defaultLinkCol.add(col);
		defaultLinkBindVar.add(var);
		//defaultLinkBindVarType.add(type);
		
	}
	
	/*
	public void noGenCache()
	{
		noGenCache = true;
	}*/
	
	public void setColumnTypes(Class... classes)
	{
		columnTypes = classes;
		if (columnNames.length > 0 && columnTypes.length != columnNames.length)
		{
			//throw new SkylinException(getAM(),"column names and columnTypes have different lengths in view object " + getClass().getName());
			System.out.println("WARNING: " + "column names and columnTypes have different lengths in view object " + getClass().getName());
		}
	}
	
	public void setColumnNames(String... names)
	{
		columnNames = names;
		if (columnTypes.length > 0 && columnTypes.length != columnNames.length)
		{
			//throw new SkylinException(getAM(),"column names and columnTypes have different lengths in view object " + getClass().getName());
			System.out.println("WARNING: " + "column names and columnTypes have different lengths in view object " + getClass().getName());
		}
		if (rowType == null && setRowTypeLater != null)
		{
			setRowType(setRowTypeLater);
			setRowTypeLater = null;
		}
	}
	
	public ViewRow getOwner()
	{
		return owner;
	}
	
	public void clearBindVarValues()
	{
		for (int i = 0; i < bindingValues.size(); i++)
		{
			bindingValues.set(i, null);
		}
	}
	
}
