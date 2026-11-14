package com.distantmelody.framework.skylin.websocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.HashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.distantmelody.framework.skylin.AM;
import com.distantmelody.framework.skylin.bridge.BridgeSession;
import com.distantmelody.library.json.J;

public class SocketServer extends WebSocketServer{
	
	HashMap<WebSocket,SessionSocket> sockets = new HashMap<WebSocket,SessionSocket>();

	public SocketServer( int port,int decoders ) throws UnknownHostException {
		super( new InetSocketAddress( port ) ,decoders);
	}
	
    public void onOpen(WebSocket session, ClientHandshake handshake) {
		//System.out.println( session.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!" );
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
			websocketSession.send("FORCE_REFRESH");
			websocketSession.close();
			return;
		}
    	
		if ("AM".equals(m[0]))
		{
			AM am = (AM) bs.getObject(Integer.parseInt(m[3]));
			if (am == null)
			{
				System.out.println("session " + m[2] + " sent message to non existing am. telling client to refresh now.");
				websocketSession.send("FORCE_REFRESH");
				websocketSession.close();
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
    			websocketSession.send("FORCE_REFRESH");
    			websocketSession.close();
    			return;
    		}
    		
    		if ("AM".equals(m[0]))
    		{
    			AM am = (AM) bs.getObject(Integer.parseInt(m[3]));
    			if (am == null)
    			{
    				System.out.println("session " + m[2] + " sent message to non existing am. telling client to refresh now.");
    				websocketSession.send("FORCE_REFRESH");
    				websocketSession.close();
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
    	if (sockets.containsKey(session))
    	{
    		sockets.get(session).closed = true;
    	}
    }


    public void onError(WebSocket session,Exception ex) {
    	ex.printStackTrace();
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