package skylin.services;

import java.io.IOException;

import javax.servlet.ServletInputStream;

public class SkylinServletInputStream extends ServletInputStream//minimal implementation. Expand later.
{

	int location;
	byte[] data;
	
	public SkylinServletInputStream(byte[] data)
	{
		this.data = data;
	}
	
	@Override
	public int read() throws IOException {
		if (location == data.length)
		{
			return -1;
		}
		location++;
		return data[location - 1];
	}
	
	public int read(byte[] b)
	{
		int canRead = data.length - location;
		if (canRead == 0)
		{
			return -1;
		}
		canRead = Math.min(canRead, b.length);
		System.arraycopy(data, location, b, 0, canRead);
		location+=canRead;
		return canRead;
	}
	
	public int read(byte[] b, int off, int len)
	{
		int canRead = data.length - location;
		if (canRead == 0)
		{
			return -1;
		}
		canRead = Math.min(Math.min(canRead, len),b.length-off);
		System.arraycopy(data, location, b, off, canRead);
		location+=canRead;
		return canRead;
	}
	
	public int available()
	{
		return data.length - location;
	}

}
