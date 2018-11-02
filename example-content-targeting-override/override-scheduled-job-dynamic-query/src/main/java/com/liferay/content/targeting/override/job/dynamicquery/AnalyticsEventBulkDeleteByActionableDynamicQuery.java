/**
 * © 2018 Liferay, Inc. <https://liferay.com>
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */

package com.liferay.content.targeting.override.job.dynamicquery;

import com.liferay.content.targeting.analytics.model.AnalyticsEvent;
import com.liferay.content.targeting.analytics.service.AnalyticsEventLocalService;
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
		"model.class=com.liferay.content.targeting.analytics.model.AnalyticsEvent",
		"service.ranking:Integer=50"
	},
	service = ScheduledBulkOperation.class
)
public class AnalyticsEventBulkDeleteByActionableDynamicQuery
	extends BulkDeleteByActionableDynamicQuery<AnalyticsEvent> {

	@Override
	protected AnalyticsEvent delete(AnalyticsEvent analyticsEvent) {
		return analyticsEventLocalService.deleteAnalyticsEvent(analyticsEvent);
	}

	@Override
	protected ActionableDynamicQuery getActionableDynamicQuery() {
		return analyticsEventLocalService.getActionableDynamicQuery();
	}

	@Override
	protected String getDateColumnName() {
		return "createDate";
	}

	@Override
	protected Date getMaxDate() throws PortalException {
		return analyticsEventLocalService.getMaxAge();
	}

	@Override
	protected String getTableName() {
		return "CT_Analytics_AnalyticsEvent";
	}

	@Override
	@Reference
	protected void setPortal(Portal portal) {
		super.setPortal(portal);
	}

	@Reference
	protected AnalyticsEventLocalService analyticsEventLocalService;

}