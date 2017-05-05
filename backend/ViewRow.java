package skylin;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import skylin.lov.Lov;
import skylin.services.J;
import skylin.util.Util;


public class ViewRow 
{
	public View view;
	HashMap<String,Object> values = new HashMap<String,Object>();
	
	int linkId;
	String fullLinkId;
	int nextViewLinkId = 1;
	
	public ArrayList<ViewLink> viewLinks;
	private boolean treeExpanded;
	private int treeDepth;
    
	public ViewRow(View view)
	{
		if (getClass() != view.getRowType())
		{
			throw new SkylinException(getAM(),"Cannot add row of type " + getClass().getSimpleName() + " to view object " + view.getClass().getSimpleName() + " because it uses rows of type " + view.getRowType().getSimpleName());
		}
		this.view = view;
		view.rows.add(this);

		for (int i = 0; i < view.columnTypes.length;i++)
		{
			if (view.getColumnType(i) == SkylinBlob.class)
			{
				setValueInternal(getName(i),new SkylinBlob());
			}
		}
		
		linkId = view.nextRowLinkId;
		view.nextRowLinkId++;
	}
	
	public void setValue(String name, Object value)
	{
		if (view.freeColumns)
		{
			setValueInternal(name,value);
			return;
		}
		for (int i = 0; i < view.columnNames.length;i++)
		{
			if (name.equals(view.columnNames[i]))
			{
				setValueInternal(name,value);
				return;
			}
		}
		throw new SkylinException(getAM(),"CANNOT SET COLUMN VALUE: no column named " + name + " exists in view object " + view.getClass().getSimpleName());
	}
	
	public Object getValue(String name)
	{
		if (view.freeColumns)
		{
			return  getValueInternal(name);
		}
		for (int i = 0; i < view.columnNames.length;i++)
		{
			if (name.equals(view.columnNames[i]))
			{
				return  getValueInternal(name);
			}
		}
		throw new SkylinException(getAM(),"CANNOT GET COLUMN VALUE: no column named " + name + " exists in view object " + view.getClass().getSimpleName());
	}
	
	public void setValueInternal(String name, Object value)
	{
		if (view.freeColumns)
		{
			values.put(name, value);
			return;
		}
		for (int i = 0; i < view.columnNames.length;i++)
		{
			if (name.equals(view.columnNames[i]))
			{
				values.put(name, value);
				return;
			}
		}
		throw new SkylinException(getAM(),"CANNOT SET COLUMN VALUE: no column named " + name + " exists in view object " + view.getClass().getSimpleName());
	}
	
	public Object getValueInternal(String name)
	{
		if (view.freeColumns)
		{
			return values.get(name);
		}
		for (int i = 0; i < view.columnNames.length;i++)
		{
			if (name.equals(view.columnNames[i]))
			{
				return values.get(name);
			}
		}
		throw new SkylinException(getAM(),"CANNOT GET COLUMN VALUE: no column named " + name + " exists in view object " + view.getClass().getSimpleName());
	}
	
	
	
	public final void setValue(int i, Object value)
	{
		setValue(getName(i),value);
	}

	public final void setValueInternal(int i, Object value)
	{
		setValueInternal(getName(i),value);
	}
	
	public final Object getValue(int i)
	{
		return getValue(getName(i));
	}
	
	public final Object getValueInternal(int i)
	{
		return getValueInternal(getName(i));
	}
	
	
	public String getName(int i)
	{
		return view.columnNames[i];
	}
	
	public Object getKey()
	{
		ArrayList<Object> key = new ArrayList<Object>();
		for (int i = 0; i < view.keyFields.length;i++)
		{
			key.add(values.get(view.keyFields[i]));
		}
		return key;
	}

	public final boolean isUpdateable(int i)
	{
		return isUpdateable(getName(i));
	}

	public boolean isUpdateable(String name)
	{
		if (view.columnEditable != null && !view.columnEditable[view.getColumnIndex(name)])
		{
			return false;
		}
		return !view.getReadOnly();
	}
	
	public void validate(String name, Object value) throws SQLException, ValidationException
	{

	}
	
