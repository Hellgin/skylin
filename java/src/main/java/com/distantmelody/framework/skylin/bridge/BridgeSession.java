package com.distantmelody.framework.skylin.bridge;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;

import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.distantmelody.framework.skylin.AM;
import com.distantmelody.framework.skylin.AMContainer;
import com.distantmelody.framework.skylin.SkylinBlob;
import com.distantmelody.framework.skylin.SkylinBlobLink;
import com.distantmelody.framework.skylin.websocket.SessionSocket;

public class BridgeSession 
{
	private static final Logger LOG = LoggerFactory.getLogger(BridgeSession.class);

	public static ConcurrentHashMap<String,BridgeSession> sessions = new ConcurrentHashMap<String,BridgeSession>();
	ConcurrentHashMap<Integer,Object> objects = new ConcurrentHashMap<Integer,Object>();
	int previousObjectId;
	public ArrayList<AM> activeAMs = new ArrayList<AM>();
	String id;
	ServletContext context;
	private HashMap<Long,ArrayList<SessionSocket>> sessionSockets = new HashMap<Long,ArrayList<SessionSocket>>();
	ConcurrentHashMap<Long,Long> webSocketLastAvailable = new ConcurrentHashMap<Long,Long>();
	
	private int nextExposedBlobIdCounter;
	private ConcurrentHashMap<Integer,SkylinBlobLink> exposedBlobs = new ConcurrentHashMap<Integer,SkylinBlobLink>();
	
	private boolean isLocked;
	public long lastActivity;
	
	public BridgeSession(String id, ServletContext context)
	{
		this.id = id;
		this.context = context;
	}
	
