package com.example.wab.precompiler;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.osgi.web.servlet.JSPServletFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Minhchau Dang
 */
@Component(
	immediate = true,
	property = {
		"com.example.wab.precompiler=true",
		"service.ranking:Integer=" + Integer.MAX_VALUE
	},
	service = JSPServletFactory.class
)
public class PrecompileJspServletFactory implements JSPServletFactory {

	@Override
	public Servlet createJSPServlet() {
		Servlet servlet = _jspServletFactory.createJSPServlet();

		return new PrecompileJspServlet((HttpServlet)servlet);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		PrecompileJspServletFactory.class);

	@Reference(target = "(!(com.example.wab.precompiler=true))")
	private JSPServletFactory _jspServletFactory;

}