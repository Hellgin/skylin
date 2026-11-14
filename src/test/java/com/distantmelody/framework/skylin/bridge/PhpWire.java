package com.distantmelody.framework.skylin.bridge;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * What the PHP side of the bridge does (framework/php/skylin/bridge/java.php and BridgeConnection.php), written
 * independently of the production code in this package so that the tests compare two implementations of the protocol.
 * Like PHP, it works on bytes: a "length" is always the number of bytes (strlen), a string is a byte string.
 */
final class PhpWire
{
	private PhpWire() {}

	static byte[] utf8(String s)
	{
		return s.getBytes(StandardCharsets.UTF_8);
	}

	static byte[] concat(byte[]... parts)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (byte[] p:parts)
		{
			out.writeBytes(p);
		}
		return out.toByteArray();
	}

	/** java.php _objectToString(). Strings, Integers, Booleans, null and Object[] / List (arrays). */
	static byte[] encode(Object a)
	{
		if (a == null)
		{
			return utf8("n");
		}
		if (a instanceof String s)
		{
			return concat(utf8("s"), utf8(s));
		}
		if (a instanceof Integer i)
		{
			return utf8("i" + i);
		}
		if (a instanceof Boolean b)
		{
			return utf8(b ? "bT" : "bF");
		}
		if (a instanceof byte[] raw)
		{
			return concat(utf8("s"), raw);//a PHP string that is not text
		}
		if (a instanceof Object[] array)
		{
			return encode(Arrays.asList(array));
		}
		if (a instanceof List<?> list)
		{
			if (list.isEmpty())
			{
				return utf8("a0");
			}
			ByteArrayOutputStream sizes = new ByteArrayOutputStream();
			ByteArrayOutputStream data = new ByteArrayOutputStream();
			for (Object o:list)
			{
				byte[] str = encode(o);
				data.writeBytes(str);
				sizes.writeBytes(utf8(str.length + ","));
			}
			return concat(utf8("a" + list.size() + ","), sizes.toByteArray(), data.toByteArray());
		}
		throw new IllegalArgumentException("unsupported " + a.getClass());
	}

	/** java.php _stringToObject(): strings stay Strings (decoded as UTF-8 for comparison), arrays become Lists. */
	static Object decode(byte[] str)
	{
		if (str.length == 0)
		{
			return null;
		}
		char t = (char) str[0];
		byte[] data = Arrays.copyOfRange(str, 1, str.length);
		switch (t)
		{
			case 'a':
			{
				if (new String(data, StandardCharsets.US_ASCII).equals("0"))
				{
					return new ArrayList<Object>();
				}
				int comma = indexOf(data, (byte) ',', 0);
				int size = Integer.parseInt(new String(data, 0, comma, StandardCharsets.US_ASCII));
				int pos = comma + 1;
				int[] lengths = new int[size];
				for (int i = 0; i < size; i++)
				{
					comma = indexOf(data, (byte) ',', pos);
					lengths[i] = Integer.parseInt(new String(data, pos, comma - pos, StandardCharsets.US_ASCII));
					pos = comma + 1;
				}
				List<Object> ret = new ArrayList<Object>();
				for (int i = 0; i < size; i++)
				{
					ret.add(decode(Arrays.copyOfRange(data, pos, pos + lengths[i])));
					pos += lengths[i];
				}
				return ret;
			}
			case 's':
				return new String(data, StandardCharsets.UTF_8);
			case 'i':
				return Integer.parseInt(new String(data, StandardCharsets.US_ASCII));
			case 'b':
				return data.length == 1 && data[0] == 'T';
			default:
				throw new IllegalArgumentException("unknown type " + t);
		}
	}

	private static int indexOf(byte[] data, byte b, int from)
	{
		for (int i = from; i < data.length; i++)
		{
			if (data[i] == b)
			{
				return i;
			}
		}
		throw new IllegalArgumentException("no separator");
	}

	/** java.php __call(): the payload of command 2 (without the command's own size). */
	static byte[] call(int objectId, String method, Object... arguments)
	{
		ByteArrayOutputStream offsets = new ByteArrayOutputStream();
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		for (Object a:arguments)
		{
			byte[] aData = encode(a);
			data.writeBytes(aData);
			offsets.writeBytes(utf8(aData.length + ","));
		}
		return concat(utf8("2," + objectId + "," + method + "," + arguments.length + ","), offsets.toByteArray(), data.toByteArray());
	}

	/** BridgeConnection.php: 7 decimal digits of strlen, then the payload. */
	static byte[] frame(byte[] payload)
	{
		return concat(utf8(String.format("%07d", payload.length)), payload);
	}
}
