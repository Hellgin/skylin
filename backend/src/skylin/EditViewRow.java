package skylin;


public class EditViewRow extends ViewRow
{
    public EditViewRow(View view) 
    {
		super(view);
	}

	@Override
    public void setValueInternal(int i, Object object) 
    {
        if ((getValueInternal(i) == null && object == null) || (getValueInternal(i) != null && getValueInternal(i).equals(object))) {
            return;
        }
        super.setValueInternal(i, object);
        markModified();
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
    public boolean isUpdateable(int i) 
    {
        EditView vo = getHighestEditView();
        if (vo.mode == EditView.VIEW_MODE) 
        {
            return false;
        }
        if (vo.mode == EditView.ADD_MODE && getValue(vo.getIdName()) != null) 
        {
            return false;
        }
        return super.isUpdateable(i);
    }
    
    public EditView getHighestEditView() 
    {
		if (getView().getOwner() != null && getView().getOwner() instanceof EditViewRow)
		{
			return (EditView)getView().getOwner().getView();
		}
		return getEditView();
	}

	public EditView getEditView() {
        return (EditView) view;
    }
    

    
    
}
