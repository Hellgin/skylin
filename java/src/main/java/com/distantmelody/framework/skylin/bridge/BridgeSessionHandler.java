package com.distantmelody.framework.skylin.bridge;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The PHP session handler (command {@code b} of the bridge, PHP class BridgeSessionHandler): keeps the PHP session
 * files and the lock that serializes the requests of one session.
 * <p>
 * Session data is the string PHP's {@code serialize()} made, which contains byte lengths ({@code s:5:"abcde";}) and
 * can contain any byte. It is therefore never converted to a String here: the bytes that PHP wrote are the bytes
 * that are stored and the bytes that PHP reads back.
 * <p>
 * The session id comes from the browser's cookie and becomes part of a file name, so every command refuses an id that
 * {@link BridgeSession#isValidId} does not accept, and a file that would not be directly inside the save path.
 */
public class BridgeSessionHandler 
{
	private static final Logger LOG = LoggerFactory.getLogger(BridgeSessionHandler.class);

	String savepath;

	private static byte[] reply(String text)
	{
		return text.getBytes(StandardCharsets.UTF_8);
	}

	/** The session file of the id, or null (logged) when the id is not valid or the file would be outside the save path. */
	private Path sessionFile(String id)
	{
		if (!BridgeSession.isValidId(id) || savepath == null)
		{
			LOG.warn("Session handler: refused a session id that is not 22 to 128 letters, digits and '-'");
			return null;
		}
		Path dir = Path.of(savepath).toAbsolutePath().normalize();
		Path file = dir.resolve("sess1_" + id).normalize();
		if (!dir.equals(file.getParent()))
		{
			LOG.warn("Session handler: refused a session file outside the save path");
			return null;
		}
		return file;
	}
	
	private byte[] open(String sp, String sessionName,String id) {
		if (savepath == null)
		{
			savepath = sp;
		}
		if (id.isEmpty())
		{
			//no cookie, or one PHP does not send: PHP makes a new id next
			return makeSavePath();
		}
		Path file = sessionFile(id);
		if (file == null)
		{
			return reply("false");
		}

		BridgeSession session = BridgeSession.sessions.get(id);
		if (session != null)
		{
			session.getLock();
		}
		else
		{
			try 
			{
				Files.deleteIfExists(file);
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		return makeSavePath();
	}

	private byte[] makeSavePath()
	{
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
		Path file = sessionFile(id);
		if (file == null)
		{
			return reply("");
		}
		try 
		{
			return Files.readAllBytes(file);
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
		Path file = sessionFile(id);
		if (file == null)
		{
			return reply("false");
		}
		try
		{
			Files.write(file, data);
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
		Path file = sessionFile(id);
		if (file == null)
		{
			return reply("false");
		}

		BridgeSession session = BridgeSession.sessions.get(id);
		if (session != null)
		{
			session.getLock();
			//BridgeSession.sessions.remove(id);
			session.destroy();
		}
		try
		{
			Files.deleteIfExists(file);
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

	/** PHP's validateId (session.use_strict_mode): only a session the JVM holds or has a file of is taken from a cookie. */
	private byte[] exists(String id) {
		Path file = sessionFile(id);
		if (file == null)
		{
			return reply("false");
		}
		return reply(BridgeSession.sessions.containsKey(id) || Files.exists(file) ? "true" : "false");
	}

	private byte[] gc(long max) {
		
		LOG.info("session gc initiated. Max = " + (max/1000f/60f/60f) + " minutes");
		max = 3*60*60*1000;
		LOG.info("Max Overidden to 180 minutes");
		
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
				LOG.info("dropping session");
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
	 * Commands: 1 open ({@code <save path>,<session id>,<session name>}; an empty id when PHP makes a new one), 2 read ({@code <id>}), 3 write
	 * ({@code <id>,<session data>}: everything after the comma is the data, byte-exact, commas and all), 4 close,
	 * 5 destroy, 6 gc, 7 exists ({@code <id>}: {@code true} when the JVM holds the session or has its file).
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
		else if ("7".equals(command))
		{
			return exists(d.string());
		}
		return reply("");
	}
}
