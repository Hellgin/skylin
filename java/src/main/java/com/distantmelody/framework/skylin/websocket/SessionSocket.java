package com.distantmelody.framework.skylin.websocket;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.distantmelody.framework.skylin.bridge.BridgeSession;

public class SessionSocket {
	private static final Logger LOG = LoggerFactory.getLogger(SessionSocket.class);

	BridgeSession session;
	public WebSocket websocketSession;
	boolean closed;
	long pingSent = System.currentTimeMillis();
	long lastPingTime;
	long appId;
	
	boolean pingInProgress;
	
    public void sendBytes(byte[] data)
    {
    	LOG.debug("sending binary websocket: " + data.length);
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
			if (session != null)
			{
				session.resetWebSocketUnavailableCount(appId);
				session.lastActivity = System.currentTimeMillis();
			}
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

	/** Drops this socket from its bridge session so a closed connection cannot keep the session alive. */
	void detach()
	{
		BridgeSession owner = session;
		session = null;
		if (owner != null)
		{
			owner.removeSessionSocket(appId, this);
		}
	}
}
