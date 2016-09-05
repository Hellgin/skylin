package skylin;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import skylin.lov.Lov;
import skylin.util.Util;


public class ViewRow 
{
	public View view;
	Object[] values;
	//Lov[] lovs;
	private Object access;
	//public UIComponent currentValidationComponant;
    Object validationOveride;
    Object validationOverideRequest;
	
	Object[] lastUpdateCheckRequest;
	int[] updateCheckCount;
	boolean[] updateCheckPrevious;
	
	int linkId;
	String fullLinkId;
	int nextViewLinkId = 1;
	
	public ArrayList<ViewLink> viewLinks;
    
	public ViewRow(View view)
	{
		if (getClass() != view.getRowType())
		{
			throw new SkylinException(getAM(),"Cannot add row of type " + getClass().getSimpleName() + " to view object " + view.getClass().getSimpleName() + " because it uses rows of type " + view.getRowType().getSimpleName());
		}
		this.view = view;
		values = new Object[view.columnNames.length];
		//lovs = new Lov[view.columnNames.length];
		view.rows.add(this);
		
		/*
		if (view.lovs != null)
		{	
			for (int i = 0; i < lovs.length;i++)
			{
				if (view.lovs[i] != null)
				{
					try
					{
						lovs[i] = (Lov) view.lovs[i].getConstructor(ViewRow.class).newInstance(this);
						lovs[i].ownerColumns[0] = getName(i);
						//System.out.println("start!");
						for (int j = 0; j < view.extraMappingOverides[i].length;j++)
						{
							//System.out.println(view.extraMappingOverides[i][j] + "!!!");
							lovs[i].ownerColumns[j+1] = view.extraMappingOverides[i][j]; 
						}
						lovs[i].initInternal();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		*/
		
		lastUpdateCheckRequest = new Object[view.columnNames.length] ;
		updateCheckCount = new int[view.columnNames.length];
		updateCheckPrevious = new boolean[view.columnNames.length];
		
		for (int i = 0; i < view.columnTypes.length;i++)
		{
			if (view.columnTypes[i] == SkylinBlob.class)
			{
				setValueInternal(i,new SkylinBlob());
			}
		}
		
		linkId = view.nextRowLinkId;
		view.nextRowLinkId++;
	}
	
	public void setValues(Object[] data)
	{
		for (int i = 0; i < values.length;i++)
		{
			setValue(i,data[i]);
		}
	}
	
	public void setValues(Vector data)
	{
		this.values = new Object[data.size()];
		for (int i = 0; i < values.length;i++)
		{
			setValue(i,data.get(i));
		}
	}
	
	public final void setValuesInternal(Object[] data)
	{
		for (int i = 0; i < values.length;i++)
		{
			setValueInternal(i,data[i]);
		}
	}
	
	public final void setValuesInternal(Vector data)
	{
		this.values = new Object[data.size()];
		for (int i = 0; i < values.length;i++)
		{
			setValueInternal(i,data.get(i));
		}
	}
	
	public void setValue(String name, Object value)
	{
		for (int i = 0; i < view.columnNames.length;i++)
		{
			if (name.equals(view.columnNames[i]))
			{
				setValue(i,value);
				return;
			}
		}
		throw new SkylinException(getAM(),"CANNOT SET COLUMN VALUE: no column named " + name + " exists in view object " + view.getClass().getSimpleName());
	}
	
	public Object getValue(String name)
	{
		for (int i = 0; i < view.columnNames.length;i++)
		{
			if (name.equals(view.columnNames[i]))
			{
				return getValue(i);
			}
		}
		throw new SkylinException(getAM(),"CANNOT GET COLUMN VALUE: no column named " + name + " exists in view object " + view.getClass().getSimpleName());
	}
	
	public void setValueInternal(String name, Object value)
	{
		for (int i = 0; i < view.columnNames.length;i++)
		{
			if (name.equals(view.columnNames[i]))
			{
				setValueInternal(i,value);
				return;
			}
		}
		throw new SkylinException(getAM(),"CANNOT SET COLUMN VALUE: no column named " + name + " exists in view object " + view.getClass().getSimpleName());
	}
	
	public Object getValueInternal(String name)
	{
		for (int i = 0; i < view.columnNames.length;i++)
		{
			if (name.equals(view.columnNames[i]))
			{
				return getValueInternal(i);
			}
		}
		throw new SkylinException(getAM(),"CANNOT GET COLUMN VALUE: no column named " + name + " exists in view object " + view.getClass().getSimpleName());
	}
	
