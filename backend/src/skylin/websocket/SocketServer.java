package skylin.websocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import skylin.bridge.BridgeSession;

public class SocketServer extends WebSocketServer{
	
	HashMap<WebSocket,SessionSocket> sockets = new HashMap<WebSocket,SessionSocket>();

	public SocketServer( int port ) throws UnknownHostException {
		super( new InetSocketAddress( port ) );
	}
	
    public void onOpen(WebSocket session, ClientHandshake handshake) {
		//System.out.println( session.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!" );
    }


    public void onMessage(WebSocket websocketSession,String message) {
    	if (!sockets.containsKey(websocketSession))
        {
    		SessionSocket ss = new SessionSocket();
    		ss.session = BridgeSession.sessions.get(message);
        	ss.session.setSessionSocket(ss);
        	ss.websocketSession = websocketSession;
        	sockets.put(websocketSession, ss);
    		//System.out.println("session id: " + ss.session.getId());
        }
        else
        {
        	//currently websocket traffic is only from server to client. Might change in future.
        }
    	/*
    	if (session == null)
    	{
    		session = BridgeSession.sessions.get(message);
    		session.setSessionSocket(this);
    		this.websocketSession = websocketSession;
    	}
    	else
    	{
    		//currently websocket traffic is only from server to client. Might change in future.
    	}
        return;
        */
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
    



}