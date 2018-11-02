/**
 * © 2018 Liferay, Inc. <https://liferay.com>
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */

package com.liferay.content.targeting.override.job.dynamicquery;

import com.liferay.content.targeting.model.AnonymousUserUserSegment;
import com.liferay.content.targeting.override.job.ScheduledBulkOperation;
import com.liferay.content.targeting.service.AnonymousUserUserSegmentLocalService;
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
		"model.class=com.liferay.content.targeting.model.AnonymousUserUserSegment",
		"service.ranking:Integer=50"
	},
	service = ScheduledBulkOperation.class
)
public class AnonymousUserUserSegmentBulkDeleteByActionableDynamicQuery
	extends BulkDeleteByActionableDynamicQuery<AnonymousUserUserSegment> {

	@Override
	protected AnonymousUserUserSegment delete(
		AnonymousUserUserSegment anonymousUserUserSegment) {

		return anonymousUserUserSegmentLocalService.
			deleteAnonymousUserUserSegment(anonymousUserUserSegment);
	}

	@Override
	protected ActionableDynamicQuery getActionableDynamicQuery() {
		return anonymousUserUserSegmentLocalService.getActionableDynamicQuery();
	}

	@Override
	protected String getDateColumnName() {
		return "modifiedDate";
	}

	@Override
	protected Date getMaxDate() throws PortalException {
		return anonymousUserUserSegmentLocalService.getMaxAge();
	}

	@Override
	protected String getTableName() {
		return "CT_AnonymousUserUserSegment";
	}

	@Override
	@Reference
	protected void setPortal(Portal portal) {
		super.setPortal(portal);
	}

	@Reference
	protected AnonymousUserUserSegmentLocalService
		anonymousUserUserSegmentLocalService;

}