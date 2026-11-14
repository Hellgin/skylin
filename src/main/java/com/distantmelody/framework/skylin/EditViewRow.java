package com.distantmelody.framework.skylin;

import java.util.ArrayList;


public class EditViewRow extends ViewRow
{
	/** Columns whose stored value {@link #setValueInternal(String, Object)} has replaced. */
	private final ArrayList<String> modifiedColumns = new ArrayList<String>();

    public EditViewRow(View view) 
    {
		super(view);
	}

	@Override
    public void setValueInternal(String i, Object object) 
    {
        if ((getValueInternal(i) == null && object == null) || (getValueInternal(i) != null && getValueInternal(i).equals(object))) {
            return;
        }
        super.setValueInternal(i, object);
        // ViewRow's constructor sets blob columns before this row's fields are initialised.
        if (modifiedColumns == null)
        {
        	return;
        }
        if (!modifiedColumns.contains(i))
        {
        	modifiedColumns.add(i);
        }
        markModified();
    }

	/** True when this column's value was replaced after the row was read. A query fills the row without marking it. */
	public boolean isModified(String column)
	{
		return modifiedColumns.contains(column);
	}

	/** Drops the column record. Called when the view drops this row from {@code modifiedRows} but keeps the row. */
	void clearModifiedColumns()
	{
		modifiedColumns.clear();
	}
    
    public void markModified()
    {
        if (!((EditView)view).modifiedRows.contains(this))
        {
            ((EditView)view).modifiedRows.add(this);
        }
        if (getView().owner != null && getView().owner instanceof EditViewRow)
        {
        	((EditViewRow)getView().owner).markModified();
        }
    }
    
    @Override
    public boolean isUpdateable(String name) 
    {
        EditView vo = getHighestEditView();
        if (vo.mode == EditView.VIEW_MODE) 
        {
            return false;
        }
        if (vo.mode == EditView.ADD_MODE && getValue(vo.getRowIdCol()) != null) 
        {
            return false;
        }
        return super.isUpdateable(name);
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
        return (EditView) view;
    }
    

	public void transfer(View to)
	{
		super.transfer(to);
		markModified();
	}
	
	public void delete() 
	{
		getEditView().delete(this);
	}

	/**
	 * The problems of the editable columns, joined by {@code separator}, or null when every one is acceptable.
	 * A column is included when {@code setEditableColumns} marked it. When that list was never set, every column is
	 * included. Each column is asked through {@link #getCurrentValueValidation(String)}.
	 */
	public String concatValidationErrors(String separator)
	{
		String[] names = view.getColumnNames();
		if (names == null || names.length == 0)
		{
			return null;
		}
		StringBuilder found = new StringBuilder();
		for (int i = 0; i < names.length; i++)
		{
			if (view.columnEditable != null && (i >= view.columnEditable.length || !view.columnEditable[i]))
			{
				continue;
			}
			String error = getCurrentValueValidation(names[i]);
			if (error != null)
			{
				if (found.length() > 0)
				{
					found.append(separator);
				}
				found.append(error);
			}
		}
		return found.length() == 0 ? null : found.toString();
	}
    
}
