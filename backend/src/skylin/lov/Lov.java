package skylin.lov;


import java.math.BigDecimal;
import java.util.ArrayList;

import skylin.AM;
import skylin.View;
import skylin.ViewRow;

public class Lov extends View
{
	FilterColumn[] filterColumns = {};
	DisplayColumn[] displayColumns = {};
	
	String seperator = " ";
	
	public boolean skipValidationForSecondaryMappings;

	public Lov(AM am) {
		super(am);
	}	
	
	public void setDisplayColumns(String[] displayColumns,String[] displayColumNames,int[] displayWidths)
	{
		this.displayColumns = new DisplayColumn[displayColumns.length];
		for (int i = 0; i < displayColumns.length;i++)
		{
			this.displayColumns[i] = new DisplayColumn();
			this.displayColumns[i].displayColumn = displayColumns[i];
			this.displayColumns[i].displayColumnLabel = displayColumNames[i];
			this.displayColumns[i].displayWidth = displayWidths[i];
		}
		
	}
	
	public DisplayColumn[] getDisplayColumns()
	{
		return displayColumns;
	}
	
	public void selectRow(ViewRow row)
	{
		for (int i = 0; i < columnNames.length;i++)
		{
	    	if (owner.hasColumn(columnNames[i]))
	    	{
	    		Object value = null;
	    		if (row != null)
	    		{
	    			value = row.getValue(columnNames[i]);
	    		}
	    		if (skipValidationForSecondaryMappings)
	    		{
	    			owner.setValueInternal(columnNames[i],value);
	    		}
	    		else
	    		{
	    			owner.setValue(columnNames[i],value);
	    		}
	    	}
		}
	}
	
	public void setUserFilterColumns(String[] filterColumns,String[] filterColumnLabels)
	{
		this.filterColumns = new FilterColumn[filterColumns.length]; 
		for (int i = 0; i < filterColumns.length;i++)
		{
			this.filterColumns[i] = new FilterColumn();
			this.filterColumns[i].filterColumn = filterColumns[i];
			this.filterColumns[i].filterLabel = filterColumnLabels[i];
		}
		
	}
	
	public FilterColumn[] getUserFilterColumns()
	{
		return filterColumns;
	}
	
	public void setSeperator(String value)
	{
		seperator = value;
	}
	
	public String getSeperator()
	{
		return seperator;
	}
	
	
	public int getWidth()
	{
		int w = 0;
		for (DisplayColumn c:displayColumns)
		{
			w+=c.getDisplayWidth();
		}
		return w;
	}
	
	public ViewRow findRowByUserInput(String col,Object val)
	{
        if (columnTypes[getColumnIndex(col)].equals(BigDecimal.class))
        {
        	if  (val != null)
        	{
	        	try
	        	{
	        		val = new BigDecimal(Float.parseFloat((String) val));
	        	}
	        	catch (Exception e)
	        	{
	        		//return "Invalid number";
	        	}
        	}
        }
		ArrayList<ViewRow> r = getRowsWhere(col,val);	
		if (r.size() == 0)
		{
			return null;
		}
		return r.get(0);
	}
	
	public void setQuery(String q)
	{
		super.setQuery(q);
		for (int i = 0; i < bindingNames.size();i++)
		{
			addDefaultLink(bindingNames.get(i), bindingNames.get(i));	
		}
	}
	
	public boolean doInListValidation()
	{
		return true;
	}
}
