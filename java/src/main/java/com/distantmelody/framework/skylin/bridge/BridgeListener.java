package com.distantmelody.framework.skylin.bridge;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import com.distantmelody.framework.skylin.websocket.SocketServer;

public class BridgeListener implements Runnable {
    private ServletContext context = null;
    Thread thread;
	ServerSocket serverSocket;
	File socketFile;//only set when the bridge listens on a Unix domain socket
	boolean running = true;
	
	public static final int TCP_PORT = 1400;

	/**
	 * Path of the bridge Unix domain socket. Fixed by design, there is no configuration on either side: the PHP
	 * frontend has its own implementation (skylinBridgeSocketPath in BridgeConnection.php) that must return the same
	 * path. Linux and any other OS: /sky/bridge.sock; macOS: /tmp/sky/bridge.sock (the root volume is read-only, so no
	 * /sky); Windows 10+: C:\sky\bridge.sock. The directory has to be usable by the JVM and by the web server's PHP.
	 *
	 * @param osName value of the os.name system property
	 */
	public static String bridgeSocketPath(String osName)
	{
		String os = osName == null ? "" : osName.toLowerCase();
		if (os.contains("windows")) return "C:\\sky\\bridge.sock";
		if (os.contains("mac") || os.contains("darwin")) return "/tmp/sky/bridge.sock";
		return "/sky/bridge.sock";
	}

	public static File getSocketFile()
	{
		return new File(bridgeSocketPath(System.getProperty("os.name")));
	}

	/**
	 * The one rule that picks the bridge transport (the PHP side applies the same rule, see skylinBridgeUsesTcp in
	 * BridgeConnection.php): the Unix domain socket whenever it is supported, TCP 127.0.0.1:1400 otherwise (in
	 * practice only Windows before 10 1803). Support is probed on every OS, not derived from the OS name or version:
	 * PHP under Apache on Windows reports "Windows 8" (the httpd.exe has no compatibility manifest), so a version
	 * check would be wrong there. If the two probes ever disagree (for example a broken junixsocket native library on
	 * one machine), that is a setup problem and the bridge does not connect.
	 */
	public static boolean useTcpTransport()
	{
		return !AFUNIXSocket.isSupported();
	}
	private void deleteSocketFile()
	{
		if (socketFile == null) return;
		try {Files.deleteIfExists(socketFile.toPath());} catch (IOException e) {System.out.println("Unable to delete bridge socket file " + socketFile + ": " + e);}
	}

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
			if (useTcpTransport())
			{
				serverSocket = new ServerSocket(TCP_PORT, 50, java.net.InetAddress.getLoopbackAddress());
				System.out.println("Bridge listening on TCP 127.0.0.1:" + TCP_PORT + " (no Unix socket support on this Windows). Applications will run slower.");
			}
			else
			{
				//Unix domain socket: Linux, macOS and Windows 10+ (AF_UNIX) alike, at a fixed path per OS.
				socketFile = getSocketFile();
				if (socketFile.getPath().getBytes(StandardCharsets.UTF_8).length > 107)
				{
					throw new IOException("Bridge socket path is too long (limit is 107 bytes for AF_UNIX): " + socketFile);
				}
				File dir = socketFile.getParentFile();
				//Created with the default permissions. If the web server's PHP runs as another user than this JVM, the directory (and
				//the socket file) must allow both, exactly as /sky has to on Linux; nothing is widened here, the bridge has no authentication.
				if (dir != null && !dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory())
				{
					throw new IOException("Cannot create the bridge socket directory " + dir);
				}
				deleteSocketFile();//a crashed or killed server leaves its socket file behind (always on Windows)
				serverSocket = AFUNIXServerSocket.newInstance();
				serverSocket.bind(AFUNIXSocketAddress.of(socketFile));
				System.out.println("Bridge listening on unix socket " + socketFile);
			}
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
			socketServer = new SocketServer(1001,500);
			socketServer.start();
		} 
        catch (UnknownHostException e) 
        {
			e.printStackTrace();
		}
        
        Runtime.getRuntime().addShutdownHook(new Thread() {public void run() {
        	try {socketServer.stop();} catch (Exception e) {e.printStackTrace();}
        	try {serverSocket.close();}catch (Exception e) {e.printStackTrace();}
        	deleteSocketFile();
        	socketServer.closeAllConnections();
		}});
        
    }
	public void run()
	{
		while (running)
		{
			try
			{
    		    Socket clientSocket = serverSocket.accept();
    		    if (!(serverSocket instanceof AFUNIXServerSocket)) clientSocket.setTcpNoDelay(true);
    		    new BridgeConnection(clientSocket,context);
			}
			catch (IOException e)
			{
			}
		}
	}
}