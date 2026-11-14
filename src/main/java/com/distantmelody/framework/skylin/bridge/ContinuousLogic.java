package com.distantmelody.framework.skylin.bridge;

import java.util.Calendar;

import com.distantmelody.framework.skylin.server.Server;

public class ContinuousLogic implements  Runnable {
    Thread thread;
	boolean running = true;

	long shutdownAt = Long.MAX_VALUE;

    public ContinuousLogic() 
    {
    	if (Server.frameworkParams.get("SHUTDOWN_DAILY") != null)
    	{
    		System.out.println(timeTilShutdown()/1000/60);
    		shutdownAt = System.currentTimeMillis() + timeTilShutdown();
    	}
        thread = new Thread(this);
        thread.start();
    }
    
	public void run()
	{
		int c = 0;
		while (running)
		{
			if (c==10)c=0;
			for (BridgeSession session:BridgeSession.sessions.values())
			{
				session.updateActiveAMs();
				session.cleanExposedBlobs();
				if (c==0)
				{
					session.webSocketPing();
				}
			}
			c++;
			if (System.currentTimeMillis() > shutdownAt) 
			{
				System.out.println("Scheduled shutdown");
				System.exit(0);
			};
			try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
		}
	}
	
	public static long timeTilShutdown()
	{
		long timeTilShutdown = -1;
		int inc = 24;
		int c = 0;
		while (timeTilShutdown <= 0)
		{
			c+=inc;
			timeTilShutdown = timeTil(c);
		}
		return timeTilShutdown;
	}
	
	public static long timeTil(int h)
	{
		Calendar c = Calendar.getInstance();
	    c.set(Calendar.HOUR_OF_DAY, h);
	    c.set(Calendar.MINUTE, 0);
	    c.set(Calendar.SECOND, 0);
	    c.set(Calendar.MILLISECOND, 0);

	    return  c.getTimeInMillis() - System.currentTimeMillis();
	}
}