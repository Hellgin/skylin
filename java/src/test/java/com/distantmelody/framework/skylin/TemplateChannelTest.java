package com.distantmelody.framework.skylin;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.distantmelody.framework.skylin.TemplateChannel.Failure;
import com.distantmelody.framework.skylin.TemplateChannel.Target;
import com.distantmelody.framework.skylin.TemplateChannel.UnresolvableChannel;
import com.distantmelody.framework.skylin.bridge.BridgeSession;
import com.distantmelody.library.json.J;

/**
 * A template's channel names the AM that receives its messages, its application and id, and the row it rendered on,
 * and comes back from the browser as exactly that template and row, or not at all.
 */
class TemplateChannelTest
{
	public static class Cards extends View
	{
		public Cards(AM am)
		{
			super(am);
			setColumnNames("name");
			setColumnTypes(String.class);
		}
	}

	/** Rows that handle their own messages. */
	public static class Alerts extends Cards
	{
		public Alerts(AM am)
		{
			super(am);
			setRowType(AlertRow.class);
		}
	}

	public static class AlertRow extends ViewRow
	{
		public AlertRow(View view)
		{
			super(view);
		}

		@Override
		public byte[] receiveChannelMessage(J message, int applicationId, String templateId)
		{
			return ("row " + applicationId + " " + templateId + " " + message).getBytes(StandardCharsets.UTF_8);
		}
	}

	public static class Notes extends Cards
	{
		public Notes(AM am)
		{
			super(am);
		}
	}

	/** Root views in the order they are created: Cards is 0 and Alerts 1, though Alerts comes first by name. */
	public static class ChannelAM extends AM
	{
		final List<String> received = new ArrayList<String>();

		public ChannelAM()
		{
			new Cards(this);
			new Alerts(this);
		}

		@Override
		public byte[] receiveChannelMessage(J message, int applicationId, String templateId, ViewRow row)
		{
			received.add(applicationId + " " + templateId + " " + (row == null ? "-" : row.getLinkId()) + " " + message);
			return null;
		}

		@Override
		public byte[] receiveChannelBinaryMessage(byte[] message, int applicationId, String templateId, ViewRow row)
		{
			received.add("binary " + applicationId + " " + templateId + " " + (row == null ? "-" : row.getLinkId()) + " " + message.length);
			return null;
		}
	}

	private BridgeSession session;
	private ChannelAM am;
	private Cards cards;
	private ViewRow first;
	private ViewRow second;
	private ViewRow note;

	@BeforeEach
	void setUp()
	{
		session = new BridgeSession("templatechanneltest0123456789abc", null);
		am = am(3);
		cards = (Cards) am.getView("Cards");
		first = cards.newRow();
		second = cards.newRow();
		note = linkedView(second).newRow();
	}

	/** The first AM of application {@code application}. */
	private ChannelAM am(int application)
	{
		String id = session.instantiateByName(ChannelAM.class.getName());
		ChannelAM a = (ChannelAM) session.getObject(Integer.parseInt(id));
		a.setObjectId(Integer.parseInt(id));
		a.setFrontEndApplicationId(application);
		a.setTopFrontEndApplicationId(1);
		a.setDataSource("none");
		return a;
	}

	/** A Notes view linked under the row, as ViewRow.getLinkedView links one, without running a query. */
	private static Notes linkedView(ViewRow row)
	{
		Notes notes = new Notes(row.getAM());
		notes.owner = row;
		notes.initInternal();
		notes.linkId = 1;
		ViewLink link = new ViewLink();
		link.linkedView = notes;
		row.viewLinks = new ArrayList<ViewLink>();
		row.viewLinks.add(link);
		return notes;
	}

	private Target resolve(String channel) throws UnresolvableChannel
	{
		return TemplateChannel.resolve(session, channel);
	}

	private Failure failure(String channel)
	{
		return assertThrows(UnresolvableChannel.class, () -> resolve(channel), channel).failure;
	}

	@Test
	void aTemplateWithoutARowGetsItsAmApplicationAndId() throws Exception
	{
		String channel = am.channel("chat");
		assertEquals(am.getObjectId() + "x3xchat", channel);
		assertEquals(channel, am.channel(3, "chat", null));
		Target target = resolve(channel);
		assertSame(am, target.am());
		assertEquals(3, target.applicationId());
		assertEquals("chat", target.templateId());
		assertNull(target.row());
	}

	@Test
	void aTemplateOnARowGetsThatRowBack() throws Exception
	{
		String channel = am.channel(3, "card", second);
		assertEquals(am.getObjectId() + "x3xcardx0x2", channel);
		Target target = resolve(channel);
		assertSame(am, target.am());
		assertEquals("card", target.templateId());
		assertSame(second, target.row());
		assertSame(first, resolve(am.channel(3, "card", first)).row());
	}

	@Test
	void aRowOfALinkedViewIsFoundThroughItsParentRow() throws Exception
	{
		String channel = am.channel(3, "7", note);
		assertEquals(am.getObjectId() + "x3x7x0x2x1x1", channel);
		assertSame(note, resolve(channel).row());
	}

