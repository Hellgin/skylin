package com.distantmelody.framework.skylin.services.builtin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.distantmelody.framework.skylin.AM;
import com.distantmelody.framework.skylin.TemplateChannel;
import com.distantmelody.framework.skylin.bridge.BridgeSession;
import com.distantmelody.library.json.J;

/**
 * {@code restless.php?com.distantmelody.framework.skylin.services.builtin.MessageAM} with the body
 * {@code {"channel":"<template channel>","message":<json>}}: hands the message to {@link AM#receiveChannelMessage} of
 * the template's AM in the caller's own session and answers with what it returns.
 */
public class MessageAM extends HttpServlet
{
	private static final Logger LOG = LoggerFactory.getLogger(MessageAM.class);

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		BridgeSession session = (BridgeSession)req.getAttribute("BRIDGE_SESSION");
		if (session == null)
		{
			return;
		}

		String channel;
		J message;
		try
		{
			J in = J.fromString(new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
			channel = in.getString("channel");
			message = in.getJ("message");
		}
		catch (RuntimeException | StackOverflowError e)
		{
			LOG.warn("MessageAM: the request body is not a message ({})", e.getClass().getSimpleName());
			return;
		}

		TemplateChannel.Target target;
		try
		{
			target = TemplateChannel.resolve(session, channel);
		}
		catch (TemplateChannel.UnresolvableChannel e)
		{
			if (e.failure == TemplateChannel.Failure.UNKNOWN_AM)
			{
				LOG.warn("MessageAM: the channel names an AM the session does not have");
			}
			return;
		}
		byte[] response = target.am().receiveChannelMessage(message, target.templateId(), target.row());
		if (response != null)
		{
			resp.getOutputStream().write(response);
		}
	}
}
