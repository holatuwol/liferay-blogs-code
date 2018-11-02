/**
 * © 2018 Liferay, Inc. <https://liferay.com>
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */

package com.liferay.content.targeting.override.job.listener;

import com.liferay.content.targeting.override.job.ScheduledBulkOperation;
import com.liferay.content.targeting.override.job.ScheduledBulkOperationMessageListener;
import com.liferay.portal.kernel.scheduler.SchedulerEngineHelper;
import com.liferay.portal.kernel.scheduler.TriggerFactory;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Minhchau Dang
 */
@Component(
	configurationPid = "com.liferay.content.targeting.configuration.AnonymousUserUserSegmentServiceConfiguration",
	configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true,
	service = ScheduledBulkOperationMessageListener.class
)
public class AnonymousUserUserSegmentCheckEventsMessageListener
	extends ScheduledBulkOperationMessageListener {

	@Activate
	protected void activate(Map<String, Object> properties) {
		registerScheduledJob(
			properties, "anonymousUserUserSegmentCheckInterval", 1,
			"anonymousUserUserSegmentCheckTimeUnit");
	}

	@Deactivate
	protected void deactivate() {
		unregisterScheduledJob();
	}

	@Modified
	protected void modified(Map<String, Object> properties) {
		unregisterScheduledJob();

		registerScheduledJob(
			properties, "anonymousUserUserSegmentCheckInterval", 1,
			"anonymousUserUserSegmentCheckTimeUnit");
	}

	@Override
	@Reference(
		target = "(model.class=com.liferay.content.targeting.model.AnonymousUserUserSegment)"
	)
	protected void setScheduledBulkOperation(
		ScheduledBulkOperation scheduledBulkOperation) {

		super.setScheduledBulkOperation(scheduledBulkOperation);
	}

	@Override
	@Reference
	protected void setSchedulerEngineHelper(
		SchedulerEngineHelper schedulerEngineHelper) {

		super.setSchedulerEngineHelper(schedulerEngineHelper);
	}

	@Override
	@Reference
	protected void setTriggerFactory(TriggerFactory triggerFactory) {
		super.setTriggerFactory(triggerFactory);
	}

}