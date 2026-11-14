package com.distantmelody.framework.skylin.services.builtin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.distantmelody.framework.skylin.AM;
import com.distantmelody.framework.skylin.bridge.BridgeSession;
import com.distantmelody.library.json.J;

public class MessageAM extends HttpServlet
{
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		byte[] data = new byte[req.getInputStream().available()];
		req.getInputStream().read(data);
		J in = J.fromString(new String(data, StandardCharsets.UTF_8));

		BridgeSession session = null;
		if (in.contains("sessionId"))
		{
			session = BridgeSession.sessions.get(in.getString("sessionId"));//used passed in session
		}
		
		if (session == null)
		{
			session = (BridgeSession)req.getAttribute("BRIDGE_SESSION");//use actual session
		}
		if (session == null)
		{
			return;
		}
		
		AM am = (AM) session.getObject(in.getInt("amId"));
		String channel = in.getString("channel");
		if (channel == null)channel = "DEFAULT";
		byte[] response = am.receiveMessage(in.getJ("message"),channel);
		if (response != null)
		{
			resp.getOutputStream().write(response);
		}
	}
}
