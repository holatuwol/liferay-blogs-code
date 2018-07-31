package com.example.groupurlprovider;

import com.liferay.site.util.GroupURLProvider;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.permission.GroupPermissionUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;

import java.util.Collection;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

import javax.portlet.PortletRequest;
import javax.portlet.PortletURL;

/**
 * @author Minhchau Dang
 */
@Component(
	immediate = true,
	property = {
		// TODO enter required service properties
	},
	service = GroupURLProvider.class
)
public class PreferPrivatePagesGroupURLProvider extends GroupURLProvider {

	@Activate
	public void activate(
	        ComponentContext componentContext, BundleContext bundleContext,
	        Map<String, Object> config)
	    throws Exception {

	    _bundleContext = bundleContext;

	    _deactivateExistingComponent();
	}

	@Reference(
	    cardinality = ReferenceCardinality.OPTIONAL,
	    policy = ReferencePolicy.DYNAMIC,
	    policyOption = ReferencePolicyOption.GREEDY,
	    unbind = "unsetGroupURLProvider"
	)
	public void setGroupURLProvider(GroupURLProvider groupURLProvider)
		throws Exception {

	    _deactivateExistingComponent();
	}

	public void unsetGroupURLProvider(GroupURLProvider groupURLProvider) {
	}

	@Override
	protected String getGroupURL(
		Group group, PortletRequest portletRequest,
		boolean includeStagingGroup) {

		ThemeDisplay themeDisplay = (ThemeDisplay)portletRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		// Customization START
		// Usually Liferay passes false and then true. We'll change that to
		// instead pass true and then false, which will result in the Go to Site
		// preferring private pages over public pages whenever both are present.

		String groupDisplayURL = group.getDisplayURL(themeDisplay, true);

		if (Validator.isNotNull(groupDisplayURL)) {
			return _http2.removeParameter(groupDisplayURL, "p_p_id");
		}

		groupDisplayURL = group.getDisplayURL(themeDisplay, false);

		if (Validator.isNotNull(groupDisplayURL)) {
			return _http2.removeParameter(groupDisplayURL, "p_p_id");
		}

		// Customization END

		if (includeStagingGroup && group.hasStagingGroup()) {
			try {
				if (GroupPermissionUtil.contains(
						themeDisplay.getPermissionChecker(), group,
						ActionKeys.VIEW_STAGING)) {

					return getGroupURL(group.getStagingGroup(), portletRequest);
				}
			}
			catch (PortalException pe) {
				_log.error(
					"Unable to check permission on group " +
						group.getGroupId(),
					pe);
			}
		}

		return getGroupAdministrationURL(group, portletRequest);
	}

	private void _deactivateExistingComponent()
		throws Exception {

	    if (_bundleContext == null) {
	        return;
	    }

        String componentName = GroupURLProvider.class.getName();

	    Collection<ServiceReference<GroupURLProvider>>
	        serviceReferences =
	            _bundleContext.getServiceReferences(
	                GroupURLProvider.class,
	                "(component.name=" + componentName + ")");

	    for (ServiceReference serviceReference : serviceReferences) {
	        Bundle bundle = serviceReference.getBundle();

	        ComponentDescriptionDTO description =
	            _serviceComponentRuntime.getComponentDescriptionDTO(
	                bundle, componentName);

	        _serviceComponentRuntime.disableComponent(description);
	    }
	}

	private static final Log _log = LogFactoryUtil.getLog(
		PreferPrivatePagesGroupURLProvider.class);

	private BundleContext _bundleContext;

	@Reference
	private Http _http2;

	@Reference
	private ServiceComponentRuntime _serviceComponentRuntime;

}