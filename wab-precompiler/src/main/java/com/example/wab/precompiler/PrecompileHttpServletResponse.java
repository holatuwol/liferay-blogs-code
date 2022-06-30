package com.example.wab.precompiler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Minhchau Dang
 */
public class PrecompileHttpServletResponse implements HttpServletResponse {

	@Override
	public void addCookie(Cookie cookie) {
	}

	@Override
	public void addDateHeader(String s, long l) {
	}

	@Override
	public void addHeader(String s, String s1) {
	}

	@Override
	public void addIntHeader(String s, int i) {
	}

	@Override
	public boolean containsHeader(String s) {
		return false;
	}

	@Override
	public String encodeRedirectUrl(String s) {
		return null;
	}

	@Override
	public String encodeRedirectURL(String s) {
		return null;
	}

	@Override
	public String encodeUrl(String s) {
		return null;
	}

	@Override
	public String encodeURL(String s) {
		return null;
	}

	@Override
	public void flushBuffer() throws IOException {
	}

	@Override
	public int getBufferSize() {
		return 0;
	}

	@Override
	public String getCharacterEncoding() {
		return null;
	}

	@Override
	public String getContentType() {
		return null;
	}

	@Override
	public String getHeader(String s) {
		return null;
	}

	@Override
	public Collection<String> getHeaderNames() {
		return null;
	}

	@Override
	public Collection<String> getHeaders(String s) {
		return null;
	}

	@Override
	public Locale getLocale() {
		return null;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return new ServletOutputStream() {

			@Override
			public void write(int b) throws IOException {
			}

		};
	}

	@Override
	public int getStatus() {
		return 0;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		OutputStream outputStream = new OutputStream() {

			@Override
			public void write(int b) throws IOException {
			}

		};

		return new PrintWriter(outputStream);
	}

	@Override
	public boolean isCommitted() {
		return false;
	}

	@Override
	public void reset() {
	}

	@Override
	public void resetBuffer() {
	}

	@Override
	public void sendError(int i) throws IOException {
	}

	@Override
	public void sendError(int i, String s) throws IOException {
	}

	@Override
	public void sendRedirect(String s) throws IOException {
	}

	@Override
	public void setBufferSize(int i) {
	}

	@Override
	public void setCharacterEncoding(String s) {
	}

	@Override
	public void setContentLength(int i) {
	}

	@Override
	public void setContentType(String s) {
	}

	@Override
	public void setDateHeader(String s, long l) {
	}

	@Override
	public void setHeader(String s, String s1) {
	}

	@Override
	public void setIntHeader(String s, int i) {
	}

	@Override
	public void setLocale(Locale locale) {
	}

	@Override
	public void setStatus(int i) {
	}

	@Override
	public void setStatus(int i, String s) {
	}

}