	@Test
	void onARowTheChannelNamesTheRowsAmWhicheverAmBuildsIt() throws Exception
	{
		ChannelAM parent = am(2);
		ViewRow parentRow = ((Cards) parent.getView("Cards")).newRow();
		String channel = am.channel(3, "child", parentRow);
		assertEquals(parent.getObjectId() + "x3xchildx0x1", channel);
		assertEquals(channel, parent.channel(3, "child", parentRow));
		Target target = resolve(channel);
		assertSame(parent, target.am());
		assertEquals(3, target.applicationId(), "the template's application, not the row AM's");
		assertSame(parentRow, target.row());
	}

	@Test
	void templatesOfTwoApplicationsOnOneRowHaveTheirOwnChannels() throws Exception
	{
		String inThree = am.channel(3, "5", first);
		String inFour = am.channel(4, "5", first);
		assertNotEquals(inThree, inFour);
		assertEquals(3, resolve(inThree).applicationId());
		assertEquals(4, resolve(inFour).applicationId());
		assertSame(first, resolve(inFour).row());
	}

	@Test
	void aMessageGoesToTheRowWhichPassesItToItsAm() throws Exception
	{
		J message = J.fromString("{\"a\":1}");
		resolve(am.channel("chat")).receive(message);
		resolve(am.channel(4, "card", second)).receive(message);
		resolve(am.channel(4, "card", second)).receiveBinary(new byte[3]);
		assertEquals(List.of("3 chat - " + message, "4 card 2 " + message, "binary 4 card 2 3"), am.received);

		ViewRow alert = am.getView("Alerts").newRow();
		byte[] answer = resolve(am.channel(4, "warn", alert)).receive(message);
		assertArrayEquals(("row 4 warn " + message).getBytes(StandardCharsets.UTF_8), answer, "a row that overrides the handler");
		assertEquals(3, am.received.size(), "and the AM does not see it");
	}

	@Test
	void aRowThatIsGoneDropsTheMessage()
	{
		String channel = am.channel(3, "card", first);
		cards.rows.remove(first);
		assertEquals(Failure.STALE_ROW, failure(channel));
		String inLinkedView = am.channel(3, "note", note);
		second.viewLinks.clear();
		assertEquals(Failure.STALE_ROW, failure(inLinkedView));
	}

	@Test
	void onlyIdsTheMessageFormatAndThePageCanCarryMakeAChannel()
	{
		for (String id : new String[] {"a,b", "box", "Xy", "a'b", "a b", "", null, "a".repeat(65)})
		{
			assertThrows(SkylinException.class, () -> am.channel(id), String.valueOf(id));
		}
		am.channel("Game_2-b");
		am.channel("a".repeat(64));
		assertThrows(SkylinException.class, () -> am.channel(-1, "card", first), "an application id");
		AM noApplication = new AM();
		assertThrows(SkylinException.class, () -> noApplication.channel("chat"), "an AM outside an application");
	}

	@Test
	void aRowWhoseRootViewIsNotOneOfItsAmsViewsHasNoChannel()
	{
		ViewRow loose = new Cards(am).newRow();
		assertThrows(SkylinException.class, () -> am.channel(3, "card", loose));
	}

	@Test
	void anAmTheSessionDoesNotHaveIsUnknown()
	{
		String nonAm = session.instantiateByName(StringBuilder.class.getName());
		assertEquals(Failure.UNKNOWN_AM, failure("999x3xchat"));
		assertEquals(Failure.UNKNOWN_AM, failure(nonAm + "x3xchat"));
		assertEquals(Failure.UNKNOWN_AM, failure("999x3xcardx0x1"));
	}

	@Test
	void aForgedOrDamagedChannelIsMalformed()
	{
		int a = am.getObjectId();

		assertEquals(Failure.MALFORMED, failure(null));
		assertEquals(Failure.MALFORMED, failure(""));
		assertEquals(Failure.MALFORMED, failure(a + ""));
		assertEquals(Failure.MALFORMED, failure(a + "xchat"), "no application");
		assertEquals(Failure.MALFORMED, failure(a + "x3xcardx0"));
		assertEquals(Failure.MALFORMED, failure("abcx3xchat"));
		assertEquals(Failure.MALFORMED, failure(a + "xthreexchat"));
		assertEquals(Failure.MALFORMED, failure(a + "x-1xchat"));
		assertEquals(Failure.MALFORMED, failure(a + "x3xa,b"));
		assertEquals(Failure.MALFORMED, failure(a + "x4xchat"), "without a row, an application the AM is not in");
		assertEquals(Failure.MALFORMED, failure(a + "x3xcardx2x1"), "no such root view");
		assertEquals(Failure.MALFORMED, failure(a + "x3xcardx-1x1"));
		assertEquals(Failure.MALFORMED, failure(a + "x3xcardx0x2x1"), "ends at a view, not a row");
		assertEquals(Failure.MALFORMED, failure(a + "x3xcardx0x2xx1"));
		assertEquals(Failure.MALFORMED, failure(a + "x3xchat" + "1".repeat(TemplateChannel.MAX_LENGTH)), "too long");
	}
}
