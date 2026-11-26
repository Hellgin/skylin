package com.distantmelody.framework.skylin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.distantmelody.framework.skylin.TemplateChannel.Failure;
import com.distantmelody.framework.skylin.TemplateChannel.Target;
import com.distantmelody.framework.skylin.TemplateChannel.UnresolvableChannel;
import com.distantmelody.framework.skylin.bridge.BridgeSession;

/**
 * A template's channel names its AM, its id and the row it rendered on, and comes back from the browser as exactly
 * that template and row, or not at all.
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

	public static class Alerts extends Cards
	{
		public Alerts(AM am)
		{
			super(am);
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
		public ChannelAM()
		{
			new Cards(this);
			new Alerts(this);
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
		am = am(1);
		cards = (Cards) am.getView("Cards");
		first = cards.newRow();
		second = cards.newRow();
		note = linkedView(second).newRow();
	}

	private ChannelAM am(int topApplication)
	{
		String id = session.instantiateByName(ChannelAM.class.getName());
		ChannelAM a = (ChannelAM) session.getObject(Integer.parseInt(id));
		a.setObjectId(Integer.parseInt(id));
		a.setTopFrontEndApplicationId(topApplication);
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
	void aTemplateWithoutARowGetsItsAmAndId() throws Exception
	{
		String channel = am.channel("chat", null);
		assertEquals(am.getObjectId() + "xchat", channel);
		Target target = resolve(channel);
		assertSame(am, target.am());
		assertEquals("chat", target.templateId());
		assertNull(target.row());
	}

	@Test
	void aTemplateOnARowGetsThatRowBack() throws Exception
	{
		String channel = am.channel("card", second);
		assertEquals(am.getObjectId() + "xcardx" + am.getObjectId() + "x0x2", channel);
		Target target = resolve(channel);
		assertSame(am, target.am());
		assertEquals("card", target.templateId());
		assertSame(second, target.row());
		assertSame(first, resolve(am.channel("card", first)).row());
	}

	@Test
	void aRowOfALinkedViewIsFoundThroughItsParentRow() throws Exception
	{
		String channel = am.channel("7", note);
		assertEquals(am.getObjectId() + "x7x" + am.getObjectId() + "x0x2x1x1", channel);
		assertSame(note, resolve(channel).row());
	}

	@Test
	void aRowOfAnotherAmOfTheSameTopApplicationIsFound() throws Exception
	{
		ChannelAM parent = am(1);
		ViewRow parentRow = ((Cards) parent.getView("Cards")).newRow();
		Target target = resolve(am.channel("child", parentRow));
		assertSame(am, target.am(), "the template's AM receives the message");
		assertSame(parentRow, target.row());
	}

	@Test
	void aRowThatIsGoneDropsTheMessage()
	{
		String channel = am.channel("card", first);
		cards.rows.remove(first);
		assertEquals(Failure.STALE_ROW, failure(channel));
		String inLinkedView = am.channel("note", note);
		second.viewLinks.clear();
		assertEquals(Failure.STALE_ROW, failure(inLinkedView));
	}

	@Test
	void onlyIdsTheMessageFormatAndThePageCanCarryMakeAChannel()
	{
		for (String id : new String[] {"a,b", "box", "Xy", "a'b", "a b", "", null, "a".repeat(65)})
		{
			assertThrows(SkylinException.class, () -> am.channel(id, null), String.valueOf(id));
		}
		am.channel("Game_2-b", null);
		am.channel("a".repeat(64), null);
	}

	@Test
	void aRowWhoseRootViewIsNotOneOfItsAmsViewsHasNoChannel()
	{
		ViewRow loose = new Cards(am).newRow();
		assertThrows(SkylinException.class, () -> am.channel("card", loose));
	}

	@Test
	void anAmTheSessionDoesNotHaveIsUnknown()
	{
		String nonAm = session.instantiateByName(StringBuilder.class.getName());
		assertEquals(Failure.UNKNOWN_AM, failure("999xchat"));
		assertEquals(Failure.UNKNOWN_AM, failure(nonAm + "xchat"));
	}

	@Test
	void aForgedOrDamagedChannelIsMalformed()
	{
		int a = am.getObjectId();
		String nonAm = session.instantiateByName(StringBuilder.class.getName());
		ChannelAM otherTop = am(2);
		((Cards) otherTop.getView("Cards")).newRow();

		assertEquals(Failure.MALFORMED, failure(null));
		assertEquals(Failure.MALFORMED, failure(""));
		assertEquals(Failure.MALFORMED, failure(a + ""));
		assertEquals(Failure.MALFORMED, failure(a + "xcardx" + a));
		assertEquals(Failure.MALFORMED, failure(a + "xcardx" + a + "x1"));
		assertEquals(Failure.MALFORMED, failure("abcxchat"));
		assertEquals(Failure.MALFORMED, failure(a + "xa,b"));
		assertEquals(Failure.MALFORMED, failure(a + "xcardx" + nonAm + "x1x1"), "the row's AM is not an AM");
		assertEquals(Failure.MALFORMED, failure(a + "xcardx" + otherTop.getObjectId() + "x1x1"), "another top application");
		assertEquals(Failure.MALFORMED, failure(a + "xcardx" + a + "x2x1"), "no such root view");
		assertEquals(Failure.MALFORMED, failure(a + "xcardx" + a + "x-1x1"));
		assertEquals(Failure.MALFORMED, failure(a + "xcardx" + a + "x1x2x1"), "ends at a view, not a row");
		assertEquals(Failure.MALFORMED, failure(a + "xcardx" + a + "x1x2xx1"));
		assertEquals(Failure.MALFORMED, failure(a + "xchat" + "1".repeat(TemplateChannel.MAX_LENGTH)), "too long");
	}
}