	public String instantiateByName(String data) 
	{
		try 
		{
			previousObjectId ++;
			Object newObject = Class.forName(data).newInstance();
			objects.put(previousObjectId, newObject);
			
			return previousObjectId+"";
		} 
		catch (ClassNotFoundException e) 
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e) 
		{
			e.printStackTrace();
		}
		catch (InstantiationException e) 
		{
			e.printStackTrace();
		}
		return "";
	}


	public String instantiateByClass(String data) {
		try 
		{
			previousObjectId ++;
			Object newObject = ((Class)objects.get(Integer.parseInt(data))).newInstance();
			objects.put(previousObjectId, newObject);
			
			return previousObjectId+"";
		} 
		catch (IllegalAccessException e) 
		{
			e.printStackTrace();
		}
		catch (InstantiationException e) 
		{
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * Calls a method for PHP. {@code data} is the command's payload, a sequence of comma terminated fields
	 * <pre>
	 * &lt;objectId&gt;,&lt;method&gt;,&lt;argCount&gt;,&lt;len1&gt;,&lt;len2&gt;,...,&lt;the arguments, back to back&gt;
	 * </pre>
	 * where every length is the length in bytes of the encoded argument (see {@link #bytesToObject}). The arguments are
	 * cut out of the payload by those lengths as bytes and decoded one by one; nothing is converted to a String
	 * before that.
	 *
	 * @return the encoded result (see {@link #objectToBytes})
	 */
	public byte[] instanceMethod(ByteReader data) 
	{
		int objectId = data.integer();
		String methodName = data.token(',');
		int amount = data.integer();
		int[] lengths = new int[amount];
		for (int i = 0; i < amount;i++)
		{
			lengths[i] = data.integer();
		}
		Object[] argObjects = new Object[amount];
		for (int i = 0; i < amount;i++)
		{
			argObjects[i] = bytesToObject(data.slice(lengths[i]));
		}
		
		Object object = objects.get(objectId);
		Object result = callMethod(object,methodName,argObjects);
		
		return objectToBytes(result);
	}


	private Object callMethod(Object receiver,String methodName, Object... params) 
	{
		  if (receiver == null || methodName == null) {
		    return null;
		  }
		  Class<?> cls = receiver.getClass();
		  Method[] methods = null;
		  if (receiver instanceof Class)
		  {
			  Method[] staticMethods = ((Class) receiver).getMethods();
			  Method[] instanceMethods = cls.getMethods();
			  methods = new Method[staticMethods.length+instanceMethods.length];
			  System.arraycopy(staticMethods, 0, methods, 0, staticMethods.length);
			  System.arraycopy(instanceMethods, 0, methods, staticMethods.length, instanceMethods.length);
		  }
		  else
		  {
			  methods = cls.getMethods();
		  }

		  Method toInvoke = null;
		  methodLoop: for (Method method : methods) {
		    if (!methodName.equals(method.getName())) {
		      continue;
		    }
		    Class<?>[] paramTypes = method.getParameterTypes();
		    if (params == null && paramTypes == null) {
		      toInvoke = method;
		      break;
		    } else if (params == null || paramTypes == null
		        || paramTypes.length != params.length) {
		      continue;
		    }

		    for (int i = 0; i < params.length; ++i) {
		      if (params[i] != null && !paramTypes[i].isAssignableFrom(params[i].getClass()) && !autobox(paramTypes[i],params[i].getClass())) {
		        continue methodLoop;
		      }
		    }
		    toInvoke = method;
		  }
		  if (toInvoke != null) {
		    try {
		      return toInvoke.invoke(receiver, params);
		    } catch (Exception t) {
		      t.printStackTrace();
		    }
		  }
		  else
		  {
			  if (receiver instanceof SkylinBridgeVirtualMethods)
			  {
				  boolean[] matchFound = new boolean[1];
				  try
				  {
					  Object returnValue = ((SkylinBridgeVirtualMethods) receiver).handleVirtualMethodCall(methodName,params,matchFound);
					  if (matchFound[0])
					  {
						  return returnValue;
					  }
				  }
				  catch (Exception t)
				  {
					  t.printStackTrace();
				  }

			  }
			  System.out.println("could not find method '" + methodName + "' in '" + receiver + "' with the given parameters.");
		  }
		  return null;
		}


	private boolean autobox(Class class1, Class class2) 
	{
		if (class1 == Integer.class && class2 == int.class) return true;
		if (class2 == Integer.class && class1 == int.class) return true;
		if (class1 == Boolean.class && class2 == boolean.class) return true;
		if (class2 == Boolean.class && class1 == boolean.class) return true;
		if (class1 == Double.class && class2 == double.class) return true;
		if (class2 == Double.class && class1 == double.class) return true;
		
		if ((class1 == Long.class || class1 == long.class) && (class2 == int.class || class2 == Integer.class)) return true;
		if ((class2 == Long.class || class2 == long.class) && (class1 == int.class || class1 == Integer.class)) return true;
		return false;
	}
	
	/**
	 * Encodes a Java value for PHP. The wire format (all lengths are BYTE lengths, text is UTF-8):
	 * <ul>
	 * <li>{@code s<text>}: string, {@code i<n>}: integer, {@code d<n>}: decimal number, {@code bT} / {@code bF}: boolean</li>
	 * <li>{@code o<id>}: any other object, as the id of a proxy that PHP uses to call back</li>
	 * <li>{@code a0}: empty array; {@code a<count>,<len1>,<len2>,...,<element 1><element 2>...}: array of
	 * encoded elements</li>
	 * <li>nothing at all for null</li>
	 * </ul>
	 * The inverse of {@link #bytesToObject(ByteReader)}.
	 */
	public byte[] objectToBytes(Object object)
	{
		if (object == null)
		{
			return new byte[0];
		}
		if (object instanceof Iterable || object.getClass().isArray())
		{
			Iterable<?> iter = null;
			if (object instanceof Iterable)
			{
				iter = (Iterable<?>) object;
			}
			else
			{
				iter = ArrayUtil.toIterable(object);
			}
			ArrayList<byte[]> elements = new ArrayList<byte[]>();
			for (Object o:iter)
			{
				elements.add(objectToBytes(o));
			}
			if (elements.isEmpty())
			{
				return new ByteWriter().ascii("a0").toByteArray();
			}
			ByteWriter out = new ByteWriter().ascii("a").ascii(elements.size()).ascii(",");
			for (byte[] element:elements)
			{
				out.ascii(element.length).ascii(",");
			}
			for (byte[] element:elements)
			{
				out.bytes(element);
			}
			return out.toByteArray();
		}
		if (object instanceof Integer)
		{
			return new ByteWriter().ascii("i").ascii(object.toString()).toByteArray();
		}
		if (object instanceof Double || object instanceof Float || object instanceof BigDecimal)
		{
			return new ByteWriter().ascii("d").ascii(object.toString()).toByteArray();
		}
		if (object instanceof Boolean)
		{
			if ((Boolean)object)
			{
				return new ByteWriter().ascii("bT").toByteArray();
			}
			return new ByteWriter().ascii("bF").toByteArray();
		}
		if (object instanceof String)
		{
			return new ByteWriter().ascii("s").utf8((String) object).toByteArray();
		}
		previousObjectId ++;
		objects.put(previousObjectId, object);
		return new ByteWriter().ascii("o").ascii(previousObjectId).toByteArray();
	}

	/**
	 * Decodes one encoded value sent by PHP (the format of {@link #objectToBytes}, plus {@code n} for null). The
	 * reader must be limited to exactly that value, see {@link ByteReader#slice(int)}: the content of a value is the
	 * rest of its slice. A string is decoded from UTF-8 here, once, and nowhere else.
	 */
	public Object bytesToObject(ByteReader arg) 
	{
		if (!arg.hasRemaining())
		{
			return null;
		}
		char t = arg.readChar();
		if (t == 's')
		{
			return arg.string();
		}
		if (t == 'i')
		{
			return Integer.parseInt(arg.string());
		}
		if (t == 'b')
		{
			return "T".equals(arg.string());
		}
		if (t == 'd')
		{
			//return Double.parseDouble(content);
			return new BigDecimal(arg.string());
		}
		if (t == 'a')
		{
			int size = arg.integer();
			if(size == 0)return new Object[] {};
			int[] lengths = new int[size];
			for (int i = 0; i < size;i++)
			{
				lengths[i] = arg.integer();
			}
			Object[] ret = new Object[size];
			for (int i = 0; i < size;i++)
			{
				ret[i] = bytesToObject(arg.slice(lengths[i]));
			}
			return narrowArray(ret);
		}
		if (t == 'o')
		{
			return objects.get(Integer.parseInt(arg.string()));
		}
		return null;
	}

	/** An array of only Integers, Booleans or Doubles becomes an int[], boolean[] or double[]; anything else stays an Object[]. */
	private static Object narrowArray(Object[] ret)
	{
		block:
		{
			for (Object o:ret)
			{
				if (!(o instanceof Integer))
				{
					break block;
				}
			}
			int[] newRet = new int[ret.length];
			for (int i = 0; i < newRet.length;i++)
			{
				newRet[i] = (Integer) ret[i];
			}
			return newRet;
		}
		block:
		{
			for (Object o:ret)
			{
				if (!(o instanceof Boolean))
				{
					break block;
				}
			}
			boolean[] newRet = new boolean[ret.length];
			for (int i = 0; i < newRet.length;i++)
			{
				newRet[i] = (Boolean) ret[i];
			}
			return newRet;
		}
		block:
		{
			for (Object o:ret)
			{
				if (!(o instanceof Double || o instanceof Float))
				{
					break block;
				}
			}
			double[] newRet = new double[ret.length];
			for (int i = 0; i < newRet.length;i++)
			{
				newRet[i] = ((Number) ret[i]).doubleValue();
			}
			return newRet;
		}
		return ret;
	}

	public String getClassInstance(String data) 
	{
		Object result;
		String[] d = data.split(",");
		try 
		{
			if (d.length == 1)
			{
				result = Class.forName(d[0]);
			}
			else
			{
				result = Class.forName(d[0],false,objects.get(Integer.parseInt(d[1])).getClass().getClassLoader()); 
			}
		} 
		catch (ClassNotFoundException e) 
		{
			e.printStackTrace();
			return "";
		}
		previousObjectId ++;
		objects.put(previousObjectId, result);
		return previousObjectId+"";
	}


	public String getServletContext(ServletContext c) 
	{
		previousObjectId ++;
		objects.put(previousObjectId, c);
		return previousObjectId+"";
	}
	
	public String getBridgeSession()
	{
		previousObjectId ++;
		objects.put(previousObjectId, this);
		return previousObjectId+"";
	}
	
	public void releaseObjects(String data) 
	{
		ArrayList<Integer> i = new ArrayList<Integer>();
		for (String d:data.split(","))
		{
			i.add(Integer.parseInt(d));
		}
		//System.out.println("Before: " + objects.size());
		objects.keySet().retainAll(i);
		for (int j = 0; j < activeAMs.size();j++)
		{
			if (!objects.contains(activeAMs.get(j)))
			{
				activeAMs.remove(j);
				j--;
			}
		}
		//System.out.println("after: " + objects.size());
		
		/*
		int j = 0;
		for (Object o:objects.values())
		{
			j++;
			System.out.println(j + ": " + o.getClass());
		}
		*/
		
	}
	
	public void updateActiveAMs()
	{
		ArrayList<AM> ams = (ArrayList<AM>)activeAMs.clone();
		for (AM am:ams)
		{
			if (am.initializedFinal)
			{
				am.update();
			}
		}
	}
	
	public String getId()
	{
		return id;
	}
	
	public ServletContext getContext()
	{
		return context;
	}

	public void addSessionSocket(long appId,SessionSocket socket) 
	{
		synchronized (sessionSockets)
		{
			ArrayList<SessionSocket> list = sessionSockets.get(appId);
			if (list == null)
			{
				list = new ArrayList<SessionSocket>();
				sessionSockets.put(appId, list);
			}
			list.add(socket);
		}
	}

	/** Removes one closed connection. The app entry goes with it when it was the last socket. */
	public void removeSessionSocket(long appId, SessionSocket socket)
	{
		synchronized (sessionSockets)
		{
			ArrayList<SessionSocket> list = sessionSockets.get(appId);
			if (list == null)
			{
				return;
			}
			list.remove(socket);
			if (list.isEmpty())
			{
				sessionSockets.remove(appId);
			}
		}
	}
	
	public ArrayList<SessionSocket> getSessionSockets(long appId)
	{
		synchronized (sessionSockets)
		{
			ArrayList<SessionSocket> list = sessionSockets.get(appId);
			if (list == null)
			{
				return new ArrayList<SessionSocket>();
			}
			return (ArrayList<SessionSocket>) list.clone();
		}
	}
	
	public void sendWebSocketText(long appId,String message)
	{
		ArrayList<SessionSocket> clone;
		synchronized (sessionSockets)
		{
			ArrayList<SessionSocket> sockets = sessionSockets.get(appId);
			if (sockets == null)
			{
				return;
			}
			clone = (ArrayList<SessionSocket>) sockets.clone();
		}
		for (SessionSocket ss:clone)
		{
			try
			{
				ss.sendText(message);
			}
			catch(WebsocketNotConnectedException e)
			{
				removeSessionSocket(appId, ss);
				ss.websocketSession.close();
			}
		}
	}
	
	public void sendWebSocketBinary(long appId,byte[] message)
	{
		ArrayList<SessionSocket> clone;
		synchronized (sessionSockets)
		{
			ArrayList<SessionSocket> sockets = sessionSockets.get(appId);
			if (sockets == null)
			{
				return;
			}
			clone = (ArrayList<SessionSocket>) sockets.clone();
		}
		for (SessionSocket ss:clone)
		{
			try
			{
				ss.sendBytes(message);
			}
			catch(WebsocketNotConnectedException e)
			{
				removeSessionSocket(appId, ss);
				ss.websocketSession.close();
			}
		}
	}
	
	public void webSocketPing() 
	{
		HashMap<Long,ArrayList<SessionSocket>> map;
		synchronized (sessionSockets)
		{
			map = (HashMap<Long, ArrayList<SessionSocket>>) sessionSockets.clone();
		}
		for (Long key:map.keySet())
		{
			ArrayList<SessionSocket> clone;
			synchronized (sessionSockets)
			{
				ArrayList<SessionSocket> sockets = sessionSockets.get(key);
				if (sockets == null)
				{
					continue;
				}
				clone = (ArrayList<SessionSocket>) sockets.clone();
			}
			for (SessionSocket ss:clone)
			{
				try
				{
					if (ss.getPingInProgressTime() > 60000)
					{
						removeSessionSocket(key, ss);
						ss.websocketSession.close();
					}
					else
					{
						ss.ping();			
					}
				}
				catch(WebsocketNotConnectedException e)
				{
					removeSessionSocket(key, ss);
					ss.websocketSession.close();
				}
			}
		}
		
	}
	
	public int exposeBlob(SkylinBlob blob,Map<String,Object> linkInfo)
	{
		if (nextExposedBlobIdCounter == Integer.MAX_VALUE-1)//not very likely...
		{
			nextExposedBlobIdCounter = 0;
		}
		nextExposedBlobIdCounter++;
		exposedBlobs.put(nextExposedBlobIdCounter, new SkylinBlobLink(blob,linkInfo));
		return nextExposedBlobIdCounter;
	}
	
	public SkylinBlobLink getExposedBlobLink(int id)
	{
		SkylinBlobLink o = exposedBlobs.get(id);
		exposedBlobs.remove(id);
		if (o == null)
		{
			return null;
		}
		return o;
	}

	public void cleanExposedBlobs() 
	{
		Object[] keys = exposedBlobs.keySet().toArray();
		for (int i = 0; i < keys.length;i++)
		{
			SkylinBlobLink b = exposedBlobs.get(keys[i]);
			if (b != null && b.createTime< System.currentTimeMillis() - 10000)
			{
				exposedBlobs.remove(keys[i]);
				i--;
			}
		}
	}
	
	
	
	public boolean isWebsocketAvailable(long appId)
	{
		synchronized (sessionSockets)
		{
			ArrayList<SessionSocket> sockets = sessionSockets.get(appId);
			if (sockets != null && sockets.size() > 0)
			{
				resetWebSocketUnavailableCount(appId);
				return true;
			}
		}
		return false;
		
	}
	
	public long websocketUnavailableFor(long appId)
	{
		if (isWebsocketAvailable(appId))
		{
			return 0;
		}
		Long last = webSocketLastAvailable.get(appId);
		if (last == 0)
		{
			return 100000;
		}
		return System.currentTimeMillis() - webSocketLastAvailable.get(appId);
	}
	
	public void resetWebSocketUnavailableCount(long appId)
	{
		webSocketLastAvailable.put(appId, System.currentTimeMillis());
	}
	
	
	public Object getObject(int id)
	{
		return objects.get(id);
	}

	public synchronized void getLock() {
		while (isLocked)
		{
			try{Thread.sleep(1);}catch (Exception e){e.printStackTrace();}
		}
		isLocked = true;
	}

	public void releaseLock() {
		isLocked = false;
	}

	public boolean isLocked() {
		return isLocked;
	}
	
	public void destroy()
	{
		ArrayList<SessionSocket> open = new ArrayList<SessionSocket>();
		try
		{
			synchronized (sessionSockets)
			{
				for (ArrayList<SessionSocket> s:sessionSockets.values())
				{
					open.addAll(s);
				}
				sessionSockets.clear();
			}
			for (SessionSocket ss:open)
			{
				try
				{
					ss.websocketSession.close();
				}
				catch (Exception e)
				{
					LOG.error("Could not close a websocket session", e);
				}
			}
		}
		catch (Exception e)
		{
			LOG.error("Could not destroy the bridge session", e);
		}
		
		//System.out.println("Bridge Session Destroyed");
		//new Exception().printStackTrace();
		
		sessions.remove(getId());
	}
}
