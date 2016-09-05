package skylin;

import java.util.ArrayList;
import java.util.HashMap;

import skylin.services.J;

public class JsonViewRow extends ViewRow
{

	public JsonViewRow(JsonView view) {
		super(view);
		// TODO Auto-generated constructor stub
	}

	public void fromJ(J json,JsonViewGenConfig config) 
	{
		if (!json.isMap())
		{
			new SkylinException(getAM(),"JsonView " + getClass().getName() + " needs an map object but an array/element was provided.");
		}
		
		HashMap<String,ArrayList<J>> views = new HashMap<String,ArrayList<J>>();
		for (String k:json.keys())
		{
			Object v = json.get(k);
			if (v instanceof J)
			{
				J vj = (J)v;
				String splitType = (String) vj.get(config.splitType).get(0);
				ArrayList<J> rowsInView = views.get(splitType);
				if (rowsInView == null)
				{
					rowsInView = new ArrayList<J>();
					views.put(splitType, rowsInView);
				}
				rowsInView.add(vj);
			}
			else
			{
				setValue(k,v);
			}
		}
		for (String k:views.keySet())
		{
			JsonView jview = (JsonView) getLinkedView(JsonView.class, k);
			jview.fromJ(J.A(views.get(k).toArray()), config);
		}
	}
	
	public final void setValueInternal(String name, Object value)
	{
		for (int i = 0; i < view.columnNames.length;i++)
		{
			if (name.equals(view.columnNames[i]))
			{
				setValueInternal(i,value);
				return;
			}
		}
		String[] oldNames = view.columnNames;
		view.columnNames = new String[oldNames.length+1];
		System.arraycopy(oldNames, 0, view.columnNames, 0, oldNames.length);
		
		Class[] oldTypes = view.columnTypes;
		view.columnTypes = new Class[oldTypes.length+1];
		System.arraycopy(oldTypes, 0, view.columnTypes, 0, oldTypes.length);
		
		boolean[] oldEdit = view.columnEditable;
		view.columnEditable = new boolean[oldEdit.length+1];
		System.arraycopy(oldEdit, 0, view.columnEditable, 0, oldEdit.length);
		
		view.columnNames[oldNames.length] = name;
		view.columnTypes[oldTypes.length] = value.getClass();
		view.columnEditable[oldEdit.length] = true;
		
		setValueInternal(oldNames.length,value);
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
		return null;
	}
	
	public boolean isUpdateable(int i)
	{
		if (view.columnEditable != null && i >= view.columnEditable.length)
		{
			return false;
		}
		return super.isUpdateable(i);
	}

}
