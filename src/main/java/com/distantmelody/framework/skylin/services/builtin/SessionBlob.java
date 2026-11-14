package com.distantmelody.framework.skylin.services.builtin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.distantmelody.framework.skylin.SkylinBlob;
import com.distantmelody.framework.skylin.SkylinBlobLink;
import com.distantmelody.framework.skylin.bridge.BridgeSession;

public class SessionBlob extends HttpServlet 
{

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/octet-stream");
		
		BridgeSession session = (BridgeSession)req.getAttribute("BRIDGE_SESSION");
		if (session == null)
		{
			resp.getOutputStream().write("no session found".getBytes(StandardCharsets.UTF_8));
			return;
		}
		
		byte[] data = new byte[req.getInputStream().available()];
		req.getInputStream().read(data);
		
		Integer no = null;
		try
		{
			no = Integer.valueOf(new String(data, StandardCharsets.US_ASCII).trim());
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
