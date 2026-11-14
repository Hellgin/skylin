package com.distantmelody.framework.skylin.bridge;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;

/** The bridge protocol over TCP, the fallback transport. */
class BridgeProtocolTcpTest extends BridgeProtocolTestBase
{
	@Override
	Socket[] connectedPair(Path dir) throws IOException
	{
		try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress()))
		{
			Socket client = new Socket(InetAddress.getLoopbackAddress(), server.getLocalPort());
			client.setTcpNoDelay(true);
			Socket accepted = server.accept();
			accepted.setTcpNoDelay(true);
			return new Socket[] {accepted, client};
		}
	}
}
