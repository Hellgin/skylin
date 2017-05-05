package skylin.server;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

public class SkylinContextFactory implements InitialContextFactory
{

	@Override
	public Context getInitialContext(Hashtable<?, ?> env)
			throws NamingException {
		return new SkylinContext();
	}

}
