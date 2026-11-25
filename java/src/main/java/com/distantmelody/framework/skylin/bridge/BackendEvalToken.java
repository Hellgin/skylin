package com.distantmelody.framework.skylin.bridge;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * The secret that proves a request to {@code skylin/websocket/BackendEval.php} comes from this JVM ({@code AM.eval}).
 * It is made once per JVM start and never leaves the server: the JVM puts it first in the body of its request, and
 * {@code BackendEval.php} asks the JVM over the bridge (command {@code g}) whether it matches before it uses anything
 * else in the request.
 */
public final class BackendEvalToken
{
	private static final byte[] TOKEN;
	static
	{
		byte[] random = new byte[32];
		new SecureRandom().nextBytes(random);
		TOKEN = HexFormat.of().formatHex(random).getBytes(StandardCharsets.US_ASCII);
	}

	private BackendEvalToken()
	{
	}

	public static String get()
	{
		return new String(TOKEN, StandardCharsets.US_ASCII);
	}

	/** Compares in constant time. */
	public static boolean matches(byte[] candidate)
	{
		return candidate != null && MessageDigest.isEqual(TOKEN, candidate);
	}
}
