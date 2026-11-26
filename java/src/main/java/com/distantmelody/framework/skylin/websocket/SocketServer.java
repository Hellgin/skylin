package com.distantmelody.framework.skylin.websocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.extensions.IExtension;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.distantmelody.framework.skylin.AM;
import com.distantmelody.framework.skylin.bridge.BridgeSession;
import com.distantmelody.framework.skylin.util.Util;
import com.distantmelody.library.json.J;

public class SocketServer extends WebSocketServer{

	private static final Logger LOG = LoggerFactory.getLogger(SocketServer.class);

	/** The largest message a browser may send, in bytes, unless FrameworkParams sets WEB_SOCKET_MAX_MESSAGE_SIZE. */
	public static final int DEFAULT_MAX_MESSAGE_SIZE = 1024 * 1024;
	
	ConcurrentHashMap<WebSocket,SessionSocket> sockets = new ConcurrentHashMap<WebSocket,SessionSocket>();

	public SocketServer( int port,int decoders ) throws UnknownHostException {
		this(port, decoders, maxMessageSize(Util.getFrameworkParam("WEB_SOCKET_MAX_MESSAGE_SIZE")));
	}

	/**
	 * A frame, or a message sent in several frames, over maxMessageSize bytes closes that connection (1009). Without a
	 * limit the library reserves whatever size a frame header claims, so one header claiming 2 GB stopped the server.
	 */
	SocketServer(int port, int decoders, int maxMessageSize) {
		super(new InetSocketAddress(port), decoders,
			Collections.<Draft>singletonList(new Draft_6455(Collections.<IExtension>emptyList(), maxMessageSize)));
	}

	/** The WEB_SOCKET_MAX_MESSAGE_SIZE value in bytes; the default when it is not set or not a positive whole number. */
	static int maxMessageSize(String param) {
		if (param == null || param.isBlank()) return DEFAULT_MAX_MESSAGE_SIZE;
		try {
			int size = Integer.parseInt(param.trim());
			if (size > 0) return size;
		}
		catch (NumberFormatException e) {
		}
		LOG.warn("WEB_SOCKET_MAX_MESSAGE_SIZE '{}' is not a positive number of bytes; using {}", param, Integer.valueOf(DEFAULT_MAX_MESSAGE_SIZE));
		return DEFAULT_MAX_MESSAGE_SIZE;
	}
	
	/**
	 * Only a browser with a session this JVM holds may stay connected. Any other is told to reload, as before, and
	 * closed before it can send anything; after a JVM restart that reload gives the page a new session.
	 */
    public void onOpen(WebSocket session, ClientHandshake handshake) {
		String id = sessionCookie(handshake.getFieldValue("Cookie"));
		if (id == null || !BridgeSession.sessions.containsKey(id))
		{
			forceRefresh(session);
		}
    }

	/** The PHPSESSID value of a Cookie header, or null. */
	static String sessionCookie(String header) {
		if (header == null) return null;
		for (String cookie : header.split(";")) {
			String c = cookie.trim();
			if (c.startsWith("PHPSESSID=")) {
				String id = c.substring("PHPSESSID=".length());
				return BridgeSession.isValidId(id) ? id : null;
			}
		}
		return null;
	}

    public void onMessage(WebSocket websocketSession,ByteBuffer messageBuffer) {
    	//System.out.println("received bytes: " + new String(message.array()));
    	byte[] data = messageBuffer.array();
    	int headerLength = data[0] & 0xFF;//a length of 128 and more is a negative byte
    	byte[] header = new byte[headerLength];
    	byte[] message = new byte[data.length-headerLength-1];
    	System.arraycopy(data, 1, header, 0, headerLength);
    	System.arraycopy(data, 1+headerLength, message, 0, message.length);
    	
    	/*
    	System.out.println("header " + new String(header, StandardCharsets.UTF_8));
    	for (int i = 0; i < message.length;i++)
    	{
    		System.out.println("message " +i+": " + message[i]);
    	}*/
    	
    	String[] m = new String(header, StandardCharsets.UTF_8).split(",", 5);
    	
   		BridgeSession bs = BridgeSession.sessions.get(m[2]);
		if (bs == null)
		{
			//System.out.println("Bridge Session not found. forcing client refresh");
			forceRefresh(websocketSession);
			return;
		}
    	
		if ("AM".equals(m[0]))
		{
			AM am = (AM) bs.getObject(Integer.parseInt(m[3]));
			if (am == null)
			{
				LOG.warn("session " + m[2] + " sent message to non existing am. telling client to refresh now.");
				forceRefresh(websocketSession);
				return;
			}
			byte[] response = am.receiveBinaryMessage(message,m[4]);
			if (response != null)
			{
				//consider being able to send response back like this. Would likely never be needed.
			}
		}
    }

    public void onMessage(WebSocket websocketSession,String message) {
    		String[] m = message.split(",", 6);
    		
       		BridgeSession bs = BridgeSession.sessions.get(m[2]);
    		if (bs == null)
    		{
    			//System.out.println("Bridge Session not found. forcing client refresh");
    			forceRefresh(websocketSession);
    			return;
    		}
    		
    		if ("AM".equals(m[0]))
    		{
    			AM am = (AM) bs.getObject(Integer.parseInt(m[3]));
    			if (am == null)
    			{
    				LOG.warn("session " + m[2] + " sent message to non existing am. telling client to refresh now.");
    				forceRefresh(websocketSession);
    				return;
    			}
    			byte[] response = am.receiveMessage(J.fromString(m[5]),m[4]);
    			if (response != null)
    			{
    				//consider being able to send response back like this. Would likely never be needed.
    			}
    		}
    		else if ("CONNECTED".equals(m[0]))
    		{
    			if (!sockets.containsKey(websocketSession))
    			{
	        		SessionSocket ss = new SessionSocket();
	        		ss.session = bs;
	            	//ss.session.setSessionSocket(ss);
	        		long appId = Long.parseLong(m[1]);
	        		ss.session.addSessionSocket(appId,ss);
	            	ss.websocketSession = websocketSession;
	            	ss.appId = appId;
	            	sockets.put(websocketSession, ss);
	        		//System.out.println("session id: " + ss.session.getId());
    			}
    		}
    		else if ("PING".equals(m[0]))
    		{
    			for (SessionSocket ss:bs.getSessionSockets(Long.parseLong(m[1])))
    			{
    				if (ss.websocketSession == websocketSession)
    				{
    					ss.pingReturned();
    				}
    			}
    		}
    }


    public void onClose(WebSocket session,int arg1, String arg2, boolean arg3) {
    	SessionSocket socket = sockets.remove(session);
    	if (socket != null)
    	{
    		socket.closed = true;
    		socket.detach();
    	}
    }


    public void onError(WebSocket session,Exception ex) {
    	ex.printStackTrace();
    }

	/** The browser is already gone, so telling it to refresh cannot succeed and is not a server error. */
	private void forceRefresh(WebSocket websocketSession)
	{
		try
		{
			websocketSession.send("FORCE_REFRESH");
			websocketSession.close();
		}
		catch (WebsocketNotConnectedException e)
		{
		}
	}
    
    public boolean isClosed()
    {
    	return true;
    }

	@Override
	public void onStart() {
	
	}

	public void closeAllConnections() {
		for (WebSocket s:sockets.keySet())
		{
			try{s.close();}catch (Exception e) {};
		}
	}
    



}