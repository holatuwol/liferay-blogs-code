/**
 * © 2018 Liferay, Inc. <https://liferay.com>
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */

package com.liferay.content.targeting.override.job.sql;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.Map;
import java.util.SortedSet;

import javax.sql.DataSource;

/**
 * @author Minhchau Dang
 */
public abstract class RelatedTableBulkDeleteBySQL extends BulkDeleteBySQL {

	@Override
	public void execute(Map<Long, SortedSet<Timestamp>> timestampsMap)
		throws SQLException {

		String subquery = String.format(
			"SELECT %s FROM %s WHERE companyId = ? AND %s < ?",
			getPrimaryKeyColumnName(), getTableName(), getDateColumnName());

		String relatedDeleteSQL = String.format(
			"DELETE FROM %s WHERE %s IN (%s)", getRelatedTableName(),
			getRelatedTableColumnName(), subquery);

		String deleteSQL = String.format(
			"DELETE FROM %s WHERE companyId = ? AND %s < ?", getTableName(),
			getDateColumnName());

		DataSource dataSource = getDataSource(getTableName());
		DataSource relatedDataSource = getDataSource(getRelatedTableName());

		try (Connection conn = dataSource.getConnection();
			PreparedStatement ps = conn.prepareStatement(deleteSQL);
			Connection relatedConn = relatedDataSource.getConnection();
			PreparedStatement relatedPS = relatedConn.prepareStatement(
				relatedDeleteSQL)) {

			relatedConn.setAutoCommit(true);
			conn.setAutoCommit(true);

			for (Map.Entry<Long, SortedSet<Timestamp>> entry :
					timestampsMap.entrySet()) {

				long companyId = entry.getKey();
				SortedSet<Timestamp> timestamps = entry.getValue();

				relatedPS.setLong(1, companyId);

				ps.setLong(1, companyId);

				for (Timestamp timestamp : timestamps) {
					_log.fatal(
						String.format(
							"Deleting %s entries with companyId = %d, %s < %s",
							getRelatedTableName(), companyId,
							getDateColumnName(), timestamp));

					relatedPS.setTimestamp(2, timestamp);

					relatedPS.executeUpdate();

					clearCache(getRelatedTableName());

					_log.fatal(
						String.format(
							"Deleting %s entries with companyId = %d, %s < %s",
							getTableName(), companyId, getDateColumnName(),
							timestamp));

					ps.setTimestamp(2, timestamp);

					ps.executeUpdate();

					clearCache(getTableName());
				}
			}
		}
	}

	protected abstract String getPrimaryKeyColumnName();

	protected abstract String getRelatedTableColumnName();

	protected abstract String getRelatedTableName();

	private static final Log _log = LogFactoryUtil.getLog(
		RelatedTableBulkDeleteBySQL.class);

}