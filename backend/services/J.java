package skylin.services;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class J 
{
	int ARRAY = 0;
	int MAP = 1;
	
	int type;
	
	ArrayList<Object> arrayValues;
	Map<String,J> mapValues;
	private boolean arrayHint;
	
	private J()
	{
		
	}
	
	private J(boolean map, Object... d)
	{
		if (map)
		{
			type = MAP;
			mapValues = new HashMap<String,J>();
			for (int i = 0; i < d.length;i+=2)
			{
				Object v = d[i+1];
				if (!(v instanceof J))
				{
					v = new J(false,v);
				}
				else if (((J)v).isMap())
				{
					v = new J(false,v);
				}
				mapValues.put((String)d[i], (J)v);
			}
		}
		else
		{
			type = ARRAY;
			arrayValues = new ArrayList<Object>();
			if (d == null)
			{
				arrayValues.add(null);
			}
			else
			{
				for (Object o:d)
				{
					if (o != null && o.getClass().isArray())
					{
						int length =  Array.getLength(o);
						for (int i = 0; i < length;i++)
						{
							arrayValues.add(Array.get(o, i));
						}
					}
					else if (o != null && o instanceof Iterable) 
					{
						for (Object o2 : (Iterable) o) 
						{
							arrayValues.add(o2);
				        }
				    }
					else
					{
						arrayValues.add(o);
					}
				}
			}
		}
	}
	
	public static J A(Object... d){	J ret = new J(false,d); ret.arrayHint = true;return ret;}
	private static J A_INTERNAL(Object... d){return new J(false,d);}
	public static J M(Object... d){return new J(true,d);}
	
	public static J fromString(String s)
	{
		boolean inString = false;
		for (int i = 0; i < s.length();i++)
		{
			if (s.charAt(i) == '"')
			{
				inString = !inString;
			}
			if (!inString)
			{
				char c = s.charAt(i);
				if (c == ' ' || c == '\n' || c == '\r' || c == '\t')
				{
					s = s.substring(0,i) + s.substring(i+1,s.length());
					i--;
				}
			}
		}
		return new J().fromStringInternal(s);
	}
	
	private J fromStringInternal(String s)
	{
		if (s.startsWith("{"))
		{
			type = MAP;
			s = s.substring(1,s.length()-1);
			mapValues = getMap(s);
		}
		else
		{
			type = ARRAY;
			if (s.startsWith("["))
			{
				s = s.substring(1,s.length()-1);
				arrayHint = true;
			}
			arrayValues = getArray(s);
		}
		return this;
	}
	
	private ArrayList<Object> getArray(String s) {
		ArrayList<Object> ret = new ArrayList<Object>();
		ArrayList<String> elements = getElements(s);
		for (String e:elements)
		{
			//System.out.println("e:"+e);
			if (e.startsWith("{") || e.startsWith("["))//second part might not be needed. dont think it would be legal json
			{
				ret.add(new J().fromStringInternal(e));
			}
			else if (e.startsWith("\""))
			{
				ret.add(e.substring(1,e.length()-1));
			}
			else if (e.toLowerCase().equals("true") || e.toLowerCase().equals("false"))
			{
				ret.add(Boolean.parseBoolean(e));
			}
			else if (e.toLowerCase().equals("null"))
			{
				ret.add(null);
			}
			else
			{
				ret.add(new BigDecimal(Double.parseDouble(e)));
			}
		}
		return ret;
	}

	private Map<String, J> getMap(String s) 
	{
		HashMap<String,J> ret = new HashMap<String,J>();
		ArrayList<String> elements = getElements(s);
		for (String e:elements)
		{
			J newj = new J().fromStringInternal(e.substring(e.indexOf(":")+1,e.length()));
			if (newj.isMap())
			{
				newj = J.A_INTERNAL(newj);
			}
			ret.put(e.substring(1, e.indexOf(":")-1), newj);
		}
		return ret;
	}
	
	public static ArrayList<String> getElements(String r) 
	{
		ArrayList<String> ret = new ArrayList<String>();
		
		int level = 0;
		boolean inString = false;
		int count = 0;
		while (r.length() > 0)
		{
			if (r.charAt(count) == '"')
			{
				inString = !inString;
			}
			if (!inString && (r.charAt(count) == '{' ||  r.charAt(count) == '['))
			{
				level++;
			}
			if (!inString && (r.charAt(count) == '}' ||  r.charAt(count) == ']'))
			{
				level--;
			}
			if (!inString && level == 0 && (r.charAt(count) == ',' || count == r.length()-1))
			{
				int take = count;
				if (count == r.length()-1)
				{
					take++;
				}
				String s = r.substring(0,take);
				ret.add(s);

				r = r.substring(count+1);
				count = 0;
			}
			else
			{
				count++;
			}
		}
		return ret;
	}

	public String toString()
	{
		String ret = "";
		if (type == ARRAY)
		{
			for (int i = 0; i < arrayValues.size();i++)
			{
				Object e = arrayValues.get(i);
				String v = e+"";
				if (e instanceof String)
				{
					v = "\""+v+"\"";
				}
				if (i < arrayValues.size()-1)
				{
					ret += v + ",";
				}
				else
				{
					ret += v;
				}	
			}
			if (arrayValues.size() > 1 || arrayHint)
			{
				ret = "["+ret+"]";
			}
			if (ret.length()==0)
			{
				ret = "[]";
			}
		}
		else//type == MAP
		{
			int i = 0;
			for (String k: mapValues.keySet())
			{
				Object e = mapValues.get(k);
				String v = e.toString();
				if (i < mapValues.size()-1)
				{
					ret += "\""+k+"\":"+v + ",";
				}
				else
				{
					ret += "\""+k+"\":"+v;
				}
				i++;
			}
			ret = "{"+ret+"}";
		}
		return ret;
	}
	
	public Object[] values()
	{
		/*
		if (type == ARRAY)
		{
			return arrayValues.toArray();
		}
		else//type == map
		{
			return mapValues.values().toArray();
		}
		*/
		return arrayValues.toArray();
	}
	public J[] valuesj()
	{
		Object[] v = values();
		J[] ret = new J[v.length];
		for (int i = 0; i < v.length;i++)
		{
			ret[i] = (J) v[i];
		}
		return ret;
	}
	
	public J get(String name)
	{
		if (isMap())
		{
			return mapValues.get(name);
		}
		return getJ(0).getJ(name);
	}
	

	public boolean contains(String name) {
		if (isMap())
		{
			return mapValues.containsKey(name);
		}
		return getJ(0).contains(name);
	}
	

	public void remove(String name) {
		if (isMap())
		{
			mapValues.remove(name);
			return;
		}
		getJ(0).remove(name);
	}
	
	public Object get(int index)
	{
		return arrayValues.get(index);
	}
	public Object get()
	{
		return arrayValues.get(0);
	}
	
	public J getJ(int index)
	{
		return (J)get(index);
	}
	
	public J getJ(String name)
	{
		return (J)get(name);
	}
	
	public String[] keys()
	{
		String ret[] = new String[mapValues.size()];
		int i = 0;
		for (String k:mapValues.keySet())
		{
			ret[i] = k;
			i++;
		}
		return ret;
	}
	
	public boolean isMap()
	{
		return type == MAP;
	}
	
	
	public int getInt()
	{
		return ((BigDecimal)get(0)).intValue();
	}
	public BigDecimal getBigDecimal()
	{
		return (BigDecimal)get(0);
	}
	public int getInt(int index)
	{
		return ((BigDecimal)get(index)).intValue();
	}
	public BigDecimal getBigDecimal(int index)
	{
		return (BigDecimal)get(index);
	}
	public String getString()
	{
		return (String)get(0);
	}
	
	public String getString(int index)
	{
		return (String)get(index);
	}
	
	public String getString(String name)
	{
		J j = get(name);
		if (j == null)return null;
		return (String)j.get(0);
	}
	
	public String getString(String name, int index)
	{
		return (String)get(name).get(index);
	}
	
	public int getInt(String name)
	{
		return ((BigDecimal)get(name).get(0)).intValue();
	}
	
	public int getInt(String name,int index)
	{
		return ((BigDecimal)get(name).get(index)).intValue();
	}
	
	public float getFloat(String name)
	{
		return ((BigDecimal)get(name).get(0)).floatValue();
	}
	
	public float getFloat(String name,int index)
	{
		return ((BigDecimal)get(name).get(index)).floatValue();
	}
	
	public BigDecimal getBigDecimal(String name)
	{
		return (BigDecimal)get(name).get(0);
	}
	
	public BigDecimal getBigDecimal(String name, int index)
	{
		return (BigDecimal)get(name).get(index);
	}
	
	public boolean getBoolean(int index)
	{
		return (boolean)get(index);
	}
	
	public boolean getBoolean(String name)
	{
		return (boolean)get(name).get(0);
	}
	
	public boolean getBoolean(String name, int index)
	{
		return (boolean)get(name).get(index);
	}
	
	public void add(Object v)
	{
		arrayValues.add(v);
	}
	
	public void put(String key, Object v)
	{
		J value = null;
		if (v instanceof J)
		{
			value = (J) v;
			if (value.isMap())
			{
				value = J.A_INTERNAL(value);
			}
		}
		else
		{
			value = J.A_INTERNAL(v);
		}
		mapValues.put(key, value);
	}

	public int getMapSize() {
		if (isMap())
		{
			return mapValues.size();
		}
		else
		{
			return getJ(0).getMapSize();
		}
	}
	
	public int getArraySize()
	{
		return arrayValues.size();
	}
}