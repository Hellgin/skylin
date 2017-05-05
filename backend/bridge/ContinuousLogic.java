package skylin.bridge;

public class ContinuousLogic implements  Runnable {
    Thread thread;
	boolean running = true;


    public ContinuousLogic() 
    {
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
			try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
		}
	}
}