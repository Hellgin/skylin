package com.distantmelody.framework.skylin.bridge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.java_websocket.WebSocket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.distantmelody.framework.skylin.server.Server;
import com.distantmelody.framework.skylin.websocket.SessionSocket;

/**
 * Round trip tests of the PHP bridge protocol against a real {@link BridgeConnection} on a real socket, with
 * {@link PhpWire} playing PHP. The subclasses only choose the transport (TCP or the Unix domain socket of production).
 */
abstract class BridgeProtocolTestBase
{
	static final String SESSION = "unit-test-session";
	static final String E_ACUTE = "\u00e9";
	static final String EURO = "\u20ac";
	static final String JAPANESE = "\u65e5\u672c\u8a9e";
	static final String EMOJI = "\uD83D\uDE00";
	/** 1, 2, 3 and 4 bytes per character, with ASCII around them. */
	static final String MIXED = "a" + E_ACUTE + EURO + EMOJI + "b";

	@TempDir
	Path tmp;
	Socket client;
	InputStream in;
	OutputStream out;
	BridgeConnection connection;

	/** @return {server side, client side} of one connected socket pair */
	abstract Socket[] connectedPair(Path dir) throws IOException;

	record Reply(char type, byte[] payload) {}

	@BeforeEach
	void connect() throws Exception
	{
		Server.frameworkParams.put("UPLOAD_TMP", tmp.toString());
		Server.bridgeSessionHandler = new BridgeSessionHandler();
		Socket[] pair = connectedPair(tmp);
		connection = new BridgeConnection(pair[0], null, 5000);
		client = pair[1];
		client.setSoTimeout(15000);
		in = client.getInputStream();
		out = client.getOutputStream();
		assertEquals("1", text(call(PhpWire.utf8("8," + SESSION))), "new session");
		BridgeSession.sessions.get(SESSION).objects.put(1, new EchoTarget());
	}

	@AfterEach
	void disconnect() throws IOException
	{
		client.close();
		BridgeSession.sessions.remove(SESSION);
		Server.frameworkParams.remove("UPLOAD_TMP");
	}

	// ---------------------------------------------------------------- the PHP side of the socket

	static String text(byte[] b)
	{
		return new String(b, StandardCharsets.UTF_8);
	}

	void send(byte[] bytes) throws IOException
	{
		out.write(bytes);
		out.flush();
	}

	/** BridgeConnection.php, reading one reply: 7 hex digits, type, payload; type 4 is a file that is read and removed. */
	Reply readReply() throws IOException
	{
		int size = Integer.parseInt(new String(in.readNBytes(7), StandardCharsets.US_ASCII), 16);
		byte[] body = in.readNBytes(size);
		assertEquals(size, body.length, "reply must be complete");
		char type = (char) body[0];
		byte[] payload = Arrays.copyOfRange(body, 1, body.length);
		if (type == '4')
		{
			Path file = Path.of(text(payload));
			payload = Files.readAllBytes(file);
			Files.delete(file);
		}
		return new Reply(type, payload);
	}

	/** Sends one command (without size) the way PHP does and returns its reply. */
	byte[] call(byte[] payload) throws IOException
	{
		send(PhpWire.frame(payload));
		return readReply().payload();
	}

	/** java.php __call(): calls a method of the target and decodes the result like _stringToObject(). */
	Object invoke(String method, Object... args) throws IOException
	{
		return decodeCall(call(PhpWire.call(1, method, args)));
	}

	/** The reply of a method call ('0' and the encoded result) decoded like _stringToObject(). */
	static Object decodeCall(byte[] reply)
	{
		assertEquals('0', (char) reply[0], "end of a multi call");
		return PhpWire.decode(Arrays.copyOfRange(reply, 1, reply.length));
	}

	// ---------------------------------------------------------------- strings

	@Test
	void stringsOfEveryUtf8Length() throws Exception
	{
		for (String s:List.of("plain", E_ACUTE, EURO, JAPANESE, EMOJI, MIXED, EMOJI + EMOJI + EMOJI))
		{
			assertEquals(s, invoke("echo", s), s);
		}
	}

	@Test
	void emptyStringsAndNull() throws Exception
	{
		assertEquals("", invoke("echo", ""));
		assertEquals(null, invoke("nothing"));
		assertEquals("||", invoke("concat3", "", "", ""));
		assertEquals(E_ACUTE + "||" + EMOJI, invoke("concat3", E_ACUTE, "", EMOJI));
	}

	@Test
	void stringsWithCommasAndDigits() throws Exception
	{
		for (String s:List.of(",", ",,,", "3,5,s7,a2,1,1,x", "0000012,1,2", E_ACUTE + ",12," + EURO, "s,7," + JAPANESE + ",7"))
		{
			assertEquals(s, invoke("echo", s), s);
		}
		assertEquals("1,2|" + EMOJI + ",3|" + EURO + "4", invoke("concat3", "1,2", EMOJI + ",3", EURO + "4"));
	}

