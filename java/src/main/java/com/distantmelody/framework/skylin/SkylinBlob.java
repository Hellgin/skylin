package com.distantmelody.framework.skylin;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;

import com.distantmelody.framework.skylin.bridge.BridgeSession;
import com.distantmelody.framework.skylin.util.Util;

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
		String skylinRoot = Util.getFrameworkParam("SKYLIN_ROOT");
		String path = "restless.php?com.distantmelody.framework.skylin.services.builtin.SessionBlob?"+session.exposeBlob(this,linkInfo);
		if (skylinRoot == null || skylinRoot.isEmpty())
		{
			return path;
		}
		return skylinRoot+"/"+path;
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
