package com.distantmelody.framework.skylin.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.ByteBuffer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.Test;

import com.distantmelody.framework.skylin.bridge.BridgeSession;

/** A closed browser socket must leave the server map and the bridge session. */
class SocketServerCloseTest
{
	@Test
	void closedConnectionDropsTheSessionSocket() throws Exception
	{
		String id = "socketclosetest0123456789abcdef";
		BridgeSession session = new BridgeSession(id, null);
		BridgeSession.sessions.put(id, session);
		SocketServer server = new SocketServer(0, 1, SocketServer.DEFAULT_MAX_MESSAGE_SIZE);
		WebSocketClient client = null;
		try
		{
			server.start();
			long deadline = System.currentTimeMillis() + 5000;
			while (server.getPort() <= 0 && System.currentTimeMillis() < deadline)
			{
				Thread.sleep(20);
			}
			client = new QuietClient(new URI("ws://127.0.0.1:" + server.getPort() + "/SessionSocket"), id);
			assertTrue(client.connectBlocking(5, java.util.concurrent.TimeUnit.SECONDS));
			client.send("CONNECTED,1," + id);
			waitUntil(5000, () -> server.sockets.size() == 1);
			assertEquals(1, server.sockets.size());
			assertEquals(1, session.getSessionSockets(1).size());

			client.closeBlocking();
			waitUntil(5000, () -> server.sockets.isEmpty());
			assertEquals(0, server.sockets.size());
			assertEquals(0, session.getSessionSockets(1).size());
		}
		finally
		{
			if (client != null)
			{
				client.closeBlocking();
			}
			server.stop(1000);
			BridgeSession.sessions.remove(id);
		}
	}

	private static void waitUntil(long timeoutMillis, Check check) throws Exception
	{
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (!check.ready() && System.currentTimeMillis() < deadline)
		{
			Thread.sleep(20);
		}
	}

	private interface Check
	{
		boolean ready() throws Exception;
	}

	private static class QuietClient extends WebSocketClient
	{
		QuietClient(URI serverUri, String sessionId)
		{
			super(serverUri, java.util.Map.of("Cookie", "PHPSESSID=" + sessionId));
		}

		public void onOpen(ServerHandshake handshake)
		{
		}

		public void onMessage(String message)
		{
		}

		public void onMessage(ByteBuffer bytes)
		{
		}

		public void onClose(int code, String reason, boolean remote)
		{
		}

		public void onError(Exception ex)
		{
		}
	}
}
