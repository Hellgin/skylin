package skylin.websocket;

import org.java_websocket.WebSocket;

import skylin.bridge.BridgeSession;

public class SessionSocket {
	BridgeSession session;
	WebSocket websocketSession;
	boolean closed;
	
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
}
