package com.distantmelody.framework.skylin;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.distantmelody.framework.skylin.lov.Lov;
import com.distantmelody.framework.skylin.util.Util;
import com.distantmelody.library.json.J;

public class NullViewRow
{
	public void setValue(String name, Object value)
	{
	}
	
	public Object getValue(String name)
	{
		return null;
	}
	
	public void setValueInternal(String name, Object value)
	{
		
	}
	
	public Object getValueInternal(String name)
	{
		return null;
	}
	
	
	
	public final void setValue(int i, Object value)
	{
	}

	public final void setValueInternal(int i, Object value)
	{
	}
	
	public final Object getValue(int i)
	{
		return null;
	}
	
	public final Object getValueInternal(int i)
	{
		return null;
	}
	
	
	public String getName(int i)
	{
		return null;
	}
	
	public Object getKey()
	{
		return null;
	}

	public final boolean isUpdateable(int i)
	{
		return false;
	}


	public void validate(String name, Object value) throws SQLException, ValidationException
	{

	}
	
	public void validate(String name, Object value, boolean serverValue) throws SQLException, ValidationException
	{

	}

	public final boolean isAllColumnsValid()
	{
		return true;
	}

	public boolean hasColumn(String name) 
	{
		return false;
	}
	
	public void validationError(String body) throws ValidationException
	{

	}
	
	public void validationError(String header, String body) throws ValidationException
	{

	}
	

	
	public int getIndex()
	{
		return 0;
	}
	
	public View getView()
	{
		return null;
	}
	
	//deprecated use getView
	public View getViewObject(String name)
	{
		return null;
	}
	
	public View getView(String name)
	{
		return null;
	}

	@Deprecated // use initNew instead. If you need to initualize all rows use the rows constructor.
	public void init() 
	{

	}
	public void initNew() 
	{

	}
	
	public AM getAM()
	{
		return null;
	}
	
	public Object[] setValueFromFrontEnd(String name, Object value)
	{
		return null;
	}
	
	public String setFileFromFrontEnd(String col, String filePath, String colName, String name)
	{
		return null;
	}
	
	public void setValuesFromViewRow(ViewRow other, boolean internal)
	{

	}
	
	public void setValuesFromJ(J j, boolean internal) 
	{

	}
	
	public String getLinkId()
	{
		return null;
	}
	
	public long getLocalLinkId()
	{
		return 0;
	}
	
	public void linkChain(ArrayList<Integer> chain)
	{
	}
	
	public String getHtmlInputType(String col)
	{
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
		return null;
	}
	public View getLinkedView(String instanceName)
	{
		return null;
	}
	
	public View getLocalViewByLinkId(int id)
	{
		return null;
	}
	
	public Lov getLov(String string) 
	{
		return null;
	}
	
	public BigDecimal getNumber(String name)
	{
		return null;
	}
	
	public Timestamp getDate(String name)
	{
		return null;
	}
	
	public SkylinBlob getBlob(String name)
	{
		return null;
	}
	
	
	public void fromJ(J json,JToViewConfig config) 
	{
	}
	
	public J toJ(ViewToJConfig config) {
		return J.M();
	}
	
	
	public List<ViewRow> getTreeLinkedRows(String layoutName)
	{
		return null;
	}
	
	public boolean isTreeExpandable(String layoutName)
	{
		return false;
	}
	
	public boolean isTreeExpanded()
	{
		return false ;
	}
	
	public void setTreeExpanded(boolean b)
	{
	}
	
	public void toggleTreeExpanded()
	{
	}
	
	public boolean isCurrentRow()
	{
		return false;
	}
	
	public boolean isFirst()
	{
		return false;
	}
	
	public boolean isLast()
	{
		return false;
	}
	
	public boolean isSelectable()
	{
		return false;
	}
	
	public void moveTo(ViewRow to)
	{
	}
	
	public void moveToPage(int pageNo,int pageSize)
	{
	}
	
	public final List<ViewRow> getFullTree(String layoutName,int depth)
	{
		return null;
	}
	public final List<ViewRow> getFullTree(String layoutName)
	{
		return getFullTree(layoutName,0);
	}
	
	public int getTreeDepth()
	{
		return 0;
	}
	
	protected void resetFullId()
	{
	}
	
	public String getCurrentValueValidation(String col)
	{
		return null;
	}
	
	public void validateAll() throws SQLException, ValidationException
	{
	}
	
	public void setAllNull()
	{
	}
	
	
	public void setAllNullInternal()
	{
	}
	public void remove()
	{
	}

    public void markModified()
    {
    }
    
    public boolean isUpdateable(String name) 
    {
    	return false;
    }
    
    public EditView getHighestEditView() 
    {
		if (getView().getOwner() != null && getView().getOwner() instanceof EditViewRow)
		{
			return ((EditViewRow)getView().getOwner()).getHighestEditView();
		}
		return getEditView();
	}

	public EditView getEditView() {
        return null;
    }
    

	public void transfer(View to)
	{
	}
	
	public void delete() 
	{
	}
    

}
