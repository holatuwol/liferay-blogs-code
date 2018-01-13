package com.liferay.pandoc.wiki;

import com.liferay.portal.kernel.util.ResourceBundleLoader;
import com.liferay.wiki.engine.WikiEngine;

import java.util.Collection;
import java.util.Map;

import javax.servlet.ServletContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

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
public class CreoleWikiEngine extends BasePandocWikiEngine {

	@Activate
	public void activate(
			ComponentContext componentContext, BundleContext bundleContext,
			Map<String, Object> config)
		throws Exception {

		Collection<ServiceReference<WikiEngine>>
			serviceReferences =
				bundleContext.getServiceReferences(
					WikiEngine.class,
					"(component.name=" + _BUILT_IN_COMPONENT_NAME + ")");

		for (ServiceReference serviceReference : serviceReferences) {
			Bundle bundle = serviceReference.getBundle();

			ComponentDescriptionDTO description =
				_serviceComponentRuntime.getComponentDescriptionDTO(
					bundle, _BUILT_IN_COMPONENT_NAME);

			_serviceComponentRuntime.disableComponent(description);
		}
	}

	@Deactivate
	public void deactivate(
			ComponentContext componentContext, BundleContext bundleContext,
			Map<String, Object> config)
		throws Exception {

		Collection<ServiceReference<WikiEngine>>
			serviceReferences =
			bundleContext.getServiceReferences(
				WikiEngine.class,
				"(component.name=" + _BUILT_IN_COMPONENT_NAME + ")");

		for (ServiceReference serviceReference : serviceReferences) {
			Bundle bundle = serviceReference.getBundle();

			ComponentDescriptionDTO description =
				_serviceComponentRuntime.getComponentDescriptionDTO(
					bundle, _BUILT_IN_COMPONENT_NAME);

			_serviceComponentRuntime.enableComponent(description);
		}
	}

	@Override
	public String getFormat() {
		return "creole";
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

	private static final String _BUILT_IN_COMPONENT_NAME =
		"com.liferay.wiki.engine.creole.internal.CreoleWikiEngine";

	@Reference
	private ServiceComponentRuntime _serviceComponentRuntime;

}