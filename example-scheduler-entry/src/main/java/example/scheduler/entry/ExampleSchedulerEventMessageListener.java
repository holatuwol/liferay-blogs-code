package example.scheduler.entry;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageListenerException;
import com.liferay.portal.kernel.module.framework.ModuleServiceLifecycle;
import com.liferay.portal.kernel.scheduler.SchedulerEngineHelper;
import com.liferay.portal.kernel.scheduler.SchedulerEntry;
import com.liferay.portal.kernel.scheduler.SchedulerEntryImpl;
import com.liferay.portal.kernel.scheduler.SchedulerException;
import com.liferay.portal.kernel.scheduler.TimeUnit;
import com.liferay.portal.kernel.scheduler.Trigger;
import com.liferay.portal.kernel.scheduler.TriggerFactory;
import com.liferay.portal.kernel.scheduler.TriggerState;
import com.liferay.portal.kernel.scheduler.messaging.SchedulerEventMessageListener;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import java.util.Map;

@Component(
	configurationPid = "example.scheduler.entry.ExampleSchedulerConfiguration",
	immediate = true, service = SchedulerEventMessageListener.class
)
public class ExampleSchedulerEventMessageListener
	implements SchedulerEventMessageListener {

	// Common Boilerplate: TriggerFactory

	@Reference(unbind = "-")
	private volatile TriggerFactory _triggerFactory;

	// Common Boilerplate: getSchedulerEntry

	protected SchedulerEntry getSchedulerEntry(Map<String, Object> properties) {
		ExampleSchedulerConfiguration configuration =
			ConfigurableUtil.createConfigurable(
				ExampleSchedulerConfiguration.class, properties);

		Class<?> clazz = getClass();

		String className = clazz.getName();

		Trigger trigger = _triggerFactory.createTrigger(
			className, className, null, null,
			configuration.interval(), TimeUnit.SECOND);

		return new SchedulerEntryImpl(className, trigger);
	}

	// Common Boilerplate: ModuleServiceLifecycle

	@Reference(target = ModuleServiceLifecycle.PORTAL_INITIALIZED, unbind = "-")
	private volatile ModuleServiceLifecycle _moduleServiceLifecycle;

	// Common Boilerplate: doReceive

	protected void doReceive(Message message) throws MessageListenerException {
		Class<?> clazz = getClass();
		String className = clazz.getName();

		System.out.println(className + " received message on schedule: " + message);
	}

	// Using SchedulerEventMessageListener: implement the interface, so that
	// Liferay can automatically handle registering the scheduled job

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		_schedulerEntry = getSchedulerEntry(properties);
	}

	@Override
	public SchedulerEntry getSchedulerEntry() {
		return _schedulerEntry;
	}

	private SchedulerEntry _schedulerEntry;

	// Using SchedulerEventMessageListener: add any functionality from
	// SchedulerEventMessageListenerWrapper to the receive method.

	@Override
	public void receive(Message message) throws MessageListenerException {
		doReceive(message);

		// Extra things done by SchedulerEventMessageListenerWrapper
		// that we want our scheduled job to do as well.

		try {
			_schedulerEngineHelper.auditSchedulerJobs(
				message, TriggerState.NORMAL);
		}
		catch (SchedulerException se) {
			throw new MessageListenerException(se);
		}
	}

	@Reference(unbind = "-")
	private volatile SchedulerEngineHelper _schedulerEngineHelper;

}