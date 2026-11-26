package com.distantmelody.framework.skylin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * {@link AM#error(SQLException)} shows the browser only messages the database raised for the user on purpose. The
 * driver's text of any other error stays in the log.
 */
class AMDatabaseErrorTest
{
	private static final String GENERIC = "displaymsg,fatal,A database error occurred \\(reference [0-9a-f]{8}\\)\\.";

	/** An AM whose datasource is of the given type. */
	static class TypedAM extends AM
	{
		private final String type;

		TypedAM(String type)
		{
			this.type = type;
		}

		@Override
		protected String getDataSourceType()
		{
			return type;
		}
	}

	private static String onlyMessage(AM am)
	{
		List<String> messages = am.getMessages();
		assertEquals(1, messages.size(), messages.toString());
		return messages.get(0);
	}

	@Test
	void aMysqlErrorShowsNoDriverText()
	{
		AM am = new TypedAM("mysql");
		am.error(new SQLException("Duplicate entry 'jane@example.com' for key 'USERS.EMAIL'", "23000", 1062));
		String message = onlyMessage(am);
		assertTrue(message.matches(GENERIC), message);
		assertFalse(message.contains("jane"), message);
	}

	@Test
	void aPostgresConstraintErrorShowsNoDriverText()
	{
		AM am = new TypedAM("postgresql");
		am.error(new SQLException("ERROR: duplicate key value violates unique constraint \"users_email\"\n  Detail: Key (email)=(jane@example.com) already exists.", "23505"));
		String message = onlyMessage(am);
		assertTrue(message.matches(GENERIC), message);
		assertFalse(message.contains("users_email"), message);
	}

	@Test
	void aPostgresRaiseExceptionShowsItsMessage()
	{
		AM am = new TypedAM("postgresql");
		am.error(new SQLException("ERROR: The batch is already closed.\n  Where: PL/pgSQL function close_batch() line 4 at RAISE", "P0001"));
		String message = onlyMessage(am);
		assertTrue(message.startsWith("displaymsg,error,The batch is already closed."), message);
		assertFalse(message.contains("PL/pgSQL"), message);
	}

	@Test
	void oracleApplicationErrorsShowTheirMessageAtTheirLevel()
	{
		AM am = new TypedAM("oracle");
		am.error(new SQLException("ORA-20900: Not allowed.\nORA-06512: at line 1", "72000", 20900));
		am.error(new SQLException("ORA-20901: Check the date.\nORA-06512: at line 1", "72000", 20901));
		am.error(new SQLException("ORA-20902: Saved.\nORA-06512: at line 1", "72000", 20902));
		assertEquals(List.of("displaymsg,error, Not allowed.", "displaymsg,warn, Check the date.", "displaymsg,info, Saved."),
			am.getMessages());
	}

	@Test
	void anyOtherOracleErrorShowsNoDriverText()
	{
		AM am = new TypedAM("oracle");
		am.error(new SQLException("ORA-00001: unique constraint (APP.USERS_EMAIL) violated", "23000", 1));
		String message = onlyMessage(am);
		assertTrue(message.matches(GENERIC), message);
		assertFalse(message.contains("USERS_EMAIL"), message);
	}

	@Test
	void anUnknownDatasourceTypeShowsNoDriverText()
	{
		AM am = new TypedAM("");
		am.error(new SQLException("ORA-20900: looks deliberate\n", "72000", 20900));
		assertTrue(onlyMessage(am).matches(GENERIC));
	}

	@Test
	void eachErrorGetsItsOwnReference()
	{
		AM am = new AM();
		String first = am.logDatabaseError("first", new SQLException("a"));
		String second = am.logDatabaseError("second", new SQLException("b"));
		assertTrue(first.matches("A database error occurred \\(reference [0-9a-f]{8}\\)\\."), first);
		assertNotEquals(first, second);
	}
}