	public void setValue(int i, Object value)
	{
		setValueInternal(i,value);
		/*
		if (lovs[i] != null)
		{
			lovs[i].valueSet(value);
		}
		*/
	}
	
	public Object getValue(int i)
	{
		return getValueInternal(i);
	}
	
	public void setValueInternal(int i, Object value)
	{
		if (value != null && value.getClass() != view.columnTypes[i])
		{
			throw new SkylinException(getAM(),"CANNOT SET COLUMN VALUE: Column " + view.columnNames[i] + " is of type " + view.columnTypes[i].getName() + " but a value of type " + value.getClass().getName() + " was provided.");
		}
		values[i] = value;
	}
	
	public final Object getValueInternal(int i)
	{
		return values[i];
	}
	
	public String getName(int i)
	{
		return view.columnNames[i];
	}
	
	public Object[] getValues()
	{
		return values;
	}
	
	public Object getKey()
	{
		ArrayList<Object> key = new ArrayList<Object>();
		for (int i = 0; i < view.keyFields.length;i++)
		{
			key.add(values[view.keyFields[i]]);
		}
		return key;
	}
	
	public Object getAccess()
	{
		/*
		if (access == null)
		{
			try 
			{
				access = view.getRowAccessType().getDeclaredConstructor(Object.class).newInstance(this);
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		return access;
		*/
		return null;
	}

	/*
	public void setLov(Lov lov) 
	{
		for (int i = 0; i < view.columnNames.length;i++)
		{
			if (lov.ownerColumns[0].equals(view.columnNames[i]))
			{
				lovs[i] = lov;
				return;
			}
		}
		throw new SkylinException(getAM(),"CANNOT SET COLUMN LOV: no column named " + lov.ownerColumns[0] + " exists in view object " + view.getClass().getSimpleName());
	}
	*/
	
	/*
	public ListOfValuesModel getLovModel(int i)
	{
		return lovs[i].getListOfValuesModel();
	}
	*/
	
	/*
	public Lov getLov(int index)
	{
		return lovs[index];
	}
	
	public Lov getLov(String column)
	{
		return lovs[view.getColumnIndex(column)];
	}
	*/
	
	public boolean isUpdateable(int i)
	{
		if (view.columnEditable != null && !view.columnEditable[i])
		{
			return false;
		}
		return !view.getReadOnly();
	}
	
	/*
	public boolean isUpdateableFromFrontEnd(int i)
	{
		Object request = Util.getCurrentRequest();
		if (request != lastUpdateCheckRequest[i])
		{
			updateCheckCount[i] = -1;
			lastUpdateCheckRequest[i] = request;
		}
		else
		{
			updateCheckCount[i] ++;
		}
		return isUpdateable(i);
	}
	*/
	
	public boolean isUpdateable(String s)
	{
		return isUpdateable(view.getColumnIndex(s));
	}
	
	public void validate(int i, Object value) throws SQLException, ValidationException
	{
		/*
		block:
		if (view.autoValidateAllLovs)
		{
			if (value == null || value.equals(""))
			{
				break block;
			}
			Lov lov = lovs[i];
			if (lov != null)
			{
				lov.executeQuery();
				int index = lov.getColumnIndex(lov.lovColumns[0]);
				for (ViewRow row:lov.getRows())
				{
					Object v = row.getValue(index);
					if (v == null)
					{
						continue;
					}
					if (value.equals(v) || (view.columnTypes[i] == View.string && v.toString().toLowerCase().equals(value.toString().toLowerCase())))
					{
						break block;
					}
				}
				validationError("Invalid value","Value not found in list");
			}
		}
		*/
	}
	


	public boolean hasColumn(String name) 
	{
		return view.hasColumn(name);
	}
	
	public void validationError(String body) throws ValidationException
	{
		validationError(null,body);
	}
	
	public void validationError(String header, String body) throws ValidationException
	{
		throw new ValidationException(body);
	}
	

	
	public int getIndex()
	{
		return view.rows.indexOf(this);
	}
	
	public View getView()
	{
		return view;
	}
	
	public View getViewObject(String name)
	{
		return view.getViewObject(name);
	}

	public void init() 
	{

	}
	
	public AM getAM()
	{
		return view.getAM();
	}
	
