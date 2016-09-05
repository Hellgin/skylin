package skylin.services;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletOutputStream;

public class SkylinServletOutputStream extends ServletOutputStream//minimal implementation. Expand later.
{
	ArrayList<byte[]> data = new ArrayList<byte[]>();
	byte[] current = new byte[BLOCK_SIZE];
	int position;
	
	final static int BLOCK_SIZE = 1024;
	
	@Override
	public void write(int b) throws IOException 
	{
		if (position == BLOCK_SIZE)
		{
			data.add(current);
			current = new byte[BLOCK_SIZE];
			current[0] = (byte)b;
			position = 1;
		}
		else
		{
			current[position] = (byte)b;
			position+=1;
		}
	}
	
	public void write(byte[] b)
	{
		int readPos = 0;
		while (position + b.length - readPos > BLOCK_SIZE)
		{
			System.arraycopy(b, readPos, current, position, BLOCK_SIZE - position);
			readPos += BLOCK_SIZE - position;
			position = 0;
			data.add(current);
			current = new byte[BLOCK_SIZE];
		}
		System.arraycopy(b, readPos, current, position, b.length - readPos);
		position += b.length - readPos;
	}
	
	public byte[] getData()
	{
		byte[] all = new byte[data.size() * BLOCK_SIZE + position];
		for (int i = 0; i < data.size(); i++)
		{
			System.arraycopy(data.get(i), 0, all, BLOCK_SIZE*i, BLOCK_SIZE);
		}
		System.arraycopy(current, 0, all, data.size() * BLOCK_SIZE, position);
		return all;
	}

}
