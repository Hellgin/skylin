package com.distantmelody.framework.skylin.bridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;

import com.distantmelody.framework.skylin.server.Server;
import com.distantmelody.framework.skylin.server.SkylinServletConfig;
import com.distantmelody.framework.skylin.services.ServiceContainer;
import com.distantmelody.framework.skylin.services.SkylinHttpServletRequest;
import com.distantmelody.framework.skylin.services.SkylinHttpServletResponse;
import com.distantmelody.framework.skylin.services.SkylinServletInputStream;
import com.distantmelody.framework.skylin.services.SkylinServletOutputStream;

/**
 * One connection of the PHP bridge: reads the commands PHP sends, runs them and writes the replies.
 * <p>
 * Wire format (also described in docs/architecture.md). Everything is BYTES, text in it is UTF-8:
 * <ul>
 * <li>request: 7 ASCII decimal digits {@code N}, then {@code N} bytes of {@code <command>[,<data>]}</li>
 * <li>reply: 7 ASCII hex digits {@code M}, then {@code M} bytes: one type character ({@code 3}: the reply follows
 * inline, {@code 4}: the reply is in the file whose path follows) and the reply</li>
 * </ul>
 * A frame is read as bytes and parsed as bytes ({@link ByteReader}); no text is decoded before the fields have been
 * cut out, because every length in the protocol counts the bytes of the UTF-8 encoding (PHP's strlen).
 */
public class BridgeConnection implements Runnable{

	Thread thread;
	Socket socket;
	InputStream is;
	OutputStream os;
	ServletContext context;
	boolean keepAlive = true;
	BridgeSession session;
	private static long nextNo;
	
	/** A connection that has been idle for this long is closed. */
	static final int IDLE_TIMEOUT_MILLIS = 10000;
	private final int idleTimeoutMillis;
	/** Replies bigger than this are handed over in a file, if UPLOAD_TMP is configured. */
	static final int MAX_INLINE_REPLY = 1048576;
	
	private static final int sizeLength = 7;
	private static final byte[] EMPTY = new byte[0];
	private static final byte[] ENCODED_QUESTION_MARK = "%3F".getBytes(StandardCharsets.US_ASCII);
	public HashMap<String,String> cookies = new HashMap<String,String>();
	
	
	private static ThreadLocal<Boolean> isBridgeConnection = new ThreadLocal<Boolean>();
	public static boolean isBridgeConnection()
	{
		return isBridgeConnection.get() != null && isBridgeConnection.get();
	}
	
	private static ThreadLocal<Long> no = new ThreadLocal<Long>();
	public static Long getNo()
	{
		Long number = no.get();
		if (number == null)
		{
			return -1l;
		}
		return number;
	}
	
	private static ThreadLocal<BridgeConnection> bridgeConnection = new ThreadLocal<BridgeConnection>();
	public static BridgeConnection getBridgeConnection()
	{
		return bridgeConnection.get();
	}
	
	private static ThreadLocal<Boolean> sameRequestEvalSupported = new ThreadLocal<Boolean>();
	public static Boolean isSameRequestEvalSupported()
	{
		return sameRequestEvalSupported.get() != null && sameRequestEvalSupported.get();
	}
	
	public BridgeConnection(Socket clientSocket, ServletContext context) 
	{
		this(clientSocket, context, IDLE_TIMEOUT_MILLIS);
	}

