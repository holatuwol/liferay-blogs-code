/**
 * © 2018 Liferay, Inc. <https://liferay.com>
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */

package com.liferay.content.targeting.override.job.dynamicquery;

import com.liferay.content.targeting.override.job.ScheduledBulkOperation;
import com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.DefaultActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Portal;

import java.sql.SQLException;

import java.util.Date;

/**
 * @author Minhchau Dang
 */
public abstract class BulkDeleteByActionableDynamicQuery<T>
	implements ScheduledBulkOperation {

	public BulkDeleteByActionableDynamicQuery() {
		_deleteMethod = this::delete;
	}

	@Override
	public void execute()
		throws PortalException,
			   SQLException {

		Date maxDate = getMaxDate();

		_log.fatal(
			String.format(
				"Checking %s for entries with %s < %s", getTableName(),
				getDateColumnName(), getMaxDate()));

		for (long companyId : _portal.getCompanyIds()) {
			ActionableDynamicQuery actionableDynamicQuery =
				getActionableDynamicQuery();

			actionableDynamicQuery.setAddCriteriaMethod(
				(DynamicQuery dynamicQuery) -> {
					Property companyIdProperty = PropertyFactoryUtil.forName(
						"companyId");
					Property createDateProperty = PropertyFactoryUtil.forName(
						getDateColumnName());

					dynamicQuery.add(companyIdProperty.eq(companyId));
					dynamicQuery.add(createDateProperty.lt(maxDate));
				});

			actionableDynamicQuery.setPerformActionMethod(_deleteMethod);

			actionableDynamicQuery.setTransactionConfig(
				DefaultActionableDynamicQuery.REQUIRES_NEW_TRANSACTION_CONFIG);

			actionableDynamicQuery.setInterval(getBatchSize());

			actionableDynamicQuery.performActions();
		}
	}

	@Override
	public int getBatchSize() {
		return 10000;
	}

	protected abstract T delete(T t);

	protected abstract ActionableDynamicQuery getActionableDynamicQuery();

	protected abstract String getDateColumnName();

	protected abstract Date getMaxDate() throws PortalException;

	protected abstract String getTableName();

	protected void setPortal(Portal portal) {
		_portal = portal;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		BulkDeleteByActionableDynamicQuery.class);

	private ActionableDynamicQuery.PerformActionMethod<T> _deleteMethod;
	private Portal _portal;

}