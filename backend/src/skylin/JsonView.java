package skylin;

import skylin.services.J;

public class JsonView extends View{

	
	public JsonView(AM am) 
	{
		super(am);
	}

	public void fromJ(J json,String splitType)
	{
		fromJ(json,new JsonViewGenConfig(splitType));
	}
	
	public void fromJ(String json,String splitType)
	{
		fromJ(J.fromString(json),new JsonViewGenConfig(splitType));
	}
	
	public void fromJ(String json,JsonViewGenConfig config)
	{
		fromJ(J.fromString(json),config);
	}
	
	public void fromJ(J json,JsonViewGenConfig config)
	{
		if (json.isMap())
		{
			new SkylinException(getAM(),"JsonView " + getClass().getName() + " needs an array object but an map was provided.");
		}
		
		for (J j:json.valuesj())
		{
			JsonViewRow row = (JsonViewRow) newRow();
			row.fromJ(j,config);
		}
	}
	
	

}
