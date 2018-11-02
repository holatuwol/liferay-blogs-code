/**
 * © 2018 Liferay, Inc. <https://liferay.com>
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */

package com.liferay.content.targeting.override.job;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.messaging.DestinationNames;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageListener;
import com.liferay.portal.kernel.messaging.MessageListenerException;
import com.liferay.portal.kernel.scheduler.SchedulerEngineHelper;
import com.liferay.portal.kernel.scheduler.SchedulerEntryImpl;
import com.liferay.portal.kernel.scheduler.TimeUnit;
import com.liferay.portal.kernel.scheduler.Trigger;
import com.liferay.portal.kernel.scheduler.TriggerFactory;
import com.liferay.portal.kernel.util.GetterUtil;

import java.sql.SQLException;

import java.util.Map;

/**
 * @author Minhchau Dang
 */
public class ScheduledBulkOperationMessageListener implements MessageListener {

	@Override
	public void receive(Message message) throws MessageListenerException {
		if (scheduledBulkOperation == null) {
			throw new MessageListenerException("No bulk delete operation set");
		}

		if (executing) {
			_log.fatal(
				String.format(
					"Scheduled job for %s is already running", getClassName()));

			return;
		}

		executing = true;

		try {
			scheduledBulkOperation.execute();
		}
		catch (PortalException pe) {
			throw new MessageListenerException(pe);
		}
		catch (SQLException sqle) {
			throw new MessageListenerException(sqle);
		}
		finally {
			executing = false;
		}
	}

	protected String getClassName() {
		Class<?> clazz = getClass();

		return clazz.getName();
	}

	protected void registerScheduledJob(int interval, TimeUnit timeUnit) {
		SchedulerEntryImpl schedulerEntry = new SchedulerEntryImpl();

		String className = getClassName();

		schedulerEntry.setEventListenerClass(className);

		Trigger trigger = triggerFactory.createTrigger(
			className, className, null, null, interval, timeUnit);

		schedulerEntry.setTrigger(trigger);

		_log.fatal(
			String.format(
				"Registering scheduled job for %s with frequency %d %s",
				className, interval, timeUnit));

		schedulerEngineHelper.register(
			this, schedulerEntry, DestinationNames.SCHEDULER_DISPATCH);
	}

	protected void registerScheduledJob(
		Map<String, Object> properties, String intervalPropertyName,
		int defaultInterval, String timeUnitPropertyName) {

		int interval = GetterUtil.getInteger(
			properties.get(intervalPropertyName), defaultInterval);

		TimeUnit timeUnit = TimeUnit.DAY;

		String timeUnitString = GetterUtil.getString(
			properties.get(timeUnitPropertyName));

		if (!timeUnitString.isEmpty()) {
			timeUnit = TimeUnit.valueOf(timeUnitString);
		}

		registerScheduledJob(interval, timeUnit);
	}

	protected void setScheduledBulkOperation(
		ScheduledBulkOperation scheduledBulkOperation) {

		this.scheduledBulkOperation = scheduledBulkOperation;
	}

	protected void setSchedulerEngineHelper(
		SchedulerEngineHelper schedulerEngineHelper) {

		this.schedulerEngineHelper = schedulerEngineHelper;
	}

	protected void setTriggerFactory(TriggerFactory triggerFactory) {
		this.triggerFactory = triggerFactory;
	}

	protected void unregisterScheduledJob() {
		_log.fatal(
			String.format(
				"Unregistering scheduled job for %s", getClassName()));

		schedulerEngineHelper.unregister(this);
	}

	protected volatile boolean executing;
	protected ScheduledBulkOperation scheduledBulkOperation;
	protected SchedulerEngineHelper schedulerEngineHelper;
	protected TriggerFactory triggerFactory;

	private static final Log _log = LogFactoryUtil.getLog(
		ScheduledBulkOperationMessageListener.class);

}