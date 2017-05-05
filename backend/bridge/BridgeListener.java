package skylin.bridge;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import skylin.websocket.SocketServer;

public class BridgeListener implements Runnable {
    private ServletContext context = null;
    Thread thread;
	ServerSocket serverSocket;
	boolean running = true;
	
	ContinuousLogic continuousLogic;
	SocketServer socketServer;

	/*
    public void contextDestroyed(ServletContextEvent event) {
        this.context = null;
        running = false;
        continuousLogic.running = false;
        try 
        {
			serverSocket.close();
		}
        catch (IOException e) 
        {
			e.printStackTrace();
		}
    }*/

    public void contextInitialized(ServletContextEvent event) {
        this.context = event.getServletContext();
		try 
		{
			serverSocket = new ServerSocket(1400);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			return;
		}
        thread = new Thread(this);
        thread.start();
        
        continuousLogic = new ContinuousLogic();
        
        try 
        {
			socketServer = new SocketServer(1001);
			socketServer.start();
		} 
        catch (UnknownHostException e) 
        {
			e.printStackTrace();
		}
    }
	public void run()
	{
		while (running)
		{
			try
			{
    		    Socket clientSocket = serverSocket.accept();
    		    new BridgeConnection(clientSocket,context);
			}
			catch (IOException e)
			{
			}
		}
	}
}