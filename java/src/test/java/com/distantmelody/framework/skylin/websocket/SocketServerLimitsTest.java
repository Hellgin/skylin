package com.distantmelody.framework.skylin.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.distantmelody.framework.skylin.AM;
import com.distantmelody.framework.skylin.ViewRow;
import com.distantmelody.framework.skylin.bridge.BridgeSession;
import com.distantmelody.library.json.J;

/**
 * The browser socket refuses messages over the size limit, survives a frame header that claims 2 GB, keeps only
 * browsers with a session this JVM holds, and takes that session from the handshake cookie.
 */
class SocketServerLimitsTest
{
	private static final String ID = "socketlimitstest0123456789abcdef";
	private static final int LIMIT = 4096;

	private SocketServer server;

	@BeforeEach
	void start() throws Exception
	{
		BridgeSession.sessions.put(ID, new BridgeSession(ID, null));
		server = new SocketServer(0, 1, LIMIT);
		server.start();
		long deadline = System.currentTimeMillis() + 5000;
		while (server.getPort() <= 0 && System.currentTimeMillis() < deadline)
		{
			Thread.sleep(20);
		}
	}

	@AfterEach
	void stop() throws Exception
	{
		server.stop(1000);
		BridgeSession.sessions.remove(ID);
	}

	@Test
	void theLimitComesFromFrameworkParamsOrTheDefault()
	{
		assertEquals(SocketServer.DEFAULT_MAX_MESSAGE_SIZE, SocketServer.maxMessageSize(null));
		assertEquals(SocketServer.DEFAULT_MAX_MESSAGE_SIZE, SocketServer.maxMessageSize(" "));
		assertEquals(5000000, SocketServer.maxMessageSize(" 5000000 "));
		assertEquals(SocketServer.DEFAULT_MAX_MESSAGE_SIZE, SocketServer.maxMessageSize("0"));
		assertEquals(SocketServer.DEFAULT_MAX_MESSAGE_SIZE, SocketServer.maxMessageSize("1MB"));
		assertEquals(1024 * 1024, SocketServer.DEFAULT_MAX_MESSAGE_SIZE);
	}

	@Test
	void theSessionCookieIsReadFromTheCookieHeader()
	{
		assertEquals(ID, SocketServer.sessionCookie("a=1; PHPSESSID=" + ID + "; b=2"));
		assertEquals(ID, SocketServer.sessionCookie("PHPSESSID=" + ID));
		assertNull(SocketServer.sessionCookie(null));
		assertNull(SocketServer.sessionCookie("a=1"));
		assertNull(SocketServer.sessionCookie("PHPSESSID=../../x"));
	}

	@Test
	void aBrowserWithoutAKnownSessionIsToldToReloadAndClosed() throws Exception
	{
		Client unknown = new Client("PHPSESSID=unknownsession0123456789abcdef");
		assertTrue(unknown.connectBlocking(5, TimeUnit.SECONDS));
		assertTrue(unknown.closed.await(5, TimeUnit.SECONDS));
		assertEquals("FORCE_REFRESH", unknown.lastText);

		Client none = new Client(null);
		assertTrue(none.connectBlocking(5, TimeUnit.SECONDS));
		assertTrue(none.closed.await(5, TimeUnit.SECONDS));
		assertEquals("FORCE_REFRESH", none.lastText);
	}

	@Test
	void theSessionComesFromTheCookieNotFromTheMessage() throws Exception
	{
		String other = "othersession0123456789abcdef0123";
		BridgeSession otherSession = new BridgeSession(other, null);
		BridgeSession.sessions.put(other, otherSession);
		try
		{
			Client naming = new Client("PHPSESSID=" + ID);
			assertTrue(naming.connectBlocking(5, TimeUnit.SECONDS));
			naming.send("CONNECTED,7," + other);
			Client plain = new Client("PHPSESSID=" + ID);
			assertTrue(plain.connectBlocking(5, TimeUnit.SECONDS));
			plain.send("CONNECTED,8");

			BridgeSession own = BridgeSession.sessions.get(ID);
			long deadline = System.currentTimeMillis() + 5000;
			while ((own.getSessionSockets(7).isEmpty() || own.getSessionSockets(8).isEmpty())
				&& System.currentTimeMillis() < deadline)
			{
				Thread.sleep(20);
			}
			assertEquals(1, own.getSessionSockets(7).size(), "a message that still names a session joins the cookie's");
			assertEquals(1, own.getSessionSockets(8).size());
			assertTrue(otherSession.getSessionSockets(7).isEmpty());
			naming.closeBlocking();
			plain.closeBlocking();
		}
		finally
		{
			BridgeSession.sessions.remove(other);
		}
	}

