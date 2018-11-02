/**
 * © 2018 Liferay, Inc. <https://liferay.com>
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */

package com.liferay.content.targeting.override.job.sql;

import com.liferay.content.targeting.override.job.ScheduledBulkOperation;
import com.liferay.portal.dao.orm.common.SQLTransformer;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.sql.DataSource;

/**
 * @author Minhchau Dang
 */
public abstract class BulkDeleteBySQL implements ScheduledBulkOperation {

	@Override
	public void execute() throws PortalException, SQLException {
		Date maxDate = getMaxDate();

		_log.fatal(
			String.format(
				"Checking %s for entries with %s < %s", getTableName(),
				getDateColumnName(), maxDate));

		Map<Long, SortedSet<Timestamp>> timestampsMap = null;

		DB db = DBManagerUtil.getDB();

		if ((db.getDBType() == DBType.MYSQL) && (db.getMajorVersion() < 8)) {
			timestampsMap = getTimestampsMapSQL1999(maxDate);
		}
		else {
			timestampsMap = getTimestampsMapSQL2003(maxDate);
		}

		execute(timestampsMap);
	}

	@Override
	public int getBatchSize() {
		return 10000;
	}

	protected abstract void clearCache(String tableName);

	protected void execute(Map<Long, SortedSet<Timestamp>> timestampsMap)
		throws SQLException {

		String deleteSQL = String.format(
			"DELETE FROM %s WHERE companyId = ? AND %s < ?", getTableName(),
			getDateColumnName());

		DataSource dataSource = getDataSource(getTableName());

		try (Connection connection = dataSource.getConnection();
			PreparedStatement ps = connection.prepareStatement(deleteSQL)) {

			connection.setAutoCommit(true);

			for (Map.Entry<Long, SortedSet<Timestamp>> entry :
					timestampsMap.entrySet()) {

				long companyId = entry.getKey();
				SortedSet<Timestamp> timestamps = entry.getValue();

				ps.setLong(1, companyId);

				for (Timestamp timestamp : timestamps) {
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

	protected abstract DataSource getDataSource(String tableName);

	protected abstract String getDateColumnName();

	protected abstract Date getMaxDate() throws PortalException;

	protected abstract String getTableName();

	protected Map<Long, SortedSet<Timestamp>> getTimestampsMapSQL1999(
			Date maxDate)
		throws SQLException {

		String dateSQL = String.format(
			"SELECT companyId, %s FROM %s WHERE %s < ? ORDER BY companyId, %s",
			getDateColumnName(), getTableName(), getDateColumnName(),
			getDateColumnName());

		Map<Long, SortedSet<Timestamp>> timestampsMap = new HashMap<>();

		DataSource dataSource = getDataSource(getTableName());

		try (Connection connection = dataSource.getConnection();
			PreparedStatement ps = connection.prepareStatement(dateSQL)) {

			ps.setTimestamp(1, new Timestamp(maxDate.getTime()));

			long oldCompanyId = 0;
			int currentCount = 0;

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					long companyId = rs.getLong(1);

					if (companyId != oldCompanyId) {
						oldCompanyId = companyId;

						currentCount = 0;
					}

					SortedSet<Timestamp> timestamps = timestampsMap.get(
						companyId);

					if (timestamps == null) {
						timestamps = new TreeSet<>();

						timestamps.add(new Timestamp(maxDate.getTime()));

						timestampsMap.put(companyId, timestamps);
					}

					if ((++currentCount % getBatchSize()) == 0) {
						timestamps.add(rs.getTimestamp(2));
					}
				}
			}
		}

		return timestampsMap;
	}

	protected Map<Long, SortedSet<Timestamp>> getTimestampsMapSQL2003(
			Date maxDate)
		throws SQLException {

		String rowNumber = String.format(
			"ROW_NUMBER() OVER (PARTITION BY companyId ORDER BY %s)",
			getDateColumnName());

		String innerSQL = String.format(
			"(SELECT companyId, %s, %s AS rowNumber FROM %s WHERE %s < ?)",
			getDateColumnName(), rowNumber, getTableName(),
			getDateColumnName());

		String dateSQL = String.format(
			"SELECT companyId, %s FROM %s WHERE MOD(rowNumber, %d) = 0",
			getDateColumnName(), innerSQL, getBatchSize());

		dateSQL = SQLTransformer.transform(dateSQL);

		Map<Long, SortedSet<Timestamp>> timestampsMap = new HashMap<>();

		DataSource dataSource = getDataSource(getTableName());

		try (Connection connection = dataSource.getConnection();
			PreparedStatement ps = connection.prepareStatement(dateSQL)) {

			ps.setTimestamp(1, new Timestamp(maxDate.getTime()));

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					long companyId = rs.getLong(1);

					SortedSet<Timestamp> timestamps = timestampsMap.get(
						companyId);

					if (timestamps == null) {
						timestamps = new TreeSet<>();

						timestamps.add(new Timestamp(maxDate.getTime()));

						timestampsMap.put(companyId, timestamps);
					}

					timestamps.add(rs.getTimestamp(2));
				}
			}
		}

		return timestampsMap;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		BulkDeleteBySQL.class);

}