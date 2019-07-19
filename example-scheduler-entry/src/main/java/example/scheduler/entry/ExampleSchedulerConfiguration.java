package example.scheduler.entry;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

@ExtendedObjectClassDefinition(category = "collaboration")
@Meta.OCD(id = "example.scheduler.entry.ExampleSchedulerConfiguration")
public interface ExampleSchedulerConfiguration {

	@Meta.AD(deflt = "5", required=false)
	int interval();
}