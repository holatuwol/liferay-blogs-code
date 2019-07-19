package example.scheduler.entry;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.messaging.BaseMessageListener;
import com.liferay.portal.kernel.messaging.DestinationNames;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageListenerException;
import com.liferay.portal.kernel.module.framework.ModuleServiceLifecycle;
import com.liferay.portal.kernel.scheduler.SchedulerEngineHelper;
import com.liferay.portal.kernel.scheduler.SchedulerEntry;
import com.liferay.portal.kernel.scheduler.SchedulerEntryImpl;
import com.liferay.portal.kernel.scheduler.TimeUnit;
import com.liferay.portal.kernel.scheduler.Trigger;
import com.liferay.portal.kernel.scheduler.TriggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import java.util.Map;

@Component(
	configurationPid = "example.scheduler.entry.ExampleSchedulerConfiguration",
	immediate = true, service = ExampleMessageListener.class
)
public class ExampleMessageListener extends BaseMessageListener {

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

	// Using BaseMessageListener: register the component on activation, and
	// unregister the component on deactivation.

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		_schedulerEngineHelper.register(
			this, getSchedulerEntry(properties),
			DestinationNames.SCHEDULER_DISPATCH);
	}

	@Deactivate
	protected void deactivate() {
		_schedulerEngineHelper.unregister(this);
	}

	@Reference(unbind = "-")
	private volatile SchedulerEngineHelper _schedulerEngineHelper;

}
