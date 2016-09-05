package skylin.bridge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.Socket;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import skylin.server.SkylinServletConfig;
import skylin.services.ServiceContainer;
import skylin.services.SkylinHttpServletRequest;
import skylin.services.SkylinHttpServletResponse;
import skylin.services.SkylinServletInputStream;
import skylin.services.SkylinServletOutputStream;

public class BridgeConnection implements Runnable{

	Thread thread;
	Socket socket;
	InputStream is;
	OutputStream os;
	String read = "";
	ServletContext context;
	boolean keepAlive = true;
	BridgeSession session;
	long lastActive;
	
	public BridgeConnection(Socket clientSocket, ServletContext context) 
	{
		socket = clientSocket;
		this.context = context;
		try
		{
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
		lastActive = System.currentTimeMillis();
		while (keepAlive)
		{
			try
			{
				int a = is.available();
				if (a > 0)
				{
					byte[] r = new byte[a];
					int dif = is.read(r) - a;
					if (dif != 0){throw new Exception("wtf!");};
					read += new String(r);
					if (read.length() >= 7)
					{
						int size = Integer.parseInt(read.substring(0, 7));
						if (read.length() >= size + 7)
						{
							byte[] ret = process(read.substring(7,size+7));
							String length = Integer.toHexString(ret.length)+"";
							while (length.length() < 7)
							{
								length = "0"+length;
							}
							byte[] l = length.getBytes();
							byte[] toSend = new byte[l.length + ret.length];
							
							System.arraycopy(l, 0, toSend, 0, l.length);
							System.arraycopy(ret, 0, toSend, l.length, ret.length);
							os.write(toSend);//writing the byte arrays seperately to the outputstream causes severe slowdown on linux
							read = read.substring(size+7);
						}
					}
					lastActive = System.currentTimeMillis();
				}
				else 
				{
					if (System.currentTimeMillis() > lastActive + 100)
					{
						if (System.currentTimeMillis() > lastActive + 10000)
						{
							keepAlive = false;
						}
						Thread.sleep(1);
					}
				}
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

	private byte[] process(String data) 
	{	
		int commaIndex = data.indexOf(",");
		String command = data;
		if (commaIndex != -1)
		{
			command = data.substring(0,commaIndex);
			data = data.substring(commaIndex+1);
		}
		if ("9".equals(command))
		{
			String[] t = data.split(",", 2);
			String service = t[0];
			String body = null;
			if (t.length > 1)
			{
				body = t[1];
			}
			else
			{
				body = "";
			}
			
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
				req.setInputStream(new SkylinServletInputStream(body.getBytes()));
				req.setServletContext(context);
				SkylinHttpServletResponse res = new SkylinHttpServletResponse();
				servlet.service(req, res);
				String contentType = res.getContentType();
				
				
				//String header = "Content-Type:"+contentType;
				byte[] responseData = ((SkylinServletOutputStream)res.getOutputStream()).getData();
				
				//System.out.println(new String(res.getPackedHeaderData()));
				
				
				//byte[] h= (header.length()+","+header).getBytes();
				byte[] h = res.getPackedHeaderData();
				byte[] ret = new byte[responseData.length+h.length];
				System.arraycopy(h, 0, ret, 0, h.length);
				System.arraycopy(responseData, 0, ret, h.length, responseData.length);
				
				return ret;
				
				//return (header.length()+","+header+new String(responseData)).getBytes();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else if ("0".equals(command))
		{
			return session.instantiateByName(data).getBytes();
		}
		else if ("1".equals(command))
		{
			return session.instantiateByClass(data).getBytes();
		}
		else if ("2".equals(command))
		{
			return session.instanceMethod(data).getBytes();
		}
		else if ("3".equals(command))
		{
			return session.getClassInstance(data).getBytes();
		}
		else if ("4".equals(command))
		{
			return session.getServletContext(context).getBytes();
		}
		else if ("5".equals(command))
		{
			session.releaseObjects(data);
		}
		else if ("6".equals(command))
		{
			keepAlive = false;
		}
		else if ("7".equals(command))
		{
			session.getSessionSocket().sendText(data);
		}
		else if ("8".equals(command))
		{
			session = BridgeSession.sessions.get(data);
			if (session == null)
			{
				session = new BridgeSession(data,context);
				BridgeSession.sessions.put(data, session);
				return "1".getBytes();
			}
			else
			{
				return "0".getBytes();
			}
		}
		else if ("a".equals(command))
		{
			return session.getBridgeSession().getBytes();
		}
		return "".getBytes();
	}
}
