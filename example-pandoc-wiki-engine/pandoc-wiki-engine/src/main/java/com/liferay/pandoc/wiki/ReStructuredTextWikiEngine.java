package com.liferay.pandoc.wiki;

import com.liferay.portal.kernel.util.ResourceBundleLoader;
import com.liferay.wiki.engine.WikiEngine;

import javax.servlet.ServletContext;

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
	service = WikiEngine.class
)
public class ReStructuredTextWikiEngine extends BasePandocWikiEngine {

	@Override
	public String getFormat() {
		return "rst";
	}

	@Reference(target = "(bundle.symbolic.name=com.liferay.pandoc.wiki)")
	protected void setResourceBundleLoader(
		ResourceBundleLoader resourceBundleLoader) {

		super.setResourceBundleLoader(resourceBundleLoader);
	}

	@Reference(
		target = "(osgi.web.symbolicname=com.liferay.wiki.engine.text)"
	)
	protected void setEditPageServletContext(ServletContext servletContext) {
		super.setEditPageServletContext(servletContext);
	}

}