package com.distantmelody.framework.skylin.bridge;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.condition.EnabledIf;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

/** The bridge protocol over a Unix domain socket, the transport of production (junixsocket, as in BridgeListener). */
@EnabledIf("unixSocketsSupported")
class BridgeProtocolUnixSocketTest extends BridgeProtocolTestBase
{
	static boolean unixSocketsSupported()
	{
		return AFUNIXSocket.isSupported();
	}

	@Override
	Socket[] connectedPair(Path dir) throws IOException
	{
		//a short path: AF_UNIX allows 107 bytes
		Path socketFile = Files.createTempFile("sky", ".sock");
		Files.delete(socketFile);
		try (AFUNIXServerSocket server = AFUNIXServerSocket.newInstance())
		{
			server.bind(AFUNIXSocketAddress.of(socketFile));
			Socket client = AFUNIXSocket.connectTo(AFUNIXSocketAddress.of(socketFile));
			Socket accepted = server.accept();
			return new Socket[] {accepted, client};
		}
		finally
		{
			Files.deleteIfExists(socketFile);
		}
	}
}