	@Test
	void argumentsAreCutByBytesNotByCharacters() throws Exception
	{
		//the three lengths are 2, 4 and 6 bytes; cut by characters the second and third would be wrong
		assertEquals(E_ACUTE + "|" + EMOJI + "|" + JAPANESE.substring(0, 2), invoke("concat3", E_ACUTE, EMOJI, JAPANESE.substring(0, 2)));
		assertEquals(Integer.valueOf(MIXED.length()), invoke("chars", MIXED));
		assertEquals(Integer.valueOf(5), invoke("codePoints", MIXED));
		assertEquals(Integer.valueOf(1 + 2 + 3 + 4 + 1), invoke("utf8Length", MIXED));
	}

	@Test
	void invalidUtf8InAnArgumentDoesNotShiftTheOthers() throws Exception
	{
		byte[] notText = {(byte) 0xFF, (byte) 0xFE};
		assertEquals(E_ACUTE + "|\uFFFD\uFFFD|" + EURO, invoke("concat3", E_ACUTE, notText, EURO));
	}

	// ---------------------------------------------------------------- arrays

	@Test
	void arraysOfMixedStrings() throws Exception
	{
		List<Object> a = new ArrayList<Object>(List.of("ascii", E_ACUTE, EURO, JAPANESE, EMOJI, "", "a,1,b", MIXED));
		assertEquals(a, invoke("echoArray", (Object) a.toArray()));
		assertEquals(List.of(), invoke("echoArray", (Object) new Object[0]));
		assertEquals(List.of(""), invoke("echoArray", (Object) new Object[] {""}));
	}

	@Test
	void arrayArgumentFollowedByStringArgument() throws Exception
	{
		//the array is one argument whose own element lengths are byte lengths, and strings follow it
		Object[] array = {EMOJI, JAPANESE, "", E_ACUTE};
		assertEquals(EMOJI + "+" + JAPANESE + "++" + E_ACUTE + "/" + EURO + "/" + MIXED, invoke("describe", array, EURO, MIXED));
	}

	@Test
	void returnedStringsAndArrays() throws Exception
	{
		assertEquals(List.of("a", E_ACUTE, EURO, JAPANESE, EMOJI, "", "a,1,b", "3,5,s7"), invoke("samples"));
		assertEquals(Arrays.asList(E_ACUTE, List.of(JAPANESE, "x"), 5, true, ""), invoke("nested"));
	}

	@Test
	void everyReplyLengthIsInBytes() throws Exception
	{
		byte[] reply = call(PhpWire.call(1, "echo", EMOJI));
		assertEquals(1 + 1 + 4, reply.length, "0 + s + 4 bytes");
		reply = call(PhpWire.call(1, "samples"));
		assertEquals('0', (char) reply[0]);
		//the sizes in the array header are what a PHP substr() needs
		assertEquals(Arrays.asList("a", E_ACUTE, EURO, JAPANESE, EMOJI, "", "a,1,b", "3,5,s7"), decodeCall(reply));
	}

	// ---------------------------------------------------------------- framing

	@Test
	void frameInSingleByteWrites() throws Exception
	{
		byte[] frame = PhpWire.frame(PhpWire.call(1, "concat3", MIXED, E_ACUTE, EURO + EMOJI));
		for (byte b:frame)
		{
			send(new byte[] {b});
			Thread.sleep(1);
		}
		assertEquals(MIXED + "|" + E_ACUTE + "|" + EURO + EMOJI, decodeCall(readReply().payload()));
	}

	@Test
	void twoFramesInOneWrite() throws Exception
	{
		byte[] both = PhpWire.concat(PhpWire.frame(PhpWire.call(1, "echo", E_ACUTE + EURO)), PhpWire.frame(PhpWire.call(1, "echo", JAPANESE + EMOJI)));
		send(both);
		assertEquals(E_ACUTE + EURO, decodeCall(readReply().payload()));
		assertEquals(JAPANESE + EMOJI, decodeCall(readReply().payload()));
	}

	@Test
	void frameAndAHalfThenTheRest() throws Exception
	{
		byte[] first = PhpWire.frame(PhpWire.call(1, "echo", EMOJI + EURO));
		byte[] second = PhpWire.frame(PhpWire.call(1, "echo", JAPANESE));
		byte[] all = PhpWire.concat(first, second);
		int cut = first.length + 3 + 7 + 2;//in the middle of the second frame, inside its first multi byte character
		send(Arrays.copyOfRange(all, 0, cut));
		assertEquals(EMOJI + EURO, decodeCall(readReply().payload()));
		Thread.sleep(100);
		send(Arrays.copyOfRange(all, cut, all.length));
		assertEquals(JAPANESE, decodeCall(readReply().payload()));
	}

