package com.distantmelody.framework.skylin.bridge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** The Java object that the bridge tests call, the way PHP calls the Java classes of an application. */
public class EchoTarget
{
	public String echo(String s)
	{
		return s;
	}

	public String concat3(String a, String b, String c)
	{
		return a + "|" + b + "|" + c;
	}

	public Object[] echoArray(Object[] a)
	{
		return a;
	}

	public String describe(Object[] a, String s1, String s2)
	{
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < a.length; i++)
		{
			b.append(i == 0 ? "" : "+").append(a[i]);
		}
		return b + "/" + s1 + "/" + s2;
	}

	/** Length in UTF-16 code units, what Java counts (and what JavaScript counts). */
	public Integer chars(String s)
	{
		return s.length();
	}

	public Integer codePoints(String s)
	{
		return s.codePointCount(0, s.length());
	}

	public Integer utf8Length(String s)
	{
		return s.getBytes(StandardCharsets.UTF_8).length;
	}

	public String[] samples()
	{
		return new String[] {"a", "\u00e9", "\u20ac", "\u65e5\u672c\u8a9e", "\uD83D\uDE00", "", "a,1,b", "3,5,s7"};
	}

	public Object[] nested()
	{
		return new Object[] {"\u00e9", new String[] {"\u65e5\u672c\u8a9e", "x"}, 5, true, ""};
	}

	public String big(Integer n)
	{
		return "\u20ac".repeat(n);
	}

	public String nothing()
	{
		return null;
	}

	/** Like AM.eval: asks PHP, in the middle of this call, to run code (command 1 of the reply). */
	public String callback(String code) throws IOException
	{
		BridgeConnection.getBridgeConnection().sendAndReceive(("1" + 42 + "," + code).getBytes(StandardCharsets.UTF_8));
		return "done";
	}
}
