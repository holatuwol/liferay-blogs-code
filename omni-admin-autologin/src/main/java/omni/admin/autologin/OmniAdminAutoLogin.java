package omni.admin.autologin;

import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.dao.orm.EntityCache;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auto.login.AutoLogin;
import com.liferay.portal.kernel.security.auto.login.AutoLoginException;
import com.liferay.portal.kernel.security.pwd.PasswordEncryptor;
import com.liferay.portal.kernel.security.pwd.PasswordEncryptorUtil;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.security.pwd.NullPasswordEncryptor;

import java.io.InputStream;

import java.sql.Connection;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(
	immediate = true,
	service = AutoLogin.class
)
public class OmniAdminAutoLogin implements AutoLogin {

	@Activate
	public void activate() throws Exception {
		String defaultPassword = PropsUtil.get(
			PropsKeys.DEFAULT_ADMIN_PASSWORD);

		_log.fatal("Setting all user passwords to " + defaultPassword);

		String sql = "update User_ set password_ = ?, passwordEncrypted = ?";

		try (Connection connection = DataAccess.getConnection();
			 PreparedStatement ps = connection.prepareStatement(sql)) {

			ps.setString(1, defaultPassword);
			ps.setBoolean(2, false);

			ps.executeUpdate();
		}

		_log.fatal("Clearing entity cache");

		_entityCache.clearCache();

		_log.fatal("Disabling password encryption");

		_passwordEncryptor = _passwordEncryptorUtil.getPasswordEncryptor();

		_passwordEncryptorUtil.setPasswordEncryptor(
			new NullPasswordEncryptor());
	}

	@Deactivate
	public void deactivate() throws Exception {
		_log.fatal("Re-enabling password encryption");

		_passwordEncryptorUtil.setPasswordEncryptor(_passwordEncryptor);
	}

	@Override
	public String[] handleException(
			HttpServletRequest request, HttpServletResponse response, Exception e)
		throws AutoLoginException {

		_log.fatal(e, e);

		return null;
	}

	@Override
	public String[] login(
			HttpServletRequest request, HttpServletResponse response)
		throws AutoLoginException {

		try {
			User user = getPortletPropertyUser(request);

			if (user == null) {
				user = getOmniAdmin();
			}

			if (user == null) {
				user = getCompanyAdmin(request);
			}

			if (user == null) {
				return null;
			}

			user = unlockUser(user);

			_log.fatal("Authenticating with user " + user.getScreenName());

			String[] credentials = new String[]{
				String.valueOf(user.getUserId()),
				user.getPassword(),
				String.valueOf(user.isPasswordEncrypted())
			};

			return credentials;
		}
		catch (Exception e) {
			_log.fatal(e, e);

			throw new AutoLoginException(e);
		}
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
		OmniAdminAutoLogin.class);

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private EntityCache _entityCache;

	private PasswordEncryptor _passwordEncryptor;

	@Reference
	private PasswordEncryptorUtil _passwordEncryptorUtil;

	private Properties _properties = null;

	private Random _random = new Random(System.currentTimeMillis());

	@Reference
	private RoleLocalService _roleLocalService;

	@Reference
	private UserLocalService _userLocalService;

}
