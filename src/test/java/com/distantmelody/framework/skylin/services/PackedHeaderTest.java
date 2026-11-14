package com.distantmelody.framework.skylin.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class PackedHeaderTest
{
	/** rest.php: explode(',', $all, 2) gives the size and the rest; the header block is cut with substr() (bytes). */
	private static List<String> unpackLikePhp(byte[] all)
	{
		int comma = 0;
		while (all[comma] != ',') comma++;
		int blockSize = Integer.parseInt(new String(all, 0, comma, StandardCharsets.US_ASCII));
		byte[] block = Arrays.copyOfRange(all, comma + 1, comma + 1 + blockSize);
		comma = 0;
		while (block[comma] != ',') comma++;
		int count = Integer.parseInt(new String(block, 0, comma, StandardCharsets.US_ASCII));
		int pos = comma + 1;
		int[] sizes = new int[count];
		for (int i = 0; i < count; i++)
		{
			int start = pos;
			while (block[pos] != ',') pos++;
			sizes[i] = Integer.parseInt(new String(block, start, pos - start, StandardCharsets.US_ASCII));
			pos++;
		}
		List<String> headers = new ArrayList<String>();
		for (int size:sizes)
		{
			headers.add(new String(block, pos, size, StandardCharsets.UTF_8));
			pos += size;
		}
		assertEquals(block.length, pos, "the header block is used up exactly");
		return headers;
	}

	@Test
	void headerSizesAreByteLengths()
	{
		SkylinHttpServletResponse response = new SkylinHttpServletResponse();
		response.setHeader("Content-Type", "text/html; charset=UTF-8");
		response.setHeader("X-Name", "\u00e9\u20ac\u65e5\u672c\u8a9e\uD83D\uDE00,1,2");
		response.addHeader("Set-Cookie", "a=\u00e9");
		response.addHeader("Set-Cookie", "b=\uD83D\uDE00");
		byte[] packed = response.getPackedHeaderData();
		byte[] body = "\u20ac".getBytes(StandardCharsets.UTF_8);
		byte[] all = new byte[packed.length + body.length];
		System.arraycopy(packed, 0, all, 0, packed.length);
		System.arraycopy(body, 0, all, packed.length, body.length);
		List<String> headers = unpackLikePhp(all);
		assertEquals(4, headers.size());
		assertTrue(headers.contains("Content-Type: text/html; charset=UTF-8"));
		assertTrue(headers.contains("X-Name: \u00e9\u20ac\u65e5\u672c\u8a9e\uD83D\uDE00,1,2"));
		assertTrue(headers.contains("Set-Cookie: a=\u00e9"));
		assertTrue(headers.contains("Set-Cookie: b=\uD83D\uDE00"));
	}
}