	public void validate(String name, Object value, boolean serverValue) throws SQLException, ValidationException
	{
		validate(name,value);
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
	
	//deprecated use getView
	public View getViewObject(String name)
	{
		return view.getViewObject(name);
	}
	
	public View getView(String name)
	{
		return view.getView(name);
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
        	if  (value != null && (value+"").length() == 0)
        	{
        		value = null;
        	}
	        if (view.getColumnType(name).equals(BigDecimal.class))
	        {
	        	if  (value != null)
	        	{
		        	try
		        	{
		        		value = new BigDecimal(value.toString());
		        	}
		        	catch (Exception e)
		        	{
		        		return "Invalid number";
		        	}
	        	}
	        }
	        try
	        {
	        	validate(name,value,false);
	        }
	        catch (ValidationException e)
	        {
	        	return e.message;
	        }
			setValue(name,value);
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
		//int iname = view.getColumnIndex(colName);
		
    	if  (filePath != null && (filePath+"").length() == 0)
    	{
    		filePath = null;
    	}
    	SkylinBlob b = null;
		//int ifilePath = view.getColumnIndex(col);
		
		try
		{
	        try
	        {
	        	validate(colName,name,false);
	        }
	        catch (ValidationException e)
	        {
	        	return e.message;
	        }

	        //view.setCurrentColumn(ifilePath);
	        if (filePath != null)
	        {
	        	b = new SkylinBlob(getAM().session,filePath);
	        }
	
	        try
	        {
	        	validate(col,b,false);
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
		
		setValue(colName,name);
		setValue(col,b);
		
		return null;
	}
	
	public void setValuesFromViewRow(ViewRow other, boolean internal)
	{
		for (int i = 0; i < view.columnNames.length;i++)
		{
			if (other.hasColumn(view.columnNames[i]))
			{
				if (internal)
				{
					setValueInternal(view.columnNames[i],other.getValueInternal(view.columnNames[i]));
				}
				else
				{
					setValue(view.columnNames[i],other.getValue(view.columnNames[i]));
				}
			}
		}
	}
	
	public void setValuesFromJ(J j, boolean internal) 
	{
		for (String key:values.keySet())
		{
			if (j.contains(key))
			{
				if (internal)
				{
					setValueInternal(key,j.get(key).get());
				}
				else
				{
					setValue(key,j.get(key).get());
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
		if (view.freeColumns)
		{
			if (col != null)
			{
				Class c = view.getColumnType(col);
				if (c.equals(View.number))
				{
					return "number";
				}
			}
			return "text";
		}
		if (col != null)
		{
			Class c = view.getColumnType(col);
			if (c.equals(View.number))
			{
				return "number";
			}
		}
		return "text";
	}
	
	//for calling from PHP via the skylin bridge since it doesnt support varargs yet.
	public View getLinkedView(Class viewClass,String a){return getLinkedView(viewClass,new String[]{a});}
	public View getLinkedView(Class viewClass,String a,String b){return getLinkedView(viewClass,new String[]{a,b});}
	public View getLinkedView(Class viewClass,String a,String b,String c){return getLinkedView(viewClass,new String[]{a,b,c});}
	public View getLinkedView(Class viewClass,String a,String b,String c,String d){return getLinkedView(viewClass,new String[]{a,b,c,d});}
	public View getLinkedView(Class viewClass,String a,String b,String c,String d, String e){return getLinkedView(viewClass,new String[]{a,b,c,d,e});}
	
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
							vl.linkedView.setBindVar(bindVar[i], view.getColumnType(col[i]), value);
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
				v.setBindVar(bindVar[i], view.getColumnType(col[i]), value);
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
							vl.linkedView.setBindVar(vl.linkVars[i], view.getColumnType(vl.linkCols[i]), value);
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
			if (view.defaultChildLinkClass != null)
			{
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
	
	public Timestamp getDate(String name)
	{
		return (Timestamp)getValue(name);
	}
	
	public SkylinBlob getBlob(String name)
	{
		return (SkylinBlob)getValue(name);
	}
	
	
	public void fromJ(J json,JToViewConfig config) 
	{
		if (!json.isMap())
		{
			new SkylinException(getAM(),"JsonView " + getClass().getName() + " needs an map object but an array/element was provided.");
		}
		HashMap<String,J> views = new HashMap<String,J>();
		for (String k:json.keys())
		{
			
			J v = json.getJ(k);
			Object value = v.get(0);
			if (value instanceof J)
			{
				views.put(k, v);
			}
			else
			{
				if (value instanceof Integer)value = new BigDecimal((Integer)value);
				else if (value instanceof Float)value = new BigDecimal((Integer)value);
				else if (value instanceof Double)value = new BigDecimal((Integer)value);
				
				if (value != null)
				{
					int i = view.getColumnIndex(k);
					if (i != -1)
					{
						Class type= view.getColumnTypes()[i];
						if (type == View.number && !(value instanceof BigDecimal))
						{
							value = new BigDecimal(value+"");
						}
						if (type == View.string && !(value instanceof String))
						{
							value = value+"";
						}
						else if (type == View.date)
						{
							value = Util.stringToDate(value+"");
						}
					}
					else
					{
						if (!(value instanceof BigDecimal))
						{
							value = value+"";
						}
					}
				}
				setValueInternal(k,value);
			}
		}
		for (String k:views.keySet())
		{
			Class c = null;
			if (config != null) c = config.getViewClasses().get(k);
			if (c == null) c = View.class;
			View jview = getLinkedView(c, k);
			jview.enableFreeColumns();
			jview.fromJ(views.get(k), config);
		}
	}
	
	public J toJ(ViewToJConfig config) {
		J ret = J.M();
		for(String key:values.keySet())
		{
			Object v = values.get(key);
			if (v != null && config.included(key))
			{
				ret.put(key, v);
			}
		}
		if (viewLinks != null)
		{
			for (ViewLink v:viewLinks)
			{
				if (config.included(v.linkedView.getName()) && v.linkedView.getSize() > 0)
				{
					ret.put(v.linkedView.getName(), v.linkedView.toJ(config));
				}
			}
		}
		return ret;
	}
	
	
	public List<ViewRow> getTreeLinkedRows(String layoutName)
	{
		return null;
	}
	
	public boolean isTreeExpandable(String layoutName)
	{
		List<ViewRow> rows = getTreeLinkedRows(layoutName);
		return rows != null && rows.size() > 0;
	}
	
	public boolean isTreeExpanded()
	{
		return treeExpanded ;
	}
	
	public void setTreeExpanded(boolean b)
	{
		treeExpanded = b;
	}
	
	public void toggleTreeExpanded()
	{
		treeExpanded = !treeExpanded;
	}
	
	public boolean isCurrentRow()
	{
		return getView().getCurrentRow() == this;
	}
	
	public boolean isFirst()
	{
		return getView().getFirst() == this;
	}
	
	public boolean isLast()
	{
		return getView().getLast() == this;
	}
	
	public boolean isSelectable()
	{
		return getView().isSelectable();
	}
	
	public void moveTo(ViewRow to)
	{
		if (getView() != to.getView()) return;
		int dest = view.getRows().indexOf(to);
		ViewRow currentRow = view.getCurrentRow();
		view.rows.remove(this);
		view.rows.add(dest,this);
		view.currentRowIndex = view.rows.indexOf(currentRow);
	}
	
	public final List<ViewRow> getFullTree(String layoutName,int depth)
	{
		treeDepth = depth;
		depth++;
		List<ViewRow> ret = new ArrayList<ViewRow>();
		
		if (isTreeExpanded())
		{
			List<ViewRow> linked = getTreeLinkedRows(layoutName);
			if (linked != null)
			{
				for (ViewRow r:linked)
				{
					ret.add(r);
					ret.addAll(r.getFullTree(layoutName,depth));
				}
			}
		}
		
		return ret;
	}
	public final List<ViewRow> getFullTree(String layoutName)
	{
		return getFullTree(layoutName,0);
	}
	
	public int getTreeDepth()
	{
		return treeDepth;
	}
	
	public void transfer(View to)
	{
		if (!getClass().isAssignableFrom(to.getRowType()))
		{
			throw new SkylinException(getAM(),"CANNOT TRANSFER ROW: the row is not a instance of or an instance of a subclass of the rowtype of the target view: " + to.getRowType().getName());
		}
		view.remove(this);
		view = to;
		view.rows.add(this);
		
		linkId = view.nextRowLinkId;
		view.nextRowLinkId++;
		
		//fullLinkId = null;
		resetFullId();
	}
	
	protected void resetFullId()
	{
		fullLinkId = null;
		if (viewLinks != null)
		{
			for (ViewLink vl:viewLinks)
			{
				for (ViewRow r:vl.linkedView.getRows())
				{
					r.resetFullId();
				}
			}
		}
	}
	
	public String getCurrentValueValidation(String col)
	{
		try
		{
			validate(col,getValueInternal(col),true);
		} 
        catch (ValidationException e)
        {
        	return e.message;
        }
		catch (SQLException e)
		{
			if (e.getErrorCode() == 20900) 
	        {
	          String m = e.getMessage();
	          m = m.substring(0,m.indexOf("\n"));
	          m = m.substring(m.lastIndexOf("ORA-"),m.length());
	          m = m.substring(m.indexOf(" "), m.length());
	          return m;
	        }
	        else {
	          return e.getMessage();
	        }
		}

		return null;
	}
	
	public void validateAll() throws SQLException, ValidationException
	{
		if (view.isFreeColumnsEnabled())
		{
			for (String s:values.keySet())
			{
				validate(s,values.get(s),true);	
			}
		}
		for (String s:view.columnNames)
		{
			validate(s,values.get(s),true);	
		}
		if (viewLinks != null)
		{
			for (ViewLink vl:viewLinks)
			{
				vl.linkedView.validateAll();
			}
		}
	}
	
	public void setAllNull()
	{
		for (String k:values.keySet())
		{
			setValue(k,null);
		}
	}
	
	
	public void setAllNullInternal()
	{
		for (String k:values.keySet())
		{
			setValueInternal(k,null);
		}
	}
	public void remove()
	{
		getView().remove(this);
	}
}
