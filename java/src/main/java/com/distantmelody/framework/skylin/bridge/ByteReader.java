package com.distantmelody.framework.skylin.bridge;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A read cursor over a window of a byte array, used to parse the PHP bridge protocol.
 * <p>
 * Every length in the protocol is a length in BYTES (PHP's strlen), and the text in it is UTF-8. A parser therefore
 * must never convert the data to a String before it has been cut into its fields: that is what this class is for.
 * It slices the buffer without copying ({@link #slice(int)}) and converts to a String only at a leaf
 * ({@link #string()}, {@link #token(char)}), always as UTF-8.
 * <p>
 * Structure and numbers of the protocol are ASCII, so {@link #token(char)} and {@link #integer()} read them as such.
 * The cursor only moves forward; {@link #slice(int)} hands out an independent cursor on the same bytes.
 */
public final class ByteReader
{
	private final byte[] buffer;
	private int position;
	private final int end;

	public ByteReader(byte[] data)
	{
		this(data, 0, data.length);
	}

	/** A reader over {@code data[offset, offset + length)}. */
	public ByteReader(byte[] data, int offset, int length)
	{
		if (offset < 0 || length < 0 || offset + length > data.length)
		{
			throw new IllegalArgumentException("window " + offset + "+" + length + " outside of " + data.length + " bytes");
		}
		this.buffer = data;
		this.position = offset;
		this.end = offset + length;
	}

	public int remaining()
	{
		return end - position;
	}

	public boolean hasRemaining()
	{
		return position < end;
	}

	/** The next byte as an unsigned value (0 - 255), without consuming it, or -1 at the end. */
	public int peek()
	{
		return position < end ? buffer[position] & 0xFF : -1;
	}

	/** The next byte as an ASCII character; the protocol's one character type markers. */
	public char readChar()
	{
		if (position >= end)
		{
			throw new IllegalStateException("no more bytes");
		}
		return (char) (buffer[position++] & 0x7F);
	}

	/**
	 * Reads up to the next {@code separator} (an ASCII character) and consumes the separator. At the end of the data
	 * the rest is returned. The field is decoded as UTF-8.
	 */
	public String token(char separator)
	{
		int i = indexOf((byte) separator);
		if (i < 0)
		{
			return string();
		}
		String token = new String(buffer, position, i - position, StandardCharsets.UTF_8);
		position = i + 1;
		return token;
	}

	/** A decimal number terminated by a comma (or by the end of the data). */
	public int integer()
	{
		return Integer.parseInt(token(','));
	}

	/**
	 * Cuts the next {@code length} bytes off as an independent reader and moves behind them. This is how the
	 * length-prefixed fields of the protocol are read; no bytes are copied.
	 */
	public ByteReader slice(int length)
	{
		if (length < 0 || length > remaining())
		{
			throw new IllegalArgumentException("field of " + length + " bytes, but only " + remaining() + " bytes left");
		}
		ByteReader slice = new ByteReader(buffer, position, length);
		position += length;
		return slice;
	}

	/** Everything that is left, as an independent reader. Consumes it. */
	public ByteReader rest()
	{
		return slice(remaining());
	}

	/** Everything that is left, decoded as UTF-8. Consumes it. */
	public String string()
	{
		String s = new String(buffer, position, remaining(), StandardCharsets.UTF_8);
		position = end;
		return s;
	}

	/** Everything that is left, copied. Consumes it. For payloads that must stay byte-exact (session data, bodies). */
	public byte[] bytes()
	{
		byte[] copy = Arrays.copyOfRange(buffer, position, end);
		position = end;
		return copy;
	}

	/**
	 * Position of the first occurrence of {@code needle} in the rest of the data, relative to the current
	 * position, or -1. Nothing is consumed.
	 */
	public int find(byte[] needle)
	{
		outer:
		for (int i = position; i <= end - needle.length; i++)
		{
			for (int j = 0; j < needle.length; j++)
			{
				if (buffer[i + j] != needle[j])
				{
					continue outer;
				}
			}
			return i - position;
		}
		return -1;
	}

	/** Moves forward without reading. */
	public void skip(int length)
	{
		if (length < 0 || length > remaining())
		{
			throw new IllegalArgumentException("cannot skip " + length + " of " + remaining() + " bytes");
		}
		position += length;
	}

	private int indexOf(byte b)
	{
		for (int i = position; i < end; i++)
		{
			if (buffer[i] == b)
			{
				return i;
			}
		}
		return -1;
	}
}
