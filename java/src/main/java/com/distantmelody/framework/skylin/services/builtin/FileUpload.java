package com.distantmelody.framework.skylin.services.builtin;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.distantmelody.framework.skylin.View;
import com.distantmelody.framework.skylin.ViewRow;
import com.distantmelody.framework.skylin.bridge.BridgeSession;

/**
 * {@code restless.php?com.distantmelody.framework.skylin.services.builtin.FileUpload}: the body is the file and the
 * {@code Authorization} header is {@code <view id>_<token>_<file name>}, with the file name URL-encoded
 * ({@code encodeURIComponent}). The token is the one {@link View#allowFileUpload} gave a {@code FileArea} of that view in
 * the caller's own session; the row gets the file name and the file in the two columns of that grant.
 * {@code FileArea} (skylin/file-upload-worker.js) is the caller.
 */
public class FileUpload extends HttpServlet
{
	private static final Logger LOG = LoggerFactory.getLogger(FileUpload.class);
	/** The whole answer when the file was stored; a refused upload gets an empty answer (the bridge cannot set a status). */
	static final byte[] STORED = "stored".getBytes(StandardCharsets.US_ASCII);

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		BridgeSession session = (BridgeSession)req.getAttribute("BRIDGE_SESSION");
		if (session == null)
		{
			return;
		}

		String header = req.getHeader("Authorization");
		String[] in = header == null ? new String[0] : header.split("_",3);
		Integer viewId = in.length == 3 ? id(in[0]) : null;
		if (viewId == null)
		{
			LOG.warn("FileUpload: the Authorization header is not <view id>_<token>_<file name>");
			return;
		}
		String fileName;
		try
		{
			fileName = URLDecoder.decode(in[2], StandardCharsets.UTF_8);
		}
		catch (IllegalArgumentException e)
		{
			LOG.warn("FileUpload: the file name in the Authorization header is not URL-encoded");
			return;
		}

		if (!(session.getObject(viewId) instanceof View view))
		{
			LOG.warn("FileUpload: object {} of the session is not a view", viewId);
			return;
		}
		View.FileUploadGrant grant = view.getFileUploadGrant(in[1]);
		if (grant == null)
		{
			LOG.warn("FileUpload: the token is not one a FileArea of view {} was given", viewId);
			return;
		}

		byte[] data = req.getInputStream().readAllBytes();
		ViewRow row = view.newRow();
		row.setValue(grant.nameColumn(), fileName);
		row.getBlob(grant.blobColumn()).setData(data);
		resp.getOutputStream().write(STORED);
	}

	private static Integer id(String s)
	{
		try
		{
			return Integer.valueOf(s);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}
}
