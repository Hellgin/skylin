package skylin.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector implements Runnable
{
	SkylinDataSource ds;
	Thread thread;
	
	boolean databaseIssue;
	int databaseIssueRetryCount = 0;
	long dataBaseIssueFirstTime;
	
	public DatabaseConnector(SkylinDataSource ds)
	{
		this.ds = ds;
		thread = new Thread(this);
		thread.start();
	}
	
	public void run() 
	{
		outer:
		while (true)
		{
			if (ds.currentSize < ds.minSize)
			{
				Connection con = null;
				try {
					con = DriverManager.getConnection("jdbc:oracle:thin:@"+ds.host+":"+ds.port+":"+ds.dbName, ds.username, ds.password);
					ds.pool.add(new SkylinConnection(con,ds));
					ds.currentSize++;
					if (databaseIssue)
					{
						databaseIssue = false;
						databaseIssueRetryCount = 0;
						System.out.println("The database used by " + ds.name + " is working again.");
					}
				} 
				catch (SQLException e) {
					if (!databaseIssue)
					{
						databaseIssue = true;
						dataBaseIssueFirstTime = System.currentTimeMillis();
						System.out.println("Error getting connections from the database used by " + ds.name);
					}
					else
					{
						databaseIssueRetryCount++;
						System.out.println("Error getting connections from the database used by " + ds.name + " - " + databaseIssueRetryCount + " min");
					}
					try{Thread.sleep(Math.max(0, dataBaseIssueFirstTime+(databaseIssueRetryCount+1)*(60*1000)-System.currentTimeMillis()));} catch (InterruptedException ee) {}
					continue outer;
				}
			}
			inner:
			if (ds.currentSize > ds.maxSize)
			{
				SkylinConnection con = ds.pool.poll();
				if (con == null)
				{
					break inner;
				}
				try 
				{
					ds.currentSize--;
					con.kill();
					continue outer;
				} 
				catch (SQLException e) 
				{
					continue outer;
				}
				
			}

			SkylinConnection toCheck = ds.pool.poll();
			if (toCheck != null)
			{
				try
				{
					toCheck.inPool = false;
					if (!toCheck.isValid(10))
					{
						ds.currentSize--;
						try{toCheck.kill();}catch (Exception e){};
						continue outer;//skip sleep and so the next one can be checked sooner. If one connection is broken theres a high chance the rest is as wel.
					}
					else
					{
						toCheck.close();
					}
				}
				catch (SQLException e)
				{
					try{toCheck.kill();}catch (Exception e2){};
				}
			}
			ds.createConnectionSnapShot();
			try{Thread.sleep(1000);} catch (InterruptedException e) {}
			
		}
	}

}
