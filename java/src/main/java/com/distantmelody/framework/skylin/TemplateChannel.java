package com.distantmelody.framework.skylin;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.distantmelody.framework.skylin.bridge.BridgeSession;
import com.distantmelody.library.json.J;

/**
 * The address of a template's custom messages. A template hands it to its page through the {@code <skylin_channel>}
 * binding, the page passes it with every message it sends, and the AM addresses messages to the template with it.
 * <ul>
 * <li>Without a row: {@code <AM id>x<application id>x<template id>}, where the AM is the template's AM.</li>
 * <li>On a row: {@code <AM id>x<application id>x<template id>x<root view index>x<row link id>}, where the AM is the
 * row's AM.</li>
 * </ul>
 * The AM id is the AM's object id in the session. A template id is unique within its application, so the application
 * id and the template id together name one template, also when templates of several applications render on rows of
 * the same AM. The root view index is the position of the row's root view among its AM's views, which never change
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

	/**
	 * The template a message came from: the AM of the channel, the template's application and id, and the row it
	 * rendered on ({@code null} when it had none).
	 */
	public record Target(AM am, int applicationId, String templateId, ViewRow row)
	{
		/** Hands the message to the row's handler, or to the AM's when the template has no row. */
		public byte[] receive(J message)
		{
			return row == null
				? am.receiveChannelMessage(message, templateId, null)
				: row.receiveChannelMessage(message, templateId);
		}

		/** Hands the binary message to the row's handler, or to the AM's when the template has no row. */
		public byte[] receiveBinary(byte[] message)
		{
			return row == null
				? am.receiveChannelBinaryMessage(message, templateId, null)
				: row.receiveChannelBinaryMessage(message, templateId);
		}
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

	/** The channel of template {@code templateId} of application {@code applicationId}; {@code am} is the template's AM. */
	static String build(AM am, int applicationId, String templateId, ViewRow row)
	{
		if (templateId == null || !TEMPLATE_ID.matcher(templateId).matches())
		{
			throw new SkylinException(am, "A template with a channel needs an id of up to 64 letters other than x, digits, _ or -, not " + templateId);
		}
		if (applicationId < 0)
		{
			throw new SkylinException(am, "The channel of template " + templateId + " needs its application id, not " + applicationId);
		}
		if (row == null)
		{
			return am.getObjectId() + "x" + applicationId + "x" + templateId;
		}
		View root = row.view.getRootView();
		int index = root.am.views.indexOf(root);
		if (index < 0)
		{
			throw new SkylinException(am, "A template with a channel cannot render on a row of view " + root.getName()
				+ ", because that view is not one of the views of " + root.am.getClass().getSimpleName());
		}
		String channel = root.am.getObjectId() + "x" + applicationId + "x" + templateId + "x" + index + "x" + row.getLinkId();
		if (channel.length() > MAX_LENGTH)
		{
			throw new SkylinException(am, "The channel of template " + templateId + " is longer than " + MAX_LENGTH + " characters, because its row is nested too deep");
		}
		return channel;
	}

	/**
	 * The template and row a channel from the browser names, checked against the session: the AM must be an AM of this
	 * session, a template without a row must belong to that AM's application, and the row must still exist. A malformed
	 * channel is logged as a warning and a stale row at debug level; the caller decides what an unknown AM means.
	 */
	public static Target resolve(BridgeSession session, String channel) throws UnresolvableChannel
	{
		if (channel == null || channel.length() > MAX_LENGTH)
		{
			throw malformed("it is missing or too long");
		}
		String[] parts = channel.split("x", 5);
		if (parts.length != 3 && parts.length != 5)
		{
			throw malformed("it has " + parts.length + " parts");
		}
		int applicationId = number(parts[1]);
		if (applicationId < 0)
		{
			throw malformed("the application id is negative");
		}
		String templateId = parts[2];
		if (!TEMPLATE_ID.matcher(templateId).matches())
		{
			throw malformed("the template id is not one a channel can have");
		}
		if (!(session.getObject(number(parts[0])) instanceof AM am))
		{
			throw new UnresolvableChannel(Failure.UNKNOWN_AM, "the session has no AM " + parts[0]);
		}
		if (parts.length == 3)
		{
			if (applicationId != am.frontEndApplicationId)
			{
				throw malformed("a template without a row names an application its AM does not belong to");
			}
			return new Target(am, applicationId, templateId, null);
		}
		int index = number(parts[3]);
		if (index < 0 || index >= am.views.size())
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
		ViewRow row = row(am.views.get(index), chain);
		if (row == null)
		{
			LOG.debug("Dropped a message for template {}: its row is gone", templateId);
			throw new UnresolvableChannel(Failure.STALE_ROW, "the row is gone");
		}
		return new Target(am, applicationId, templateId, row);
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
