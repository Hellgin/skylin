package com.distantmelody.framework.skylin.bridge;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Builds a message of the PHP bridge protocol. The counterpart of {@link ByteReader}: text is encoded as UTF-8 exactly
 * once, when it is appended, and every length that goes into a message is taken from the bytes ({@link #size()}),
 * never from a String.
 */
public final class ByteWriter
{
	private final ByteArrayOutputStream out = new ByteArrayOutputStream();

	/** Appends ASCII only (type markers, separators, numbers). */
	public ByteWriter ascii(CharSequence text)
	{
		for (int i = 0; i < text.length(); i++)
		{
			char c = text.charAt(i);
			if (c > 0x7F)
			{
				throw new IllegalArgumentException("not ASCII: U+" + Integer.toHexString(c));
			}
			out.write(c);
		}
		return this;
	}

	public ByteWriter ascii(long number)
	{
		return ascii(Long.toString(number));
	}

	/** Appends text as UTF-8. */
	public ByteWriter utf8(String text)
	{
		out.writeBytes(text.getBytes(StandardCharsets.UTF_8));
		return this;
	}

	public ByteWriter bytes(byte[] data)
	{
		out.writeBytes(data);
		return this;
	}

	/** The number of bytes written so far. */
	public int size()
	{
		return out.size();
	}

	public byte[] toByteArray()
	{
		return out.toByteArray();
	}
}