	public String setValueFromFrontEnd(String name, Object value)
	{
		try
		{
			int i = view.getColumnIndex(name);
	        view.setCurrentColumn(i);
        	if  (value != null && (value+"").length() == 0)
        	{
        		value = null;
        	}
	        if (view.columnTypes[i].equals(BigDecimal.class))
	        {
	        	if  (value != null)
	        	{
		        	try
		        	{
		        		value = new BigDecimal((String) value);
		        	}
		        	catch (Exception e)
		        	{
		        		return "Invalid number";
		        	}
	        	}
	        }
	        try
	        {
	        	validate(i,value);
	        }
	        catch (ValidationException e)
	        {
	        	return e.message;
	        }
			setValue(i,value);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public String setFileFromFrontEnd(String col, String filePath, String colName, String name)
	{
    	if  (name != null && (name+"").length() == 0)
    	{
    		name = null;
    	}
		int iname = view.getColumnIndex(colName);
		
    	if  (filePath != null && (filePath+"").length() == 0)
    	{
    		filePath = null;
    	}
    	SkylinBlob b = null;
		int ifilePath = view.getColumnIndex(col);
		
		try
		{
	        try
	        {
	        	validate(iname,name);
	        }
	        catch (ValidationException e)
	        {
	        	return e.message;
	        }

	        view.setCurrentColumn(ifilePath);
	        if (filePath != null)
	        {
	        	b = new SkylinBlob(getAM().session,filePath);
	        }
	
	        try
	        {
	        	validate(ifilePath,b);
	        }
	        catch (ValidationException e)
	        {
	        	return e.message;
	        }
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		setValue(iname,name);
		setValue(ifilePath,b);
		
		return null;
	}
	
	public void setValueFromValidation(Object value)
	{
		validationOveride = value;
		validationOverideRequest = Util.getCurrentRequest();
	}
	
	public void setValuesFromViewRow(ViewRow other, boolean internal)
	{
		for (int i = 0; i < view.columnNames.length;i++)
		{
			if (other.hasColumn(view.columnNames[i]))
			{
				if (internal)
				{
					setValueInternal(i,other.getValueInternal(view.columnNames[i]));
				}
				else
				{
					setValue(view.columnNames[i],other.getValue(view.columnNames[i]));
				}
			}
		}
	}
	
	public String getLinkId()
	{
		if (fullLinkId == null)
		{
			ArrayList<Integer> chain = new ArrayList<Integer>();
			linkChain(chain);
			
			fullLinkId = "";
			for (Integer i:chain)
			{
				fullLinkId+="x"+i;
			}
			fullLinkId = fullLinkId.substring(1);
		}
		/*
		if (fullLinkId.contains("x"))
		{
			System.out.println(linkId + "    " + fullLinkId);
		}*/
		
		//return linkId+"";
		return fullLinkId;
	}
	
	public long getLocalLinkId()
	{
		return linkId;
	}
	
	public void linkChain(ArrayList<Integer> chain)
	{
		view.linkChain(chain);
		chain.add(linkId);
	}
	
	public String getHtmlInputType(String col)
	{
		if (col != null)
		{
			Class c = view.columnTypes[view.getColumnIndex(col)];
			if (c.equals(View.number))
			{
				return "number";
			}
		}
		return "text";
	}
	
	public View getLinkedView(Class viewClass,String... args)
	{
		String customInstanceName = null;
		if (args.length%2 != 0)
		{
			customInstanceName = args[0];
			String[] o = args;
			args = new String[o.length-1];
			for (int i = 0; i < args.length;i++)
			{
				args[i] = o[i+1];
			}
		}
		String col[] = new String[args.length/2];
		String bindVar[] = new String[args.length/2];
		for (int i = 0; i < bindVar.length;i++)
		{
			col[i] = args[i*2];
			bindVar[i] = args[i*2+1];
		}
		if (viewLinks == null)
		{
			viewLinks = new ArrayList<ViewLink>();
		}
		else
		{
			String name = customInstanceName;
			if (name == null)
			{
				name = viewClass.getSimpleName();
			}
			for (ViewLink vl:viewLinks)
			{
				if (vl.linkedView.getName().equals(name))
				{
					boolean changed = false;
					for (int i = 0; i < bindVar.length;i++)
					{
						if (vl.linkValues[i] != getValue(col[i]))
						{
							Object value = getValue(col[i]);
							vl.linkValues[i] = value;
							vl.linkedView.setBindVar(bindVar[i], view.columnTypes[view.getColumnIndex(col[i])], value);
							changed = true;
						}
					}
					if (changed)
					{
						vl.linkedView.executeQuery();
					}
					return vl.linkedView;
				}
			}
		}
		try 
		{
			getAM().nextName = customInstanceName;
			View v = (View) viewClass.getDeclaredConstructor(AM.class).newInstance(getAM());
			v.owner = this;
			ViewLink vl = new ViewLink();
			vl.linkValues = new Object[bindVar.length];
			v.initInternal();
			for (int i = 0; i < bindVar.length;i++)
			{
				Object value = getValue(col[i]);
				vl.linkValues[i] = value;
				v.setBindVar(bindVar[i], view.columnTypes[view.getColumnIndex(col[i])], value);
			}
			v.executeQuery();
			v.linkId = nextViewLinkId;
			vl.linkedView = v;
			viewLinks.add(vl);
			nextViewLinkId++;
			return v;
		} 
		catch (Exception e) 
		{
			new SkylinException(getAM(),e+"");	
		}
		return null;
	}
	public View getLinkedView(String instanceName)
	{
		try
		{
			if (viewLinks == null)
			{
				viewLinks = new ArrayList<ViewLink>();
			}
			for (ViewLink vl:viewLinks)
			{
				if (vl.linkedView.getName().equals(instanceName))
				{
					
					boolean changed = false;
					for (int i = 0; i < vl.linkValues.length;i++)
					{
						if (vl.linkValues[i] != getValue(vl.linkCols[i]))
						{
							Object value = getValue(vl.linkCols[i]);
							vl.linkValues[i] = value;
							vl.linkedView.setBindVar(vl.linkVars[i], view.columnTypes[view.getColumnIndex(vl.linkCols[i])], value);
							changed = true;
						}
					}
					if (changed)
					{
						vl.linkedView.executeQuery();
					}
					return vl.linkedView;
				}
			}
			for (int i = 0; i < view.defaultChildLinkClass.size();i++)
			{
				if (view.defaultChildLinkClassInstanceName.get(i).equals(instanceName))
				{
					getAM().nextName = instanceName;
					View v = (View) view.defaultChildLinkClass.get(i).getDeclaredConstructor(AM.class).newInstance(getAM());
					v.owner = this;
					ViewLink vl = new ViewLink();
					v.initInternal();
					if (v.defaultLinkBindVar != null)
					{
						ArrayList<Integer> include = new ArrayList<Integer>();
						for (int j = 0; j < v.defaultLinkBindVar.size();j++)
						{
							if (view.hasColumn(v.defaultLinkCol.get(j)))
							{
								include.add(j);
							}
						}
						vl.linkValues = new Object[include.size()];
						vl.linkCols = new String[include.size()];
						vl.linkVars = new String[include.size()];
						int k = 0;
						for (int j:include)
						{
							Object value = getValue(v.defaultLinkCol.get(j));
							vl.linkCols[k] = v.defaultLinkCol.get(j);
							vl.linkVars[k] = v.defaultLinkBindVar.get(j);
							vl.linkValues[k] = value;
							v.setBindVar(v.defaultLinkBindVar.get(j), value);
							k++;
						}
					}
					else
					{
						vl.linkValues = new Object[0];
					}
					v.executeQuery();
					v.linkId = nextViewLinkId;
					vl.linkedView = v;
					viewLinks.add(vl);
					nextViewLinkId++;
					return v;				
				}
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			new SkylinException(getAM(),e+"");	
		}
		return null;
	}
	
	public View getLocalViewByLinkId(int id)
	{
		for (ViewLink vl:viewLinks)
		{
			if (vl.linkedView.getLocalLinkId() == id)
			{
				return vl.linkedView;
			}
		}	
		return null;
	}
	
	public Lov getLov(String string) 
	{
		return (Lov) getLinkedView(string + "_lov");
	}
	
	public BigDecimal getNumber(String name)
	{
		return (BigDecimal)getValue(name);
	}
	
	public BigDecimal getNumber(int i)
	{
		return (BigDecimal)getValue(i);
	}
	
	public Timestamp getDate(String name)
	{
		return (Timestamp)getValue(name);
	}
	
	public Timestamp getDate(int i)
	{
		return (Timestamp)getValue(i);
	}
	
}
