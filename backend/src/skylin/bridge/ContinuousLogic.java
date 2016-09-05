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
		while (running)
		{
			for (BridgeSession session:BridgeSession.sessions.values())
			{
				session.updateActiveAMs();
				session.cleanExposedBlobs();
			}
			try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
		}
	}
}