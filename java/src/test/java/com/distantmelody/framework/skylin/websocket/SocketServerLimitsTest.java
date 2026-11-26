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

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.distantmelody.framework.skylin.bridge.BridgeSession;

/**
 * The browser socket refuses messages over the size limit, survives a frame header that claims 2 GB, and keeps only
 * browsers with a session this JVM holds.
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
	void aMessageOverTheLimitClosesOnlyThatConnection() throws Exception
	{
		Client big = new Client("PHPSESSID=" + ID);
		assertTrue(big.connectBlocking(5, TimeUnit.SECONDS));
		big.send("PING,1," + ID + "," + "x".repeat(LIMIT));
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