	@Test
	void channelMessagesReachTheTemplatesAm() throws Exception
	{
		BridgeSession own = BridgeSession.sessions.get(ID);
		String amId = own.instantiateByName(RecordingAM.class.getName());
		RecordingAM am = (RecordingAM) own.getObject(Integer.parseInt(amId));
		am.setObjectId(Integer.parseInt(amId));
		am.setFrontEndApplicationId(1);

		Client client = new Client("PHPSESSID=" + ID);
		assertTrue(client.connectBlocking(5, TimeUnit.SECONDS));
		client.send("AM," + am.channel("bad") + ",{not json");
		client.send("AM,999x1xgame,{}");
		assertTrue(client.closed.await(5, TimeUnit.SECONDS), "an AM the session does not have refreshes the page");
		assertEquals("FORCE_REFRESH", client.lastText);

		client = new Client("PHPSESSID=" + ID);
		assertTrue(client.connectBlocking(5, TimeUnit.SECONDS));
		client.send("AM,1x1xgame");
		client.send("AM," + am.channel("game") + ",{\"x\":1,\"y\":2}");
		byte[] header = ("AM," + am.channel("board")).getBytes(StandardCharsets.UTF_8);
		ByteBuffer binary = ByteBuffer.allocate(1 + header.length + 2);
		binary.put((byte) header.length).put(header).put((byte) 9).put((byte) 8).flip();
		client.send(binary);
		client.send(ByteBuffer.wrap(new byte[] {(byte) 200, 1, 2}));

		assertTrue(am.received.await(5, TimeUnit.SECONDS));
		assertEquals("game", am.templateId);
		assertEquals(1, am.message.getInt("x"));
		assertEquals(2, am.message.getInt("y"));
		assertEquals("board", am.binaryTemplateId);
		assertEquals("9,8", am.binary[0] + "," + am.binary[1]);
		assertEquals(2, am.count.get(), "the message that is not JSON was dropped");
		assertTrue(connectsAndStaysOpen(), "a malformed message does not close the connection");
		client.closeBlocking();
	}

	public static class RecordingAM extends AM
	{
		final CountDownLatch received = new CountDownLatch(2);
		final AtomicInteger count = new AtomicInteger();
		volatile String templateId;
		volatile J message;
		volatile String binaryTemplateId;
		volatile byte[] binary;

		@Override
		public byte[] receiveChannelMessage(J in, int applicationId, String templateId, ViewRow row)
		{
			this.message = in;
			this.templateId = templateId;
			count.incrementAndGet();
			received.countDown();
			return null;
		}

		@Override
		public byte[] receiveChannelBinaryMessage(byte[] message, int applicationId, String templateId, ViewRow row)
		{
			this.binary = message;
			this.binaryTemplateId = templateId;
			count.incrementAndGet();
			received.countDown();
			return null;
		}
	}

	@Test
	void aMessageOverTheLimitClosesOnlyThatConnection() throws Exception
	{
		Client big = new Client("PHPSESSID=" + ID);
		assertTrue(big.connectBlocking(5, TimeUnit.SECONDS));
		big.send("PING,1," + "x".repeat(LIMIT));
		assertTrue(big.closed.await(5, TimeUnit.SECONDS));
		assertEquals(1009, big.closeCode);

		assertTrue(connectsAndStaysOpen(), "the server still accepts browsers");
	}

	@Test
	void aFrameHeaderClaiming2GbDoesNotStopTheServer() throws Exception
	{
		try (Socket raw = new Socket("127.0.0.1", server.getPort()))
		{
			raw.setSoTimeout(5000);
			OutputStream out = raw.getOutputStream();
			out.write(("GET /SessionSocket HTTP/1.1\r\nHost: 127.0.0.1\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n"
				+ "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\nSec-WebSocket-Version: 13\r\nCookie: PHPSESSID=" + ID + "\r\n\r\n")
				.getBytes(StandardCharsets.US_ASCII));
			out.flush();
			InputStream in = raw.getInputStream();
			String response = readHeaders(in);
			assertTrue(response.startsWith("HTTP/1.1 101"), response);
			// binary frame, masked, 64-bit length 0x7FFFFFFF, mask key; no payload follows
			out.write(new byte[] {(byte) 0x82, (byte) 0xFF, 0, 0, 0, 0, 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 1, 2, 3, 4});
			out.flush();
			byte[] buffer = new byte[256];
			long deadline = System.currentTimeMillis() + 5000;
			int read = 0;
			while (read >= 0 && System.currentTimeMillis() < deadline)
			{
				read = in.read(buffer);
			}
			assertEquals(-1, read, "the server closes that connection");
		}
		assertTrue(connectsAndStaysOpen(), "the server still accepts browsers");
	}

	private boolean connectsAndStaysOpen() throws Exception
	{
		Client next = new Client("PHPSESSID=" + ID);
		boolean connected = next.connectBlocking(5, TimeUnit.SECONDS);
		boolean stayedOpen = !next.closed.await(300, TimeUnit.MILLISECONDS);
		next.closeBlocking();
		return connected && stayedOpen;
	}

	private static String readHeaders(InputStream in) throws Exception
	{
		StringBuilder sb = new StringBuilder();
		while (!sb.toString().endsWith("\r\n\r\n"))
		{
			int b = in.read();
			if (b < 0) break;
			sb.append((char) b);
		}
		return sb.toString();
	}

	private class Client extends WebSocketClient
	{
		final CountDownLatch closed = new CountDownLatch(1);
		volatile String lastText;
		volatile int closeCode;

		Client(String cookie) throws Exception
		{
			super(new URI("ws://127.0.0.1:" + server.getPort() + "/SessionSocket"),
				cookie == null ? Map.of() : Map.of("Cookie", cookie));
		}

		public void onOpen(ServerHandshake handshake)
		{
		}

		public void onMessage(String message)
		{
			lastText = message;
		}

		public void onMessage(ByteBuffer bytes)
		{
		}

		public void onClose(int code, String reason, boolean remote)
		{
			closeCode = code;
			closed.countDown();
		}

		public void onError(Exception ex)
		{
		}
	}
}
