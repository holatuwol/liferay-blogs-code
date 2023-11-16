package com.example.roleconjunction.service;

import com.example.roleconjunction.service.util.RoleConjunctionHelper;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Resource;
import com.liferay.portal.kernel.service.ResourcePermissionLocalServiceWrapper;
import com.liferay.portal.kernel.service.ServiceWrapper;
import com.liferay.portal.kernel.util.ArrayUtil;

import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Minhchau Dang
 */
@Component(service = ServiceWrapper.class)
public class RoleConjunctionResourcePermissionLocalServiceWrapper
	extends ResourcePermissionLocalServiceWrapper {

	@Override
	public boolean hasResourcePermission(
			List<Resource> resources, long[] roleIds, String actionId)
		throws PortalException {

		for (Resource resource : resources) {
			if (!_roleConjunctionHelper.isEnabled(
					resource.getCompanyId(), resource.getName())) {

				return getWrappedService().hasResourcePermission(
					resources, roleIds, actionId);
			}
		}

		Map<String, List<Long>> roleIdsBySubtype =
			_roleConjunctionHelper.getRoleIdsBySubtype(resources, roleIds);

		if (roleIdsBySubtype.isEmpty()) {
			return false;
		}

		for (List<Long> subtypeRoleIds : roleIdsBySubtype.values()) {
			if (subtypeRoleIds.isEmpty() ||
				!getWrappedService().hasResourcePermission(
					resources, ArrayUtil.toLongArray(subtypeRoleIds),
					actionId)) {

				return false;
			}
		}

		return true;
	}

	@Override
	public boolean hasResourcePermission(
			long companyId, String name, int scope, String primKey,
			long[] roleIds, String actionId)
		throws PortalException {

		if (!_roleConjunctionHelper.isEnabled(companyId, name)) {
			return getWrappedService().hasResourcePermission(
				companyId, name, scope, primKey, roleIds, actionId);
		}

		Map<String, List<Long>> roleIdsBySubtype =
			_roleConjunctionHelper.getRoleIdsBySubtype(
				companyId, name, scope, primKey, roleIds);

		if (roleIdsBySubtype.isEmpty()) {
			return false;
		}

		for (List<Long> subtypeRoleIds : roleIdsBySubtype.values()) {
			if (subtypeRoleIds.isEmpty() ||
				!getWrappedService().hasResourcePermission(
					companyId, name, scope, primKey,
					ArrayUtil.toLongArray(subtypeRoleIds), actionId)) {

				return false;
			}
		}

		return true;
	}

	@Reference
	private RoleConjunctionHelper _roleConjunctionHelper;

}