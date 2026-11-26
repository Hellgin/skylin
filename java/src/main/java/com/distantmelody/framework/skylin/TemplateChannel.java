package com.distantmelody.framework.skylin;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.distantmelody.framework.skylin.bridge.BridgeSession;

/**
 * The address of a template's custom messages. A template hands it to its page through the {@code <skylin_channel>}
 * binding, the page passes it with every message it sends, and the AM addresses messages to the template with it.
 * <ul>
 * <li>Without a row: {@code <AM id>x<template id>}.</li>
 * <li>On a row: {@code <AM id>x<template id>x<row's AM id>x<root view index>x<row link id>}.</li>
 * </ul>
 * The AM ids are the AMs' object ids in the session; the first is the template's AM, which receives the messages.
 * The root view index is the position of the row's root view among the views of the row's AM, which never change
 * after the AM initializes.
 */
public final class TemplateChannel
{
	private static final Logger LOG = LoggerFactory.getLogger(TemplateChannel.class);

	/** Letters other than x, digits, _ and -: no comma, quote or x can reach the message format or the page's script. */
	private static final Pattern TEMPLATE_ID = Pattern.compile("[0-9a-wyzA-WYZ_-]{1,64}");

	/** A binary message's header is {@code AM,<channel>} behind one byte that holds its length. */
	static final int MAX_LENGTH = 250;

	private TemplateChannel()
	{
	}

	/** The template, and the row it rendered on ({@code null} when it had none), that a message came from. */
	public record Target(AM am, String templateId, ViewRow row)
	{
	}

	public enum Failure
	{
		/** The channel is not one {@link #build} makes, or names objects that do not belong together. */
		MALFORMED,
		/** The session has no such AM, for example a page left open while the application was created again. */
		UNKNOWN_AM,
		/** The row the template rendered on is gone, for example after its view was queried again. */
		STALE_ROW
	}

	public static final class UnresolvableChannel extends Exception
	{
		private static final long serialVersionUID = 1L;

		public final Failure failure;

		UnresolvableChannel(Failure failure, String reason)
		{
			super(reason, null, false, false);
			this.failure = failure;
		}
	}

	static String build(AM am, String templateId, ViewRow row)
	{
		if (templateId == null || !TEMPLATE_ID.matcher(templateId).matches())
		{
			throw new SkylinException(am, "A template with a channel needs an id of up to 64 letters other than x, digits, _ or -, not " + templateId);
		}
		String channel = am.getObjectId() + "x" + templateId;
		if (row == null)
		{
			return channel;
		}
		View root = row.view.getRootView();
		int index = root.am.views.indexOf(root);
		if (index < 0)
		{
			throw new SkylinException(am, "A template with a channel cannot render on a row of view " + root.getName()
				+ ", because that view is not one of the views of " + root.am.getClass().getSimpleName());
		}
		channel += "x" + root.am.getObjectId() + "x" + index + "x" + row.getLinkId();
		if (channel.length() > MAX_LENGTH)
		{
			throw new SkylinException(am, "The channel of template " + templateId + " is longer than " + MAX_LENGTH + " characters, because its row is nested too deep");
		}
		return channel;
	}

	/**
	 * The template and row a channel from the browser names, checked against the session: both AMs must be AMs of
	 * this session with the same top application, and the row must still exist. A malformed channel is logged as a
	 * warning and a stale row at debug level; the caller decides what an unknown AM means.
	 */
	public static Target resolve(BridgeSession session, String channel) throws UnresolvableChannel
	{
		if (channel == null || channel.length() > MAX_LENGTH)
		{
			throw malformed("it is missing or too long");
		}
		String[] parts = channel.split("x", 5);
		if (parts.length != 2 && parts.length != 5)
		{
			throw malformed("it has " + parts.length + " parts");
		}
		if (!TEMPLATE_ID.matcher(parts[1]).matches())
		{
			throw malformed("the template id is not one a channel can have");
		}
		if (!(session.getObject(number(parts[0])) instanceof AM am))
		{
			throw new UnresolvableChannel(Failure.UNKNOWN_AM, "the session has no AM " + parts[0]);
		}
		if (parts.length == 2)
		{
			return new Target(am, parts[1], null);
		}
		if (!(session.getObject(number(parts[2])) instanceof AM rowAm))
		{
			throw malformed("the row's AM is not an AM of the session");
		}
		if (rowAm.topFrontEndApplicationId != am.topFrontEndApplicationId)
		{
			throw malformed("the row's AM belongs to another top application");
		}
		int index = number(parts[3]);
		if (index < 0 || index >= rowAm.views.size())
		{
			throw malformed("there is no root view " + index);
		}
		String[] link = parts[4].split("x", -1);
		int[] chain = new int[link.length];
		for (int i = 0; i < link.length; i++)
		{
			chain[i] = number(link[i]);
		}
		if (chain.length % 2 == 0)
		{
			throw malformed("the row link id does not end at a row");
		}
		ViewRow row = row(rowAm.views.get(index), chain);
		if (row == null)
		{
			LOG.debug("Dropped a message for template {}: its row is gone", parts[1]);
			throw new UnresolvableChannel(Failure.STALE_ROW, "the row is gone");
		}
		return new Target(am, parts[1], row);
	}

	/** Alternating row and linked view link ids from the root view down; {@code null} when one is gone. */
	private static ViewRow row(View root, int[] chain)
	{
		// Messages arrive outside the session lock, so a request may be changing these rows at the same time.
		try
		{
			View view = root;
			ViewRow row = null;
			for (int i = 0; i < chain.length; i++)
			{
				if (i % 2 == 0)
				{
					row = view.getLocalRowByLinkId(chain[i]);
					if (row == null)
					{
						return null;
					}
				}
				else
				{
					if (row.viewLinks == null)
					{
						return null;
					}
					view = row.getLocalViewByLinkId(chain[i]);
					if (view == null)
					{
						return null;
					}
				}
			}
			return row;
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	private static int number(String s) throws UnresolvableChannel
	{
		try
		{
			return Integer.parseInt(s);
		}
		catch (NumberFormatException e)
		{
			throw malformed("a part that must be a number is not");
		}
	}

	private static UnresolvableChannel malformed(String reason)
	{
		LOG.warn("Dropped a message with a malformed template channel: {}", reason);
		return new UnresolvableChannel(Failure.MALFORMED, reason);
	}
}
