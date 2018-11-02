/**
 * © 2018 Liferay, Inc. <https://liferay.com>
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */

package com.liferay.content.targeting.override.job.sql;

import com.liferay.content.targeting.analytics.service.AnalyticsEventLocalService;
import com.liferay.content.targeting.analytics.service.persistence.AnalyticsEventPersistence;
import com.liferay.content.targeting.analytics.service.persistence.AnalyticsReferrerPersistence;
import com.liferay.content.targeting.override.job.ScheduledBulkOperation;
import com.liferay.portal.kernel.exception.PortalException;

import java.util.Date;
import java.util.Objects;

import javax.sql.DataSource;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Minhchau Dang
 */
@Component(
	property = {
		"model.class=com.liferay.content.targeting.analytics.model.AnalyticsEvent",
		"service.ranking:Integer=100"
	},
	service = ScheduledBulkOperation.class
)
public class AnalyticsEventBulkDeleteBySQL extends RelatedTableBulkDeleteBySQL {

	@Override
	protected void clearCache(String tableName) {
		if (Objects.equals(getTableName(), tableName)) {
			analyticsEventPersistence.clearCache();
		}

		if (Objects.equals(getRelatedTableName(), tableName)) {
			analyticsReferrerPersistence.clearCache();
		}
	}

	@Override
	protected DataSource getDataSource(String tableName) {
		if (Objects.equals(getTableName(), tableName)) {
			return analyticsEventPersistence.getDataSource();
		}

		if (Objects.equals(getRelatedTableName(), tableName)) {
			return analyticsReferrerPersistence.getDataSource();
		}

		throw new IllegalArgumentException(tableName);
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
	protected String getPrimaryKeyColumnName() {
		return "analyticsEventId";
	}

	@Override
	protected String getRelatedTableColumnName() {
		return "analyticsEventId";
	}

	@Override
	protected String getRelatedTableName() {
		return "CT_Analytics_AnalyticsReferrer";
	}

	@Override
	protected String getTableName() {
		return "CT_Analytics_AnalyticsEvent";
	}

	@Reference
	protected AnalyticsEventLocalService analyticsEventLocalService;

	@Reference
	protected AnalyticsEventPersistence analyticsEventPersistence;

	@Reference
	protected AnalyticsReferrerPersistence analyticsReferrerPersistence;

}