	BridgeConnection(Socket clientSocket, ServletContext context, int idleTimeoutMillis) 
	{
		socket = clientSocket;
		this.context = context;
		this.idleTimeoutMillis = idleTimeoutMillis;
		try
		{
			socket.setSoTimeout(this.idleTimeoutMillis);
			is = socket.getInputStream();
			os = socket.getOutputStream();
			thread = new Thread(this);
			thread.start();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void run() 
	{
		isBridgeConnection.set(true);
		no.set(nextNo);
		nextNo++;
		bridgeConnection.set(this);
		while (keepAlive)
		{
			try
			{
				byte[] frame = readFrame();
				if (frame == null)
				{
					break;
				}
				byte[] ret = process(frame);
				send(ret);
			}
			catch (SocketTimeoutException e)
			{
				keepAlive = false;//idle for too long
			}
			catch (SocketException e)
			{
				keepAlive = false;
				e.printStackTrace();
			}
			catch (BadFrameException e)
			{
				keepAlive = false;//the stream cannot be trusted any more
				System.out.println("Bridge connection closed: " + e.getMessage());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		try 
		{
			socket.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	/**
	 * Reads the next request: the 7 digit size and then exactly that many bytes. Blocks until they have arrived, so
	 * a frame that comes in in any number of pieces (down to single bytes) is the same as one that comes in whole,
	 * and the bytes after a frame stay in the stream for the next call.
	 *
	 * @return the frame without its size, or null when the peer has closed the connection
	 */
	private byte[] readFrame() throws IOException
	{
		byte[] sizeDigits = is.readNBytes(sizeLength);
		if (sizeDigits.length == 0)
		{
			return null;
		}
		if (sizeDigits.length < sizeLength)
		{
			throw new BadFrameException("connection closed in the middle of a frame size");
		}
		int size = 0;
		for (byte digit:sizeDigits)
		{
			if (digit < '0' || digit > '9')
			{
				throw new BadFrameException("frame size is not 7 decimal digits: " + new String(sizeDigits, StandardCharsets.ISO_8859_1));
			}
			size = size * 10 + (digit - '0');
		}
		byte[] frame = is.readNBytes(size);
		if (frame.length < size)
		{
			throw new BadFrameException("connection closed in the middle of a frame: got " + frame.length + " of " + size + " bytes");
		}
		return frame;
	}

	private static class BadFrameException extends IOException
	{
		BadFrameException(String message)
		{
			super(message);
		}
	}

	private static String dot = Pattern.quote(".");
	/**
	 * Sends a reply: 7 hex digits with the number of bytes that follow, the type character, and the reply as it is
	 * (bytes). A big reply is written to a file in UPLOAD_TMP and its path is sent instead (type 4).
	 */
	public void send(byte[] ret) throws IOException {
		
		byte type = '3';
		
		if (ret.length > MAX_INLINE_REPLY && Server.frameworkParams.containsKey("UPLOAD_TMP"))
		{
			String filepath = Server.frameworkParams.get("UPLOAD_TMP")+"/big"+(Math.random()+"").replaceAll(dot, "");//maybe generate a better random name lol.
			//System.out.println(filepath + " " + ret.length);
			Files.write(Path.of(filepath), ret);
			ret = filepath.getBytes(StandardCharsets.UTF_8);
			type = '4';
		}
		
		String length = Integer.toHexString(ret.length+1);
		if (length.length() > sizeLength)
		{
			throw new IOException("reply of " + ret.length + " bytes does not fit in the " + sizeLength + " digit size field");
		}
		byte[] toSend = new byte[sizeLength + 1 + ret.length];
		int padding = sizeLength - length.length();
		for (int i = 0; i < padding; i++)
		{
			toSend[i] = '0';
		}
		for (int i = 0; i < length.length(); i++)
		{
			toSend[padding + i] = (byte) length.charAt(i);
		}
		toSend[sizeLength] = type;
		System.arraycopy(ret, 0, toSend, sizeLength + 1, ret.length);
		os.write(toSend);//writing the byte arrays seperately to the outputstream causes severe slowdown on linux
	}
	
	/**
	 * Sends a frame to PHP in the middle of a call (the backendEval callback, command 1 of the reply). PHP does not answer
	 * on this socket: it runs the code in a new connection and goes on waiting for the reply of the call in progress.
	 *
	 * @return always null
	 */
	public String sendAndReceive(byte[] bytes) throws IOException {
		send(bytes);
		return null;
	}

	private static byte[] utf8(String text)
	{
		return text.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Runs one command. {@code frame} is the request without its size: the command, a comma and the data. The
	 * data stays bytes until a command needs one of its fields as text.
	 */
	byte[] process(byte[] frame) 
	{	
		ByteReader in = new ByteReader(frame);
		String command = in.token(',');
		if ("9".equals(command))
		{
			//<http method>,<authorization>,<service>[?<body>]; the body is bytes of any kind and goes on as they are
			int separator = in.find("?".getBytes(StandardCharsets.US_ASCII));
			int separatorLength = 1;
			if (separator < 0)
			{
				separator = in.find(ENCODED_QUESTION_MARK);
				separatorLength = ENCODED_QUESTION_MARK.length;
			}
			String head;
			byte[] body;
			if (separator >= 0)
			{
				head = in.slice(separator).string();
				in.skip(separatorLength);
				body = in.bytes();
			}
			else
			{
				head = in.string();
				body = EMPTY;
			}
			String[] t2 = head.split(",");
			String httpMethod = t2[0];
			String auth = t2[1];//very temp, extend to all headers
			String service = t2[2];
			//return body;
			try
			{
				HttpServlet servlet = new ServiceContainer().load(service);
				SkylinServletConfig config = new SkylinServletConfig();
				config.setServletContext(context);
				config.setServletName(service);
				servlet.init(config);
				SkylinHttpServletRequest req = new SkylinHttpServletRequest();
				if (session != null)req.setAttribute("BRIDGE_SESSION", session);
				req.setMethod(httpMethod);
				req.setAuth_tempMethodPleaseExandToAllHeaders(auth);
				req.setInputStream(new SkylinServletInputStream(body));
				req.setServletContext(context);
				SkylinHttpServletResponse res = new SkylinHttpServletResponse();
				servlet.service(req, res);
				
				byte[] responseData = ((SkylinServletOutputStream)res.getOutputStream()).getData();
				byte[] h = res.getPackedHeaderData();
				byte[] ret = new byte[responseData.length+h.length];
				System.arraycopy(h, 0, ret, 0, h.length);
				System.arraycopy(responseData, 0, ret, h.length, responseData.length);
				
				return ret;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else if ("0".equals(command))
		{
			return utf8(session.instantiateByName(in.string()));
		}
		else if ("1".equals(command))
		{
			return utf8(session.instantiateByClass(in.string()));
		}
		else if ("2".equals(command))
		{
			sameRequestEvalSupported.set(true);
			return new ByteWriter().ascii("0").bytes(session.instanceMethod(in)).toByteArray();
		}
		else if ("3".equals(command))
		{
			return utf8(session.getClassInstance(in.string()));
		}
		else if ("4".equals(command))
		{
			return utf8(session.getServletContext(context));
		}
		else if ("5".equals(command))
		{
			session.releaseObjects(in.string());
		}
		else if ("6".equals(command))
		{
			if (session != null)session.lastActivity = System.currentTimeMillis();
			keepAlive = false;
		}
		else if ("7".equals(command))
		{
			//disabled due to changes to websockets. Rethink this when needed again.
			//<application id>,<the text for the browser, UTF-8>
			long appId = Long.parseLong(in.token(','));
			session.sendWebSocketText(appId,in.string());
		}
		else if ("8".equals(command))
		{
			String id = in.string();
			session = BridgeSession.sessions.get(id);
			if (session == null)
			{
				session = new BridgeSession(id,context);
				BridgeSession.sessions.put(id, session);
				return utf8("1");
			}
			else
			{
				return utf8("0");
			}
		}
		else if ("a".equals(command))
		{
			return utf8(session.getBridgeSession());
		}
		else if ("b".equals(command))
		{
			return Server.bridgeSessionHandler.process(in);
		}
		else if ("c".equals(command))
		{
			System.out.println(in.string());
		}
		else if ("d".equals(command))
		{
			String s[] = in.string().split(";",2);
			cookies.put(s[0], s[1]);
		}
		else if ("e".equals(command))
		{
			//PHP wrote the whole frame (without size) of a request bigger than 1 MB to a file
			try
			{
				return process(Files.readAllBytes(Path.of(in.string())));
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		else if ("f".equals(command))
		{
			if (session.isWebsocketAvailable(Long.parseLong(in.string())))
			{
				return utf8("true");
			}
			return utf8("false");
		}
		return EMPTY;
	}

}
