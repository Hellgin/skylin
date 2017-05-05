package skylin;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;

import skylin.bridge.BridgeSession;

public class SkylinBlob 
{
	public byte[] data = new byte[0];
	BridgeSession session;
	
	public SkylinBlob()
	{
		
	}
	
	public SkylinBlob(BridgeSession session,String filePath) throws IOException {
		this.session = session;
		data = java.nio.file.Files.readAllBytes(new File(filePath).toPath());
	}
	
	public SkylinBlob(AM am,String filePath) throws IOException {
		this.session = am.session;
		data = java.nio.file.Files.readAllBytes(new File(filePath).toPath());
	}
	
	public SkylinBlob(AM am,byte data[])
	{
		this.session = am.session;
		this.data = data;
	}
	
	public SkylinBlob(AM am)
	{
		this.session = am.session;
	}
	
	public byte[] getData()
	{
		return data;
	}
	
	public void setData(byte[] data)
	{
		this.data = data;
	}
	
	public String getUrl()
	{
		return getUrl(null);
	}
	
	public String getUrl(String filename)
	{
		HashMap<String,Object> linkInfo = new HashMap<String,Object>();
		linkInfo.put("filename", filename);
		return "/restless.php?skylin.services.builtin.SessionBlob,"+session.exposeBlob(this,linkInfo);
	}
	
	public int size()
	{
		return data.length;
	}
	
	public int getSize()
	{
		return size();
	}
	
}
