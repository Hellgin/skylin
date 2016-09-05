package skylin.server;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

public class SkylinContext implements Context{

	private static ConcurrentHashMap<String, Object> objects = new ConcurrentHashMap<String,Object>();

	@Override
	public Object addToEnvironment(String propName, Object propVal)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void bind(Name name, Object obj) throws NamingException {
		// TODO Auto-generated method stub
	}

	@Override
	public void bind(String name, Object obj) throws NamingException {
		// TODO Auto-generated method stub
		objects.put(name, obj);
	}

	@Override
	public void close() throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Name composeName(Name name, Name prefix) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String composeName(String name, String prefix)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Context createSubcontext(Name name) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Context createSubcontext(String name) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void destroySubcontext(Name name) throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroySubcontext(String name) throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Hashtable<?, ?> getEnvironment() throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNameInNamespace() throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NameParser getNameParser(Name name) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NameParser getNameParser(String name) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<NameClassPair> list(Name name)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<NameClassPair> list(String name)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<Binding> listBindings(Name name)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<Binding> listBindings(String name)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object lookup(Name name) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object lookup(String name) throws NamingException {
		// TODO Auto-generated method stub
		return objects.get(name);
	}

	@Override
	public Object lookupLink(Name name) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object lookupLink(String name) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void rebind(Name name, Object obj) throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rebind(String name, Object obj) throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object removeFromEnvironment(String propName) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void rename(Name oldName, Name newName) throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rename(String oldName, String newName) throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unbind(Name name) throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unbind(String name) throws NamingException {
		// TODO Auto-generated method stub
		
	}

}
