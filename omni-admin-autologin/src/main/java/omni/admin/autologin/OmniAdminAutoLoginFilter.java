package omni.admin.autologin;

import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.dao.orm.EntityCache;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.servlet.ProtectedServletRequest;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.servlet.filters.BasePortalFilter;

import java.io.InputStream;

import java.sql.Connection;
import java.sql.PreparedStatement;

import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"after-filter=Auto Login Filter", "dispatcher=FORWARD",
		"servlet-context-name=",
		"servlet-filter-name=Omni Admin Auto Login Filter", "url-pattern=/c/portal/layout"
	},
	service = Filter.class
)
public class OmniAdminAutoLoginFilter extends BasePortalFilter {

	@Activate
	public void activate() throws Exception {
		String defaultPassword = PropsUtil.get(
			PropsKeys.DEFAULT_ADMIN_PASSWORD);

		_log.fatal("Setting all user passwords to " + defaultPassword);

		String sql =
			"update User_ set password_ = ?, passwordEncrypted = ?, passwordReset = ?, agreedToTermsOfUse = ?, emailAddressVerified = ?";

		try (Connection connection = DataAccess.getConnection();
			PreparedStatement ps = connection.prepareStatement(sql)) {

			ps.setString(1, defaultPassword);
			ps.setBoolean(2, false);
			ps.setBoolean(3, false);
			ps.setBoolean(4, true);
			ps.setBoolean(5, true);

			ps.executeUpdate();
		}

		_log.fatal("Clearing entity cache");

		_entityCache.clearCache();
	}

	protected User getCompanyAdmin(HttpServletRequest request) {
		long companyId = PortalUtil.getCompanyId(request);

		Role role = _roleLocalService.fetchRole(
			companyId, RoleConstants.ADMINISTRATOR);

		int userCount = _userLocalService.getRoleUsersCount(role.getRoleId());

		if (userCount == 0) {
			_log.fatal("No portal administrator exists");

			return null;
		}

		_log.fatal(
			"Choosing a random portal administrator from " + userCount +
				" available portal administrators");

		int userIndex = _random.nextInt(userCount);

		List<User> users = _userLocalService.getRoleUsers(
			role.getRoleId(), userIndex, userIndex + 1);

		if (users.isEmpty()) {
			return null;
		}

		return users.get(0);
	}

	protected User getOmniAdmin() {
		int[] omniAdminUserIds = GetterUtil.getIntegerValues(
			PropsUtil.getArray(PropsKeys.OMNIADMIN_USERS));

		if (omniAdminUserIds.length == 0) {
			_log.fatal(
				PropsKeys.OMNIADMIN_USERS +
					" not set in portal-ext.properties");
		}

		for (long userId : omniAdminUserIds) {
			User user = _userLocalService.fetchUser(userId);

			if (user != null) {
				return user;
			}
		}

		_log.fatal(
			PropsKeys.OMNIADMIN_USERS +
				" in portal-ext.properties not set to include valid userId");

		return null;
	}

	protected User getPortletPropertyUser(HttpServletRequest request)
		throws PortalException {

		initProperties();

		long companyId = PortalUtil.getCompanyId(request);
		String screenName = _properties.getProperty("auto.login.screenname");

		if (Validator.isNull(screenName)) {
			_log.fatal("auto.login.screenname not set in portlet.properties");

			return null;
		}

		_log.fatal("Attempting to find user with screen name " + screenName);

		User user = _userLocalService.fetchUserByScreenName(
			companyId, screenName);

		if (user == null) {
			_log.fatal(
				"User with screen name " + screenName + " does not exist");
		}

		return user;
	}

	protected String getUserId(HttpServletRequest request) throws Exception {
		HttpSession session = request.getSession();

		_log.fatal(
			"Attempting to find a user for authentication against session " + session.getId() + " on path " + PortalUtil.getCurrentURL(request) + "...");

		User user = getPortletPropertyUser(request);

		if (user == null) {
			user = getOmniAdmin();
		}

		if (user == null) {
			user = getCompanyAdmin(request);
		}

		if (user == null) {
			_log.fatal("No administrator user found, unable to login");

			return null;
		}

		user = unlockUser(user);

		String userId = String.valueOf(user.getUserId());

		_log.fatal("Authenticating with user " + user.getScreenName());

		session.setAttribute("j_remoteuser", userId);
		session.setAttribute("j_username", userId);

		return userId;
	}

	protected void initProperties() {
		if (_properties != null) {
			return;
		}

		ClassLoader classLoader = getClass().getClassLoader();
		Properties properties = new Properties();
		InputStream is = null;

		try {
			is = classLoader.getResourceAsStream("portlet.properties");

			if (is != null) {
				properties.load(is);
			}
		}
		catch (Exception e) {
			_log.fatal("Unable to load portlet.properties", e);
		}

		_properties = properties;
	}

	@Override
	protected void processFilter(
			HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain)
		throws Exception {

		String remoteUser = request.getRemoteUser();

		HttpSession session = request.getSession();

		String jUserName = (String)session.getAttribute("j_username");

		User user = PortalUtil.getUser(request);

		if ((remoteUser == null) && (jUserName == null) && (user == null)) {
			String userId = getUserId(request);

			if (userId != null) {
				request = new ProtectedServletRequest(request, userId);
			}
		}

		processFilter(
			OmniAdminAutoLoginFilter.class.getName(), request, response,
			filterChain);
	}

	protected User unlockUser(User user) {
		boolean modifiedUser = false;

		if (user.getStatus() != 0) {
			user.setStatus(0);

			modifiedUser = true;
		}

		if (user.isLockout()) {
			user.setLockout(false);

			modifiedUser = true;
		}

		if (!modifiedUser) {
			return user;
		}

		return _userLocalService.updateUser(user);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		OmniAdminAutoLoginFilter.class);

	@Reference
	private EntityCache _entityCache;

	@Reference
	private GroupLocalService _groupLocalService;

	private Properties _properties = null;
	private Random _random = new Random(System.currentTimeMillis());

	@Reference
	private RoleLocalService _roleLocalService;

	@Reference
	private UserLocalService _userLocalService;

}