	@Test
	void frameOverOneMegabyteGoesThroughAFileAndSoDoesTheReply() throws Exception
	{
		String big = (EURO + "abc").repeat(300_000) + EMOJI + JAPANESE;//1.2 million characters, 2.4 million bytes
		byte[] payload = PhpWire.call(1, "echo", big);
		assertTrue(payload.length > 1048576);
		//BridgeConnection.php: the payload is written to a temp file and 'e,<path>' is sent instead
		Path file = Files.createTempFile(tmp, "big", "");
		Files.write(file, payload);
		send(PhpWire.frame(PhpWire.utf8("e," + file)));
		Reply reply = readReply();
		assertEquals('4', reply.type(), "a reply over 1 MB is handed over as a file");
		assertEquals('0', (char) reply.payload()[0]);
		assertEquals(big, decodeCall(reply.payload()));
		Files.delete(file);
		try (var left = Files.list(tmp))
		{
			assertEquals(0, left.count(), "the reply file was removed by the reader");
		}
	}

	@Test
	void bigReplyWithoutBigRequest() throws Exception
	{
		Object s = invoke("big", 400_000);//1.2 MB of 3 byte characters
		assertEquals(EURO.repeat(400_000), s);
	}

	@Test
	void smallReplyIsInline() throws Exception
	{
		send(PhpWire.frame(PhpWire.call(1, "echo", MIXED)));
		assertEquals('3', readReply().type());
	}

	@Test
	void garbageInsteadOfASizeClosesTheConnection() throws Exception
	{
		send(PhpWire.utf8("abcdefgXYZ"));
		try
		{
			assertEquals(-1, in.read(), "the server closes a stream it cannot parse");
		}
		catch (IOException reset)
		{
			//closing with bytes left unread may reset a TCP connection instead of ending it: also closed
		}
	}

	@Test
	void idleConnectionIsClosed() throws Exception
	{
		Socket[] pair = connectedPair(tmp);
		new BridgeConnection(pair[0], null, 300);
		long start = System.currentTimeMillis();
		assertEquals(-1, pair[1].getInputStream().read());
		assertTrue(System.currentTimeMillis() - start < 5000);
		pair[1].close();
	}

	@Test
	void peerClosingEndsTheConnectionAtOnce() throws Exception
	{
		Socket[] pair = connectedPair(tmp);
		new BridgeConnection(pair[0], null, 60000);
		pair[1].close();
		Thread.sleep(300);
		assertTrue(pair[0].isClosed(), "EOF is noticed without waiting for the idle timeout");
	}

	// ---------------------------------------------------------------- the other commands

	@Test
	void sessionHandlerStoresBytesExactly() throws Exception
	{
		byte[] all = new byte[512];
		for (int i = 0; i < all.length; i++)
		{
			all[i] = (byte) i;//every byte value twice, including 0, ',' and all invalid UTF-8 sequences
		}
		byte[] php = PhpWire.concat(PhpWire.utf8("a:2:{s:1:\"x\";s:" + (all.length + 5) + ":\""), all, PhpWire.utf8(",\u00e9\u20ac\";s:1:\"y\";i:1;}"));
		for (byte[] data:List.of(all, php, new byte[0], PhpWire.utf8(MIXED), new byte[] {(byte) 0xC3}, new byte[] {(byte) 0xF0, (byte) 0x9F}))
		{
			byte[] open = call(PhpWire.concat(PhpWire.utf8("b,1," + tmp + ",binsess,PHPSESSID")));
			assertEquals("true", text(open));
			assertEquals("true", text(call(PhpWire.concat(PhpWire.utf8("b,3,binsess,"), data))));
			assertArrayEquals(data, Files.readAllBytes(tmp.resolve("sess1_binsess")), "file content");
			assertArrayEquals(data, call(PhpWire.utf8("b,2,binsess")), "read back");
			assertEquals("true", text(call(PhpWire.utf8("b,4,binsess"))));
		}
		assertEquals("true", text(call(PhpWire.utf8("b,5,binsess"))));
		assertTrue(Files.notExists(tmp.resolve("sess1_binsess")));
	}

	@Test
	void cookiesAreDecodedAsUtf8() throws Exception
	{
		call(PhpWire.utf8("d,name;" + MIXED + ";x,y"));
		assertEquals(MIXED + ";x,y", connection.cookies.get("name"));
	}

	@Test
	void websocketTextIsDecodedAsUtf8() throws Exception
	{
		List<String> sent = new ArrayList<String>();
		SessionSocket socket = new SessionSocket();
		socket.websocketSession = (WebSocket) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {WebSocket.class}, (proxy, method, args) ->
		{
			if (method.getName().equals("send") && args[0] instanceof String s)
			{
				sent.add(s);
			}
			return null;
		});
		BridgeSession.sessions.get(SESSION).addSessionSocket(7, socket);
		String message = "1,12-setattr,x," + MIXED + "," + JAPANESE;
		call(PhpWire.utf8("7,7," + message));
		assertEquals(List.of(message), sent);
	}

	@Test
	void callbackToPhpCarriesUtf8Code() throws Exception
	{
		String code = "return '" + MIXED + JAPANESE + "';";
		send(PhpWire.frame(PhpWire.call(1, "callback", code)));
		byte[] first = readReply().payload();
		assertEquals('1', (char) first[0], "the callback comes first");
		assertEquals("42," + code, text(Arrays.copyOfRange(first, 1, first.length)));
		byte[] last = readReply().payload();
		assertEquals('0', (char) last[0]);
		assertEquals("done", decodeCall(last));
	}
}
