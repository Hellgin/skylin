package com.distantmelody.framework.skylin.bridge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class ByteReaderTest
{
	private static byte[] utf8(String s)
	{
		return s.getBytes(StandardCharsets.UTF_8);
	}

	@Test
	void tokensAndIntegersAreCutAtTheSeparatorAndTheRestStaysBytes()
	{
		ByteReader r = new ByteReader(utf8("12,m\u00e9thode,3,\u20ac\uD83D\uDE00"));
		assertEquals(12, r.integer());
		assertEquals("m\u00e9thode", r.token(','));
		assertEquals(3, r.integer());
		assertEquals(3 + 4, r.remaining());
		assertEquals("\u20ac\uD83D\uDE00", r.string());
		assertFalse(r.hasRemaining());
	}

	@Test
	void tokenAtTheEndTakesTheRest()
	{
		ByteReader r = new ByteReader(utf8("abc"));
		assertEquals("abc", r.token(','));
		assertEquals(-1, r.peek());
		assertEquals("", r.token(','));
	}

	@Test
	void sliceCutsByBytesAndDoesNotCopy()
	{
		ByteReader r = new ByteReader(utf8("\u20ac\uD83D\uDE00xyz"));
		ByteReader euro = r.slice(3);
		ByteReader emoji = r.slice(4);
		assertEquals("\u20ac", euro.string());
		assertEquals("\uD83D\uDE00", emoji.string());
		assertEquals("xyz", r.string());
		assertThrows(IllegalArgumentException.class, () -> new ByteReader(utf8("ab")).slice(3));
	}

	@Test
	void windowOfALargerBuffer()
	{
		byte[] data = utf8("..\u00e9..");
		ByteReader r = new ByteReader(data, 2, 2);
		assertEquals("\u00e9", r.string());
	}

	@Test
	void restBytesAreCopiedUnchanged()
	{
		byte[] raw = {(byte) 0xFF, 0, (byte) ',', (byte) 0xC3, (byte) 0x28, (byte) 0xF0};
		ByteReader r = new ByteReader(raw);
		assertArrayEquals(raw, r.bytes());
		assertEquals(0, r.remaining());
	}

	@Test
	void findLooksForABytePattern()
	{
		ByteReader r = new ByteReader(utf8("a\u00e9?b%3Fc"));
		assertEquals(3, r.find(utf8("?")));
		assertEquals(5, r.find(utf8("%3F")));
		assertEquals(-1, r.find(utf8("x")));
		r.skip(2);
		assertEquals(1, r.find(utf8("?")));
	}

	@Test
	void writerEncodesUtf8OnceAndCountsBytes()
	{
		ByteWriter w = new ByteWriter().ascii("s").utf8("\u20ac\uD83D\uDE00").ascii(12).ascii(",");
		assertEquals(1 + 3 + 4 + 2 + 1, w.size());
		assertArrayEquals(utf8("s\u20ac\uD83D\uDE0012,"), w.toByteArray());
		assertThrows(IllegalArgumentException.class, () -> new ByteWriter().ascii("\u00e9"));
	}
}
