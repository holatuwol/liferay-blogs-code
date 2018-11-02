/**
 * © 2018 Liferay, Inc. <https://liferay.com>
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */

package com.liferay.content.targeting.override.job.dynamicquery;

import com.liferay.content.targeting.anonymous.users.model.AnonymousUser;
import com.liferay.content.targeting.anonymous.users.service.AnonymousUserLocalService;
import com.liferay.content.targeting.override.job.ScheduledBulkOperation;
import com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.util.Portal;

import java.util.Date;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Minhchau Dang
 */
@Component(
	property = {
		"model.class=com.liferay.content.targeting.anonymous.users.model.AnonymousUser",
		"service.ranking:Integer=50"
	},
	service = ScheduledBulkOperation.class
)
public class AnonymousUserBulkDeleteByActionableDynamicQuery
	extends BulkDeleteByActionableDynamicQuery<AnonymousUser> {

	@Override
	protected AnonymousUser delete(AnonymousUser anonymousUser) {
		return anonymousUserLocalService.deleteAnonymousUser(anonymousUser);
	}

	@Override
	protected ActionableDynamicQuery getActionableDynamicQuery() {
		return anonymousUserLocalService.getActionableDynamicQuery();
	}

	@Override
	protected String getDateColumnName() {
		return "createDate";
	}

	@Override
	protected Date getMaxDate() throws PortalException {
		return anonymousUserLocalService.getMaxAge();
	}

	@Override
	protected String getTableName() {
		return "CT_AU_AnonymousUser";
	}

	@Override
	@Reference
	protected void setPortal(Portal portal) {
		super.setPortal(portal);
	}

	@Reference
	protected AnonymousUserLocalService anonymousUserLocalService;

}