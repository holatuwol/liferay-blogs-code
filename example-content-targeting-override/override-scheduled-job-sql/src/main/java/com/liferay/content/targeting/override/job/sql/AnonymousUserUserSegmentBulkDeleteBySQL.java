/**
 * © 2018 Liferay, Inc. <https://liferay.com>
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */

package com.liferay.content.targeting.override.job.sql;

import com.liferay.content.targeting.override.job.ScheduledBulkOperation;
import com.liferay.content.targeting.service.AnonymousUserUserSegmentLocalService;
import com.liferay.content.targeting.service.persistence.AnonymousUserUserSegmentPersistence;
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
		"model.class=com.liferay.content.targeting.model.AnonymousUserUserSegment",
		"service.ranking:Integer=100"
	},
	service = ScheduledBulkOperation.class
)
public class AnonymousUserUserSegmentBulkDeleteBySQL extends BulkDeleteBySQL {

	@Override
	protected void clearCache(String tableName) {
		if (Objects.equals(getTableName(), tableName)) {
			anonymousUserUserSegmentPersistence.clearCache();
		}
	}

	@Override
	protected DataSource getDataSource(String tableName) {
		if (Objects.equals(getTableName(), tableName)) {
			return anonymousUserUserSegmentPersistence.getDataSource();
		}

		throw new IllegalArgumentException(tableName);
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

	@Reference
	protected AnonymousUserUserSegmentLocalService
		anonymousUserUserSegmentLocalService;

	@Reference
	protected AnonymousUserUserSegmentPersistence
		anonymousUserUserSegmentPersistence;

}