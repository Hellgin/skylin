package skylin.bridge;

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

import skylin.AM;
import skylin.AMContainer;
import skylin.SkylinBlob;
import skylin.SkylinBlobLink;
import skylin.websocket.SessionSocket;

public class BridgeSession 
{
	public static ConcurrentHashMap<String,BridgeSession> sessions = new ConcurrentHashMap<String,BridgeSession>();
	ConcurrentHashMap<Integer,Object> objects = new ConcurrentHashMap<Integer,Object>();
	int previousObjectId;
	public ArrayList<AM> activeAMs = new ArrayList<AM>();
	String id;
	ServletContext context;
	SessionSocket sessionSocket;
	
	private int nextExposedBlobIdCounter;
	private ConcurrentHashMap<Integer,SkylinBlobLink> exposedBlobs = new ConcurrentHashMap<Integer,SkylinBlobLink>();
	
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

	public String instanceMethod(String data) 
	{
		String[] d = data.split(",",4);
		int amount = Integer.parseInt(d[2]);
		String dd[] = d[3].split(",",amount+1);
		String argData = dd[dd.length-1];
		int nextOffset = 0;
		//Class[] argClasses = new Class[amount];
		Object[] argObjects = new Object[amount];
		for (int i = 0; i < amount;i++)
		{
			int length = Integer.parseInt(dd[i]);
			String arg = argData.substring(nextOffset, nextOffset+length);
			nextOffset += length;
			
			argObjects[i] = stringToObject(arg);
		}
		

		
		Object object = objects.get(Integer.parseInt(d[0]));
		Object result = callMethod(object,d[1],argObjects);
		
		return objectToString(result);
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
	
	public String objectToString(Object object)
	{
		if (object == null)
		{
			return "";
		}
		if (object instanceof Iterable || object.getClass().isArray())
		{
			Iterable iter = null;
			if (object instanceof Iterable)
			{
				iter = (Iterable) object;
			}
			else
			{
				iter = ArrayUtil.toIterable(object);
			}
			String data = "";
			String sizes ="";
			int size = 0;
			for (Object o:iter)
			{
				size++;
				String str = objectToString(o);
				data = data += str;
				sizes += str.length()+",";
			}
			if (size == 0)
			{
				return "a0";
			}
			return "a"+size+","+sizes+data;
		}
		if (object instanceof Integer)
		{
			return "i"+object;
		}
		if (object instanceof Double || object instanceof Float || object instanceof BigDecimal)
		{
			return "d"+object;
		}
		if (object instanceof Boolean)
		{
			if ((Boolean)object)
			{
				return "bT";
			}
			return "bF";
		}
		if (object instanceof String)
		{
			return "s"+object;
		}
		previousObjectId ++;
		objects.put(previousObjectId, object);
		return "o"+previousObjectId;
	}

	public Object stringToObject(String arg) 
	{
		String t = arg.charAt(0)+"";
		String content = arg.substring(1);
		if ("s".equals(t))
		{
			return content;
		}
		if ("i".equals(t))
		{
			return Integer.parseInt(content);
		}
		if ("b".equals(t))
		{
			if ("T".equals(content))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		if ("d".equals(t))
		{
			//return Double.parseDouble(content);
			return new BigDecimal(content);
		}
		if ("a".equals(t))
		{
			String[] a = content.split(",",2);
			int size = Integer.parseInt(a[0]);
			Object[] ret = new Object[size];
			a = a[1].split(",",size+1);
			content = a[size];
			int nextOffset = 0;
			for (int i = 0; i < size;i++)
			{
				int splitSize = Integer.parseInt(a[i]);
				ret[i] = stringToObject(content.substring(nextOffset, nextOffset+splitSize));
				nextOffset = nextOffset + splitSize;
			}
			block:
			{
				for (Object o:ret)
				{
					if (!(o instanceof Integer || o.getClass() == int.class))
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
					if (!(o instanceof Boolean || o.getClass() == boolean.class))
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
					if (!(o instanceof Double || o.getClass() == double.class || o instanceof Float || o.getClass() == float.class))
					{
						break block;
					}
				}
				double[] newRet = new double[ret.length];
				for (int i = 0; i < newRet.length;i++)
				{
					newRet[i] = (Double) ret[i];
				}
				return newRet;
			}
			return ret;
		}
		if ("o".equals(t))
		{
			return objects.get(Integer.parseInt(content));
		}
		if ("n".equals(t))
		{
			return null;
		}
		return null;
	}

	public String getClassInstance(String data) 
	{
		Object result;
		try 
		{
			result = Class.forName(data);
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
			if (am.initializedStep2)
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

	public void setSessionSocket(SessionSocket sessionSocket) 
	{
		this.sessionSocket = sessionSocket;
	}
	
	public SessionSocket getSessionSocket()
	{
		return sessionSocket;
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
	
	public boolean isWebsocketAvailable()
	{
		return sessionSocket != null;
	}
}
