/**
 * © 2018 Liferay, Inc. <https://liferay.com>
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */

package com.liferay.content.targeting.override.job.servicewrapper;

import com.liferay.content.targeting.override.job.ScheduledBulkOperation;
import com.liferay.content.targeting.rule.visited.service.ContentVisitedLocalServiceWrapper;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.ServiceWrapper;

import java.sql.SQLException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Minhchau Dang
 */
@Component(service = ServiceWrapper.class)
public class ContentVisitedLocalService
	extends ContentVisitedLocalServiceWrapper {

	public ContentVisitedLocalService() {
		super(null);
	}

	@Override
	public void checkEvents() throws PortalException {
		try {
			_scheduledBulkOperation.execute();
		}
		catch (SQLException sqle) {
			throw new PortalException(sqle);
		}
	}

	@Reference(
		target = "(model.class=com.liferay.content.targeting.rule.visited.model.ContentVisited)"
	)
	private ScheduledBulkOperation _scheduledBulkOperation;

}