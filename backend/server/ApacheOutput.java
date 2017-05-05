package skylin.server;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ApacheOutput implements Runnable
{
	Thread thread;
	String path;
	public ApacheOutput(String path)
	{
		this.path = path;
		thread = new Thread(this);
		thread.start();
	}

	@Override
	public void run() 
	{
		BufferedInputStream reader = null;
		try {
			reader = new BufferedInputStream( 
				    new FileInputStream( path ) );
			 while( reader.available() > 0 ) {
		            reader.skip(reader.available());
		        }
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		while (true)
		{
			try
			{
			 if( reader.available() > 0 ) {
		            System.err.print( (char)reader.read() );
		        }
		        else {
		            try {
		                Thread.sleep(100);
		            }
		            catch( InterruptedException ex )
		            {
		            }
		        }
			}
			catch (Exception e)
			{
				
			}
		}
	}
}
