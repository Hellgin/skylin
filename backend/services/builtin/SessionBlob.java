package skylin.services.builtin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import skylin.SkylinBlob;
import skylin.SkylinBlobLink;
import skylin.bridge.BridgeSession;

public class SessionBlob extends HttpServlet 
{

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/octet-stream");
		
		BridgeSession session = (BridgeSession)req.getAttribute("BRIDGE_SESSION");
		if (session == null)
		{
			resp.getOutputStream().write("no session found".getBytes());
			return;
		}
		
		byte[] data = new byte[req.getInputStream().available()];
		req.getInputStream().read(data);
		
		Integer no = null;
		try
		{
			no = new Integer(new String(data));
		}
		catch (Exception e)
		{
			return;
		}
		
		SkylinBlobLink b =session.getExposedBlobLink(no);
		if (b == null)
		{
			return;
		}
		if (b.linkInfo != null)
		{
			if (b.linkInfo.containsKey("filename"))
			{
				resp.setHeader("Content-Disposition","attachment; filename=\""+b.linkInfo.get("filename")+"\"");
			}
		}
		
		resp.getOutputStream().write(b.blob.getData());
	}

}
