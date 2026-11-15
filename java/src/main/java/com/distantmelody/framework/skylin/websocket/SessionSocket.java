package com.distantmelody.framework.skylin.websocket;

import org.java_websocket.WebSocket;

import com.distantmelody.framework.skylin.bridge.BridgeSession;

public class SessionSocket {
	BridgeSession session;
	public WebSocket websocketSession;
	boolean closed;
	long pingSent = System.currentTimeMillis();
	long lastPingTime;
	long appId;
	
	boolean pingInProgress;
	
    public void sendBytes(byte[] data)
    {
    	System.out.println("sending binary websocket: " + data.length);
    	websocketSession.send(data);
    }
    
    public void sendText(String text)
    {
    	websocketSession.send(text);
    	/*
    	try 
    	{
			websocketSession.getBasicRemote().sendText(text);
		} 
    	catch (IOException e) 
    	{
			e.printStackTrace();
		}
    	*/
    }

	public void ping() 
	{
		if (!pingInProgress)
		{
			pingSent = System.currentTimeMillis();
			sendText("ping");
			pingInProgress = true;
		}
	}

	public void pingReturned() 
	{
		if (pingInProgress)
		{
			lastPingTime = System.currentTimeMillis() - pingSent;
			pingInProgress = false;
			session.resetWebSocketUnavailableCount(appId);
			session.lastActivity = System.currentTimeMillis();
		}
	}
	
	public long getLastSuccessfullPingTime()
	{
		return lastPingTime;
	}
	
	public long getPingInProgressTime()
	{
		if (pingInProgress)
		{
			return System.currentTimeMillis() - pingSent;
		}
		return 0;
	}
}
