package skylin;

public class ViewLink 
{
	View linkedView;
	Object[] linkValues;
	String[] linkCols;
	String[] linkVars;
	
	public String toString()
	{
		String ret = linkedView.getName();
		return ret;
	}
	
}
