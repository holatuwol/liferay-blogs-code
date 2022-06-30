package com.example.wab.precompiler;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.io.IOException;

import java.net.URL;

import java.util.Enumeration;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

/**
 * @author Minhchau Dang
 */
public class PrecompileJspServlet extends HttpServlet {

	public PrecompileJspServlet(HttpServlet jspServlet) {
		_jspServlet = jspServlet;
	}

	@Override
	public void destroy() {
		_jspServlet.destroy();
	}

	@Override
	public String getInitParameter(String name) {
		return _jspServlet.getInitParameter(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return _jspServlet.getInitParameterNames();
	}

	@Override
	public ServletConfig getServletConfig() {
		return _jspServlet.getServletConfig();
	}

	@Override
	public ServletContext getServletContext() {
		return _jspServlet.getServletContext();
	}

	@Override
	public String getServletInfo() {
		return _jspServlet.getServletInfo();
	}

	@Override
	public String getServletName() {
		return _jspServlet.getServletName();
	}

	@Override
	public void init() throws ServletException {
		_jspServlet.init();
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		ServletContext servletContext = config.getServletContext();

		_jspServlet.init(config);

		if (_isWabServletContext(servletContext)) {
			_compileJsps(servletContext, "/");
		}
	}

	@Override
	public void log(String msg) {
		_jspServlet.log(msg);
	}

	@Override
	public void log(String message, Throwable t) {
		_jspServlet.log(message, t);
	}

	@Override
	public void service(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException {

		_jspServlet.service(req, resp);
	}

	@Override
	public void service(ServletRequest req, ServletResponse res)
		throws IOException, ServletException {

		_jspServlet.service(req, res);
	}

	private boolean _compileJsp(
		ServletContext servletContext, String resourcePath) {

		if (!resourcePath.endsWith(".jsp") && !resourcePath.endsWith(".jspx")) {
			return true;
		}

		String messageSuffix = StringBundler.concat(
			"pre-compile ", resourcePath, " for servlet context ",
			servletContext.getContextPath());

		_log.fatal("Attempting to " + messageSuffix);

		try {
			_jspServlet.service(
				new PrecompileHttpServletRequest(servletContext, resourcePath),
				new PrecompileHttpServletResponse());
		}
		catch (Exception e) {
			_log.fatal("Exception when attempting to " + messageSuffix, e);

			return false;
		}

		return true;
	}

	private boolean _compileJsps(
		ServletContext servletContext, String rootPath) {

		Set<String> resourcePaths = servletContext.getResourcePaths(rootPath);

		if (resourcePaths == null) {
			return true;
		}

		boolean success = true;

		for (String resourcePath : resourcePaths) {
			if (resourcePath.endsWith("/")) {
				_compileJsps(servletContext, resourcePath);

				continue;
			}

			success &= _compileJsp(servletContext, resourcePath);
		}

		return success;
	}

	private boolean _isWabServletContext(ServletContext servletContext) {
		ClassLoader classLoader = servletContext.getClassLoader();

		if (!(classLoader instanceof BundleReference)) {
			return false;
		}

		BundleReference bundleReference = (BundleReference)classLoader;

		Bundle bundle = bundleReference.getBundle();

		try {
			URL hookXMLURL = servletContext.getResource(
				"/WEB-INF/liferay-hook.xml");

			if (hookXMLURL != null) {
				return false;
			}
		}
		catch (Exception e) {
			_log.fatal(e, e);
		}

		String bundleLocation = bundle.getLocation();

		return bundleLocation.startsWith("webbundle:");
	}

	private static final Log _log = LogFactoryUtil.getLog(
		PrecompileJspServlet.class);

	private final HttpServlet _jspServlet;

}