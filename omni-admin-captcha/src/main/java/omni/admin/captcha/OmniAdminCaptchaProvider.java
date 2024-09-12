package omni.admin.captcha;

import com.liferay.captcha.provider.CaptchaProvider;
import com.liferay.portal.kernel.captcha.Captcha;
import com.liferay.portal.kernel.module.framework.ModuleServiceLifecycle;

import javax.portlet.PortletRequest;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	immediate = true,
	property = {
		"service.ranking:Integer=100"
	},
	service = CaptchaProvider.class
)
public class OmniAdminCaptchaProvider implements CaptchaProvider {

	public Captcha getCaptcha() {
		return _omniAdminCaptcha;
	}

	private Captcha _omniAdminCaptcha = new OmniAdminCaptcha();

	private static class OmniAdminCaptcha implements Captcha {
		@Override
		public void check(HttpServletRequest httpServletRequest) {
		}

		@Override
		public void check(PortletRequest portletRequest) {
		}

		public void enforceCaptcha(PortletRequest portletRequest) {
		}

		@Override
		public String getTaglibPath() {
			return null;
		}

		@Override
		public boolean isEnabled(HttpServletRequest httpServletRequest) {
			return false;
		}

		@Override
		public boolean isEnabled(PortletRequest portletRequest) {
			return false;
		}

		@Override
		public void serveImage(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) {
		}

		@Override
		public void serveImage(
			ResourceRequest resourceRequest,
			ResourceResponse resourceResponse) {
		}
	}

}