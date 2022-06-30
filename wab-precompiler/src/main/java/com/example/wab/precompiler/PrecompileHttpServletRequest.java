package com.example.wab.precompiler;

import com.liferay.portal.kernel.util.LocaleUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.security.Principal;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

/**
 * @author Minhchau Dang
 */
public class PrecompileHttpServletRequest implements HttpServletRequest {

	public PrecompileHttpServletRequest(
		ServletContext servletContext, String jspPath) {

		_servletContext = servletContext;

		_attributes.put("javax.servlet.include.servlet_path", jspPath);
	}

	@Override
	public boolean authenticate(HttpServletResponse httpServletResponse)
		throws IOException, ServletException {

		return false;
	}

	@Override
	public AsyncContext getAsyncContext() {
		return null;
	}

	@Override
	public Object getAttribute(String s) {
		return _attributes.get(s);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(_attributes.keySet());
	}

	@Override
	public String getAuthType() {
		return null;
	}

	@Override
	public String getCharacterEncoding() {
		return null;
	}

	@Override
	public int getContentLength() {
		return 0;
	}

	@Override
	public String getContentType() {
		return null;
	}

	@Override
	public String getContextPath() {
		return null;
	}

	@Override
	public Cookie[] getCookies() {
		return new Cookie[0];
	}

	@Override
	public long getDateHeader(String s) {
		return 0;
	}

	@Override
	public DispatcherType getDispatcherType() {
		return null;
	}

	@Override
	public String getHeader(String s) {
		return null;
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return Collections.emptyEnumeration();
	}

	@Override
	public Enumeration<String> getHeaders(String s) {
		return Collections.emptyEnumeration();
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public int getIntHeader(String s) {
		return 0;
	}

	@Override
	public String getLocalAddr() {
		return null;
	}

	@Override
	public Locale getLocale() {
		return LocaleUtil.getDefault();
	}

	@Override
	public Enumeration<Locale> getLocales() {
		return Collections.enumeration(Collections.singleton(getLocale()));
	}

	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public int getLocalPort() {
		return 0;
	}

	@Override
	public String getMethod() {
		return "HEAD";
	}

	@Override
	public String getParameter(String s) {
		return null;
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return Collections.emptyMap();
	}

	@Override
	public Enumeration<String> getParameterNames() {
		return Collections.emptyEnumeration();
	}

	@Override
	public String[] getParameterValues(String s) {
		return new String[0];
	}

	@Override
	public Part getPart(String s) throws IOException, ServletException {
		return null;
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		return null;
	}

	@Override
	public String getPathInfo() {
		return null;
	}

	@Override
	public String getPathTranslated() {
		return null;
	}

	@Override
	public String getProtocol() {
		return null;
	}

	public String getQueryString() {
		return "jsp_precompile";
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return null;
	}

	@Override
	public String getRealPath(String s) {
		return _servletContext.getRealPath(s);
	}

	@Override
	public String getRemoteAddr() {
		return null;
	}

	@Override
	public String getRemoteHost() {
		return null;
	}

	@Override
	public int getRemotePort() {
		return 0;
	}

	@Override
	public String getRemoteUser() {
		return null;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String s) {
		return _servletContext.getRequestDispatcher(s);
	}

	@Override
	public String getRequestedSessionId() {
		return null;
	}

	@Override
	public String getRequestURI() {
		return null;
	}

	@Override
	public StringBuffer getRequestURL() {
		return null;
	}

	@Override
	public String getScheme() {
		return null;
	}

	@Override
	public String getServerName() {
		return null;
	}

	@Override
	public int getServerPort() {
		return 0;
	}

	@Override
	public ServletContext getServletContext() {
		return _servletContext;
	}

	@Override
	public String getServletPath() {
		return null;
	}

	@Override
	public HttpSession getSession() {
		return null;
	}

	@Override
	public HttpSession getSession(boolean b) {
		return null;
	}

	@Override
	public Principal getUserPrincipal() {
		return null;
	}

	@Override
	public boolean isAsyncStarted() {
		return false;
	}

	@Override
	public boolean isAsyncSupported() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return false;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public boolean isUserInRole(String s) {
		return false;
	}

	@Override
	public void login(String s, String s1) throws ServletException {
	}

	@Override
	public void logout() throws ServletException {
	}

	@Override
	public void removeAttribute(String s) {
		_attributes.remove(s);
	}

	@Override
	public void setAttribute(String s, Object o) {
		_attributes.put(s, o);
	}

	@Override
	public void setCharacterEncoding(String s)
		throws UnsupportedEncodingException {
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		return null;
	}

	@Override
	public AsyncContext startAsync(
			ServletRequest servletRequest, ServletResponse servletResponse)
		throws IllegalStateException {

		return null;
	}

	private Map<String, Object> _attributes = new HashMap<>();
	private ServletContext _servletContext;

};