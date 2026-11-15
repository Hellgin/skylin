package com.distantmelody.framework.skylin.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.distantmelody.framework.skylin.bridge.ByteWriter;

public class SkylinHttpServletResponse implements HttpServletResponse{

	ServletOutputStream os = new SkylinServletOutputStream();
	
	HashMap<String,ArrayList<String>> headers = new HashMap<String,ArrayList<String>>();
	
	//String contentType = "text/plain";
	
	@Override
	public void flushBuffer() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getBufferSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getCharacterEncoding() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContentType() {
		return getHeader("Content-Type");
	}

	@Override
	public Locale getLocale() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return os;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isCommitted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resetBuffer() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBufferSize(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCharacterEncoding(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setContentLength(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setContentType(String v) {
		setHeader("Content-Type",v);
	}

	@Override
	public void setLocale(Locale arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addCookie(Cookie arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addDateHeader(String arg0, long arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addHeader(String k, String v) {
		ArrayList<String> h = headers.get(k);
		if (h == null)
		{
			h = new ArrayList<String>();
			headers.put(k, h);
		}
		h.add(v);
	}

	@Override
	public void addIntHeader(String arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean containsHeader(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String encodeRedirectURL(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String encodeRedirectUrl(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String encodeURL(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String encodeUrl(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getHeader(String k) {
		ArrayList<String> h = headers.get(k);
		if (h != null)
		{
			return h.get(0);
		}
		return null;
	}

	@Override
	public Collection<String> getHeaderNames() {
		return headers.keySet();
	}

	@Override
	public Collection<String> getHeaders(String k) {
		return headers.get(k);
	}

	@Override
	public int getStatus() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void sendError(int arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendError(int arg0, String arg1) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendRedirect(String arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDateHeader(String arg0, long arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setHeader(String k, String v) {
		ArrayList<String> h = new ArrayList<String>();
		h.add(v);
		headers.put(k, h);
	}

	@Override
	public void setIntHeader(String arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setStatus(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setStatus(int arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * The headers for PHP (rest.php), which cuts them with substr(): {@code <size>,<count>,<size1>,...,<header 1>...}
	 * where {@code <size>} is the length of everything after its comma, and every length is in BYTES of the UTF-8
	 * encoding.
	 */
	public byte[] getPackedHeaderData() 
	{
		int headerCount = 0;
		ByteWriter data = new ByteWriter();
		ByteWriter sizes = new ByteWriter();
		
		for (String h:headers.keySet())
		{
			for (String hh:headers.get(h))
			{
				byte[] v = (h +": " + hh).getBytes(StandardCharsets.UTF_8);
				data.bytes(v);
				sizes.ascii(v.length).ascii(",");
				headerCount++;
			}
		}
		
		ByteWriter counted = new ByteWriter().ascii(headerCount).ascii(",").bytes(sizes.toByteArray()).bytes(data.toByteArray());
		return new ByteWriter().ascii(counted.size()).ascii(",").bytes(counted.toByteArray()).toByteArray();
	}

}
