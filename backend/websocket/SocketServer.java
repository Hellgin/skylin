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
    		String[] m = message.split(",", 3);
    		
       		BridgeSession bs = BridgeSession.sessions.get(m[2]);
    		if (bs == null)
    		{
    			websocketSession.close();
    			return;
    		}
    		
    		if ("CONNECTED".equals(m[0]))
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
        	//currently websocket traffic is only from server to client. Might change in future. (with the exception of some framework stuff)
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