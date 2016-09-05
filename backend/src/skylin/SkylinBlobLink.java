package skylin;

import java.util.Map;

public class SkylinBlobLink 
{
	public SkylinBlobLink(SkylinBlob blob,Map<String,Object> linkInfo)
	{
		this.blob = blob;
		this.linkInfo = linkInfo;
	}
	public SkylinBlob blob;
	public long createTime = System.currentTimeMillis();
	public Map<String,Object> linkInfo;
}
