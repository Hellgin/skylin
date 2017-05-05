package skylin.services.builtin;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import skylin.AM;
import skylin.bridge.BridgeSession;
import skylin.services.J;

public class MessageAM extends HttpServlet
{
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		BridgeSession session = (BridgeSession)req.getAttribute("BRIDGE_SESSION");
		if (session == null)
		{
			return;
		}
		
		byte[] data = new byte[req.getInputStream().available()];
		req.getInputStream().read(data);

		
		J in = J.fromString(new String(data));
		AM am = (AM) session.getObject(in.getInt("amId"));
		am.receiveMessage(in.getJ("message"),in.getString("channel"));
	}
}
