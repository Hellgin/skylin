package com.distantmelody.framework.skylin.bridge;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

/**
 * The PHP session handler (command {@code b} of the bridge, PHP class BridgeSessionHandler): keeps the PHP session
 * files and the lock that serializes the requests of one session.
 * <p>
 * Session data is the string PHP's {@code serialize()} made, which contains byte lengths ({@code s:5:"abcde";}) and
 * can contain any byte. It is therefore never converted to a String here: the bytes that PHP wrote are the bytes
 * that are stored and the bytes that PHP reads back.
 */
public class BridgeSessionHandler 
{
	String savepath;

	private static byte[] reply(String text)
	{
		return text.getBytes(StandardCharsets.UTF_8);
	}

	private byte[] open(String sp, String sessionName,String id) {
		BridgeSession session = BridgeSession.sessions.get(id);
		
		if (savepath == null)
		{
			savepath = sp;
		}
		
		if (session != null)
		{
			session.getLock();
		}
		else
		{
			try 
			{
				Files.deleteIfExists(new File(savepath+"/sess1_"+id).toPath());
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}

		

		try
		{
			new File(savepath).mkdirs();
			return reply("true");
		}
		catch (Exception e)
		{
			return reply("false");
		}
	}
	
	private byte[] read(String id) {
		
		try 
		{
			return Files.readAllBytes(new File(savepath+"/sess1_"+id).toPath());
		} 
		catch (NoSuchFileException e)
		{
			
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return reply("");
	}
	
	private byte[] write(String id, byte[] data) {
		try
		{
			Files.write(new File(savepath+"/sess1_"+id).toPath(), data);
			return reply("true");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return reply("");
	}
	
	private byte[] close(String id) {
		BridgeSession session = BridgeSession.sessions.get(id);
		if (session != null)
		{
			session.releaseLock();
		}
		return reply("true");
	}

	private byte[] destroy(String id) {

		BridgeSession session = BridgeSession.sessions.get(id);
		if (session != null)
		{
			session.getLock();
			//BridgeSession.sessions.remove(id);
			session.destroy();
		}
		try
		{
			Files.deleteIfExists(new File(savepath+"/sess1_"+id).toPath());
			return reply("true");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (session != null)
			{
				session.releaseLock();
			}
		}
		return reply("false");

		//return "".getBytes();
	}

	private byte[] gc(long max) {
		
		System.out.println("session gc initiated. Max = " + (max/1000f/60f/60f) + " minutes");
		max = 3*60*60*1000;
		System.out.println("Max Overidden to 180 minutes");
		
		if (savepath == null) return reply("true");
		String[] list = new File(savepath).list();
		if (list == null)return reply("true"); 
		//max = max * 1000;
		long time = System.currentTimeMillis() - max;
		loop:
		for (String f:list)
		{
			try
			{
				if (!f.startsWith("sess1_"))continue loop;
				File file = new File(savepath+"/"+f);
				//if (file.lastModified() > time) continue loop;
				BridgeSession session = BridgeSession.sessions.get(f.substring(6));
				if (session != null)
				{
					if (session.isLocked() || session.lastActivity > time)continue loop;
					session.getLock();
				}
				System.out.println("dropping session");
				file.delete();
				if (session != null)
				{
					session.releaseLock();	
					//BridgeSession.sessions.remove(f.substring(6));
					session.destroy();
				}	
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return reply("true");
		
		//return "true".getBytes();
	}
	
	/**
	 * Runs one session command. {@code d} is the data of the bridge command {@code b}: {@code <command>[,<data>]}.
	 * Commands: 1 open ({@code <save path>,<session id>,<session name>}), 2 read ({@code <id>}), 3 write
	 * ({@code <id>,<session data>}: everything after the comma is the data, byte-exact, commas and all), 4 close,
	 * 5 destroy, 6 gc.
	 */
	public byte[] process(ByteReader d)
	{
		String command = d.token(',');
		
		if ("1".equals(command))
		{
			String savePath = d.token(',');
			String id = d.token(',');
			String sessionName = d.string();
			return open(savePath,sessionName,id);
		}
		else if ("2".equals(command))
		{
			return read(d.string());
		}
		else if ("3".equals(command))
		{		
			String id = d.token(',');
			return write(id,d.bytes());
		}
		else if ("4".equals(command))
		{
			return close(d.string());
		}
		else if ("5".equals(command))
		{
			return destroy(d.string());
		}
		else if ("6".equals(command))
		{
			return gc(Long.parseLong(d.string()));
		}
		return reply("");
	}
}
