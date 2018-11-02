/**
 * © 2018 Liferay, Inc. <https://liferay.com>
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */

package com.liferay.content.targeting.override.job;

import com.liferay.portal.kernel.exception.PortalException;

import java.sql.SQLException;

/**
 * @author Minhchau Dang
 */
public interface ScheduledBulkOperation {

	public void execute() throws PortalException, SQLException;

	public int getBatchSize();

}