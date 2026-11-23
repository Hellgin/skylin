package com.distantmelody.framework.skylin.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes to the legacy {@code LOG_PATH} file and to the original stream. Each complete line is also passed to SLF4J.
 * A line that arrived from an SLF4J appender is not passed back.
 */
public class SplitPrintStream extends PrintStream {

	private static final Logger STDOUT = LoggerFactory.getLogger("skylin.stdout");
	private static final Logger STDERR = LoggerFactory.getLogger("skylin.stderr");
	private static final ThreadLocal<Boolean> forwarding = ThreadLocal.withInitial(() -> Boolean.FALSE);

	private final PrintStream second;
	private final boolean errorStream;
	private final ByteArrayOutputStream pending = new ByteArrayOutputStream();

	public SplitPrintStream(OutputStream main, PrintStream second) {
		this(main, second, false);
	}

	public SplitPrintStream(OutputStream main, PrintStream second, boolean errorStream) {
		super(main);
		this.second = second;
		this.errorStream = errorStream;
		writeByteToPrimary('\n');
	}

	@Override
	public void flush() {
		super.flush();
		second.flush();
	}

	@Override
	public void write(byte[] buf, int off, int len) {
		for (int i = off; i < off + len; i++)
		{
			writeByteToPrimary(buf[i]);
			capture(buf[i]);
		}
		second.write(buf, off, len);
	}

	@Override
	public void write(int b) {
		writeByteToPrimary(b);
		capture(b);
		second.write(b);
	}

	private void writeByteToPrimary(int b)
	{
		super.write(b);
		if (b == '\n')
		{
			String text = System.currentTimeMillis() + ":";
			for (int i = 0; i < text.length(); i++)
			{
				super.write(text.charAt(i));
			}
		}
	}

	@Override
	public void write(byte[] b) throws IOException {
		for (byte bb : b)
		{
			writeByteToPrimary(bb);
			capture(bb);
		}
		second.write(b);
	}

	private void capture(int b)
	{
		String text = null;
		synchronized (pending)
		{
			if (b == '\r')
			{
				return;
			}
			if (b != '\n')
			{
				pending.write(b);
				return;
			}
			if (pending.size() == 0)
			{
				return;
			}
			text = pending.toString(charset());
			pending.reset();
		}
		forward(text);
	}

	private void forward(String text)
	{
		if (forwarding.get())
		{
			return;
		}
		forwarding.set(Boolean.TRUE);
		try
		{
			if (errorStream)
			{
				STDERR.error(text);
			}
			else
			{
				STDOUT.info(text);
			}
		}
		catch (Throwable ignored)
		{
		}
		finally
		{
			forwarding.set(Boolean.FALSE);
		}
	}
}
