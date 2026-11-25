package com.distantmelody.framework.skylin.services.builtin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.distantmelody.framework.skylin.AM;
import com.distantmelody.framework.skylin.bridge.BridgeSession;
import com.distantmelody.library.json.J;

/**
 * {@code restless.php?com.distantmelody.framework.skylin.services.builtin.MessageAM} with the body
 * {@code {"amId":<id>,"channel":"<channel>","message":<json>}}: hands the message to {@link AM#receiveMessage} of that
 * AM in the caller's own session and answers with what it returns.
 */
public class MessageAM extends HttpServlet
{
	private static final Logger LOG = LoggerFactory.getLogger(MessageAM.class);

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		BridgeSession session = (BridgeSession)req.getAttribute("BRIDGE_SESSION");
		if (session == null)
		{
			return;
		}

		int amId;
		String channel;
		J message;
		try
		{
			J in = J.fromString(new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
			amId = in.getInt("amId");
			channel = in.getString("channel");
			message = in.getJ("message");
		}
		catch (RuntimeException | StackOverflowError e)
		{
			LOG.warn("MessageAM: the request body is not a message ({})", e.getClass().getSimpleName());
			return;
		}

		if (!(session.getObject(amId) instanceof AM am))
		{
			LOG.warn("MessageAM: object {} of the session is not an AM", amId);
			return;
		}
		if (channel == null)channel = "DEFAULT";
		byte[] response = am.receiveMessage(message,channel);
		if (response != null)
		{
			resp.getOutputStream().write(response);
		}
	}
}
