package com.example.roleconjunction.service.util;

import com.example.roleconjunction.service.configuration.RoleConjunctionConfiguration;

import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.model.Resource;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.ResourcePermission;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.module.configuration.ConfigurationException;
import com.liferay.portal.kernel.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.persistence.ResourcePermissionPersistence;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.security.permission.PermissionCacheUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Minhchau Dang
 */
@Component(service = RoleConjunctionHelper.class)
public class RoleConjunctionHelper {

	@Activate
	public void activate() {
		PermissionCacheUtil.clearCache();
	}

	public Map<String, List<Long>> getRoleIdsBySubtype(
			List<Resource> resources, long[] roleIds)
		throws ConfigurationException {

		if (resources.isEmpty()) {
			return Collections.emptyMap();
		}

		Resource individualResource = resources.get(0);

		long companyId = individualResource.getCompanyId();

		RoleConjunctionConfiguration roleConjunctionConfiguration =
			_configurationProvider.getCompanyConfiguration(
				RoleConjunctionConfiguration.class, companyId);

		Map<String, List<Long>> roleIdsBySubtype = new LinkedHashMap<>();

		for (String roleSubtype :
				roleConjunctionConfiguration.getConjunctionRoleSubtypes()) {

			for (Resource resource : resources) {
				if (hasNonZeroActionIds(
						companyId, resource.getName(), resource.getScope(),
						resource.getPrimKey(), roleSubtype)) {

					roleIdsBySubtype.put(roleSubtype, new ArrayList<>());

					break;
				}
			}
		}

		populateRoleIdsBySubtype(roleIdsBySubtype, roleIds);

		return roleIdsBySubtype;
	}

	public Map<String, List<Long>> getRoleIdsBySubtype(
			long companyId, String name, int scope, String primKey,
			long[] roleIds)
		throws ConfigurationException {

		RoleConjunctionConfiguration roleConjunctionConfiguration =
			_configurationProvider.getCompanyConfiguration(
				RoleConjunctionConfiguration.class, companyId);

		Map<String, List<Long>> roleIdsBySubtype = new LinkedHashMap<>();

		for (String roleSubtype :
				roleConjunctionConfiguration.getConjunctionRoleSubtypes()) {

			if (hasNonZeroActionIds(
					companyId, name, scope, primKey, roleSubtype)) {

				roleIdsBySubtype.put(roleSubtype, new ArrayList<>());
			}
		}

		populateRoleIdsBySubtype(roleIdsBySubtype, roleIds);

		return roleIdsBySubtype;
	}

	public boolean isActive(
			long companyId, long groupId, String name, String primKey)
		throws ConfigurationException {

		RoleConjunctionConfiguration roleConjunctionConfiguration =
			_configurationProvider.getCompanyConfiguration(
				RoleConjunctionConfiguration.class, companyId);

		if (!ArrayUtil.contains(
				roleConjunctionConfiguration.getConjunctionModelNames(),
				name)) {

			return false;
		}

		for (String roleSubtype :
				roleConjunctionConfiguration.getConjunctionRoleSubtypes()) {

			if (hasNonZeroActionIds(
					companyId, name, ResourceConstants.SCOPE_INDIVIDUAL,
					primKey, roleSubtype)) {

				return true;
			}

			if (hasNonZeroActionIds(
					companyId, name, ResourceConstants.SCOPE_GROUP,
					String.valueOf(groupId), roleSubtype)) {

				return true;
			}

			if (hasNonZeroActionIds(
					companyId, name, ResourceConstants.SCOPE_GROUP_TEMPLATE,
					String.valueOf(GroupConstants.DEFAULT_PARENT_GROUP_ID),
					roleSubtype)) {

				return true;
			}

			if (hasNonZeroActionIds(
					companyId, name, ResourceConstants.SCOPE_COMPANY,
					String.valueOf(companyId), roleSubtype)) {

				return true;
			}
		}

		return false;
	}

	public boolean isEnabled(long companyId, String name)
		throws ConfigurationException {

		RoleConjunctionConfiguration roleConjunctionConfiguration =
			_configurationProvider.getCompanyConfiguration(
				RoleConjunctionConfiguration.class, companyId);

		return ArrayUtil.contains(
			roleConjunctionConfiguration.getConjunctionModelNames(), name);
	}

	protected boolean hasNonZeroActionIds(
		long companyId, String name, int scope, String primKey,
		String roleSubtype) {

		List<Role> subtypeRoles = _roleLocalService.getRoles(
			RoleConstants.TYPE_REGULAR, roleSubtype);

		List<Long> subtypeRoleIds = ListUtil.toList(
			subtypeRoles, Role.ROLE_ID_ACCESSOR);

		List<ResourcePermission> subtypeResourcePermissions =
			_resourcePermissionPersistence.findByC_N_S_P_R(
				companyId, name, scope, primKey,
				ArrayUtil.toLongArray(subtypeRoleIds));

		for (ResourcePermission resourcePermission :
				subtypeResourcePermissions) {

			if (resourcePermission.getActionIds() > 0) {
				return true;
			}
		}

		return false;
	}

	protected void populateRoleIdsBySubtype(
		Map<String, List<Long>> roleIdsBySubtype, long[] roleIds) {

		for (long roleId : roleIds) {
			Role role = _roleLocalService.fetchRole(roleId);

			if ((role != null) &&
				(role.getType() == RoleConstants.TYPE_REGULAR) &&
				roleIdsBySubtype.containsKey(role.getSubtype())) {

				roleIdsBySubtype.get(role.getSubtype()).add(roleId);
			}
		}
	}

	@Reference
	private ConfigurationProvider _configurationProvider;

	@Reference
	private ResourcePermissionPersistence _resourcePermissionPersistence;

	@Reference
	private RoleLocalService _roleLocalService;

}