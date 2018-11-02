/**
 * © 2018 Liferay, Inc. <https://liferay.com>
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */

package com.liferay.content.targeting.override.job.sql;

import com.liferay.content.targeting.anonymous.users.service.AnonymousUserLocalService;
import com.liferay.content.targeting.anonymous.users.service.persistence.AnonymousUserPersistence;
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
		"model.class=com.liferay.content.targeting.anonymous.users.model.AnonymousUser",
		"service.ranking:Integer=100"
	},
	service = ScheduledBulkOperation.class
)
public class AnonymousUserBulkDeleteBySQL extends BulkDeleteBySQL {

	@Override
	protected void clearCache(String tableName) {
		if (Objects.equals(getTableName(), tableName)) {
			anonymousUserPersistence.clearCache();
		}
	}

	@Override
	protected DataSource getDataSource(String tableName) {
		if (Objects.equals(getTableName(), tableName)) {
			return anonymousUserPersistence.getDataSource();
		}

		throw new IllegalArgumentException(tableName);
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

	@Reference
	protected AnonymousUserLocalService anonymousUserLocalService;

	@Reference
	protected AnonymousUserPersistence anonymousUserPersistence;

}