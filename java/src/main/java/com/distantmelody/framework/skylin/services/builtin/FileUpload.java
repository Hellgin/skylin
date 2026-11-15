package com.distantmelody.framework.skylin.services.builtin;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.distantmelody.framework.skylin.View;
import com.distantmelody.framework.skylin.ViewRow;
import com.distantmelody.framework.skylin.bridge.BridgeSession;

public class FileUpload extends HttpServlet
{
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		byte[] data = new byte[req.getInputStream().available()];
		req.getInputStream().read(data);
		String[] in = req.getHeader("Authorization").split("_",5);
		
		int viewId = Integer.parseInt(in[0]);
		String nameCol = in[1];
		String blobCol = in[2];
		String sessionId = in[3];
		String fileName = in[4];
		
		BridgeSession session = BridgeSession.sessions.get(sessionId);
		
		View view = (View) session.getObject(viewId);
		
		ViewRow row = view.newRow();
		row.setValue(nameCol, fileName);
		row.getBlob(blobCol).setData(data);
	}
	
}
