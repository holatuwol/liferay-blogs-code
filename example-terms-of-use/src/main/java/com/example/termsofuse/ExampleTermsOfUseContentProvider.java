package com.example.termsofuse;

import com.liferay.portal.kernel.util.TermsOfUseContentProvider;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Minhchau Dang
 */
@Component(
	immediate = true,
	property = {
		// TODO enter required service properties
	},
	service = TermsOfUseContentProvider.class
)
public class ExampleTermsOfUseContentProvider implements TermsOfUseContentProvider {

	@Override
	public String getClassName() {
		System.out.println("Called getClassName()");

		return "";
	}

	@Override
	public void includeConfig(
			HttpServletRequest request, HttpServletResponse response)
		throws Exception {

		System.out.println("Called includeConfig(HttpServletRequest, HttpServletResponse)");
	}

	@Override
	public void includeView(
			HttpServletRequest request, HttpServletResponse response)
		throws Exception {

		System.out.println("Called includeView(HttpServletRequest, HttpServletResponse)");

		_servletContext.getRequestDispatcher("/terms_of_use.jsp").include(request, response);
	}

	@Reference(target = "(osgi.web.symbolicname=com.example.termsofuse)")
	private ServletContext _servletContext;

}