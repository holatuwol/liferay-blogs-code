Changing the Behavior of Scheduled Jobs
=======================================

Part of content targeting involves scheduled jobs that periodically sweep through several tables in order to remove older data. From a modeling perspective, this is as if content targeting were to make the assumption that all of those older events have a weight of zero, and therefore it does not need to store them or load them for modeling purposes.

If we wanted to evaluate whether this assumption is valid, we would ask questions like how much accuracy you lose by making that assumption. For example, is it similar to the small amount of accuracy you lose by identifying stop words for your content and removing them from a search index, or is it much more substantial? If you wanted to find out with an experiment, how would you design the A/B test to detect what you anticipate to be a very small effect size?

However, rather than look in detail at the *assumption*, today we're going to look at some problems with the assumption's *implementation* as a scheduled job.

**Note**: If you'd like to take a look at the proof of concept code being outlined in this post, you can check it out under `example-content-targeting-override <https://github.com/holatuwol/liferay-blogs-code/tree/master/example-content-targeting-override>`__ in my blogs code samples repository. The proof of concept has the following bundles:

* ``com.liferay.portal.component.blacklist.internal.ComponentBlacklistConfiguration.config``: a sample component blacklist configuration which disables the existing Liferay scheduled jobs for removing older data
* ``override-scheduled-job``: provides an interface ``ScheduledBulkOperation`` and a base class ``ScheduledBulkOperationMessageListener``
* ``override-scheduled-job-listener``: a sample which consumes the configurations of the existing scheduled jobs to pass to ``ScheduledBulkOperationMessageListener``
* ``override-scheduled-job-dynamic-query``: a sample implementation of ``ScheduledBulkOperation`` that provides the fix submitted for `WCM-1490 <https://issues.liferay.com/browse/WCM-1490>`__
* ``override-scheduled-job-sql``: a sample implementation of ``ScheduledBulkOperation`` that uses regular SQL to avoid one at a time deletes (assumes no model listeners exist on the audience targeting models)
* ``override-scheduled-job-service-wrapper``: a sample which consumes the ``ScheduledBulkOperation`` implementations in a service wrapper

Understanding the Problem
-------------------------

We have four OSGi components responsible for content targeting's scheduled jobs to remove older data.

* ``com.liferay.content.targeting.analytics.internal.messaging.CheckEventsMessageListener``
* ``com.liferay.content.targeting.anonymous.users.internal.messaging.CheckAnonymousUsersMessageListener``
* ``com.liferay.content.targeting.internal.messaging.CheckAnonymousUserUserSegmentsMessageListener``
* ``com.liferay.content.targeting.rule.visited.internal.messaging.CheckEventsMessageListener``


Each of the scheduled jobs makes a service call (which by default, encapsulates the operation in a single transaction) to a total of five service builder services that perform the work for those scheduled jobs. Each of these service calls is implemented as an ``ActionableDynamicQuery`` in order to perform the deletion.

* ``com.liferay.content.targeting.analytics.service.AnalyticsEventLocalService``
* ``com.liferay.content.targeting.anonymous.users.service.AnonymousUserLocalService``
* ``com.liferay.content.targeting.service.AnonymousUserUserSegmentLocalService``
* ``com.liferay.content.targeting.rule.visited.service.ContentVisitedLocalService``
* ``com.liferay.content.targeting.rule.visited.service.PageVisitedLocalService``

These service builder services ultimately delete older data from six tables.

* ``CT_Analytics_AnalyticsEvent``
* ``CT_Analytics_AnalyticsReferrer``
* ``CT_AU_AnonymousUser``
* ``CT_AnonymousUserUserSegment``
* ``CT_Visited_ContentVisited``
* ``CT_Visited_PageVisited``

If you have enough older data in any of these tables, the large transaction used for the mass deletion will eventually overwhelm the database transaction log and cause the transaction to be rolled back (in other words, no data will be deleted). Because the rollback occurs due to having too much data, and none of this data was successfully deleted, this rollback will repeat with every execution of the scheduled job, ultimately resulting in a very costly attempt to delete a lot of data, with no data ever being successfully deleted.

(**Note**: With `WCM-1309 <https://issues.liferay.com/browse/WCM-1309>`__, content targeting for Liferay 7.1 works around this problem by allowing the check to run more frequently, which theoretically prevents you from getting too much data in these tables, assuming you started using content targeting with Liferay 7.1 rather than in earlier releases.)

Implementing a Solution
-----------------------

When we convert our problem statement into something actionable, we can say that our goal is to update either the OSGi components or the service builder services (or both) so that the scheduled jobs which performs mass deletions do so across multiple smaller transactions. This will allow the transaction to succeed.

Step 0: Installing Dependencies
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

First, before we can even think about writing an implementation, we need to be able to compile that implementation. To do that, you'll need the API bundles (by convention, Liferay names these as ``.api`` bundles) for Audience Targeting.

.. code-block:: groovy

	compileOnly group: "com.liferay.content-targeting", name: "com.liferay.content.targeting.analytics.api"
	compileOnly group: "com.liferay.content-targeting", name: "com.liferay.content.targeting.anonymous.users.api"
	compileOnly group: "com.liferay.content-targeting", name: "com.liferay.content.targeting.api"
	compileOnly group: "com.liferay.content-targeting", name: "com.liferay.content.targeting.rule.visited.api"

With that in mind, our first road block becomes apparent when we check `repository.liferay.com <https://repository-cdn.liferay.com/nexus/content/repositories/liferay-public-releases/com/liferay/content-targeting/>`__ for our dependencies: one of the API bundles (``com.liferay.content.targeting.rule.visited.api``) is not available, because it's considered part of the enterprise release rather than the community release. To work around that problem, you will need to install all of the artifacts from the release ``.lpkg`` into a Maven repository and use those artifacts in your build scripts.

This isn't fundamentally difficult to do, as one of my previous blog posts on `Using Private Module Binaries as Dependencies <https://community.liferay.com/blogs/-/blogs/using-private-module-binaries-as-dependencies>`__ describes. However, because Liferay Audience Targeting currently lives outside of the main Liferay repository there are two wrinkles: (1) the modules in the Audience Targeting distribution don't provide the same hints in their manifests about whether they are available in a public repository or not, and (2) looking up the version each time is a pain.

To address both of these problems, I've augmented the script to ignore the manifest headers and to generate (and install) a Maven BOM from the ``.lpkg``. You can find that augmented script here: `lpkg2bom <https://github.com/holatuwol/liferay-faster-deploy/blob/master/lpkg2bom>`__. After putting it in the same folder as the ``.lpkg``, you run it as follows:

.. code-block:: bash

	./lpkg2bom com.liferay.content-targeting "Liferay Audience Targeting.lpkg"

Assuming you're using the `Target Platform Gradle Plugin <https://dev.liferay.com/develop/reference/-/knowledge_base/7-0/target-platform-gradle-plugin>`__, you'd then add this to the ``dependencies`` section in your parent ``build.gradle``:

.. code-block:: groovy

	targetPlatformBoms group: "com.liferay.content-targeting", name: "liferay.audience.targeting.lpkg.bom", version: "2.1.2"

If you're using the Spring dependency management plugin, you'd add these to the ``imports`` section of the ``dependencyManagement`` section in your parent ``build.gradle``.

.. code-block:: groovy

	mavenBom "com.liferay.content-targeting:liferay.audience.targeting.lpkg.bom:2.1.2"

(**Note**: Rumor has it that we plan to merge Audience Targeting into the main Liferay repository as part of Liferay 7.2, so it's possible that the marketplace compile time dependencies situation isn't going to be applicable to Audience Targeting in the future. It's still up in the air whether it gets merged into the main public repository or the main private repository, so it's also possible that compiling customizations to existing Liferay plugins will continue to be difficult in the future.)

Step 1: Managing Dependency Frameworks
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Knowing that we are dealing with service builder services, your initial plan might be to override the specific methods invoked by the scheduled jobs, because traditional Liferay wisdom is that the services are easy to customize in Liferay.

* ``com.liferay.content.targeting.analytics.service.AnalyticsEventLocalService``
* ``com.liferay.content.targeting.anonymous.users.service.AnonymousUserLocalService``
* ``com.liferay.content.targeting.service.AnonymousUserUserSegmentLocalService``
* ``com.liferay.content.targeting.rule.visited.service.ContentVisitedLocalService``
* ``com.liferay.content.targeting.rule.visited.service.PageVisitedLocalService``

If you attempt this, you will be blindslided by a really difficult part of the Liferay DXP learning curve: the intermixing of multiple dependency management approaches (Spring, Apache Felix Dependency Manager, Declarative Services, etc.), and how that leads to race conditions when dealing with code that runs at `component activation <https://osgi.org/specification/osgi.cmpn/7.0.0/service.component.html#service.component-activation>`__. More succinctly, you will end up needing to find a way to control which happens first: your new override of the service builder service being consumed by the OSGi component firing the scheduled job, or the scheduled job firing for the first time.

Rather than try to solve the problem, you can work around it by disabling the existing scheduled job via a `component blacklist <https://dev.liferay.com/discover/portal/-/knowledge_base/7-0/blacklisting-osgi-modules>`__ (relying on its status as a static bundle, which means it has a lower `start level <https://osgi.org/specification/osgi.core/7.0.0/framework.startlevel.html>`__ than standard modules), and then start a new scheduled job that consumes your custom implementation.

.. code-block:: text

	blacklistComponentNames=["com.liferay.content.targeting.analytics.internal.messaging.CheckEventsMessageListener","com.liferay.content.targeting.anonymous.users.internal.messaging.CheckAnonymousUsersMessageListener","com.liferay.content.targeting.internal.messaging.CheckAnonymousUserUserSegmentsMessageListener","com.liferay.content.targeting.rule.visited.internal.messaging.CheckEventsMessageListener"]

Let's take a moment to reflect on this solution design. Given that overriding the service builder service brings us back into a world where we're dealing with multiple dependency management frameworks, it makes more sense to separate the implementation from service builder entirely. Namely, we want to move from a world that's a mixture of Spring and OSGi into a world that is pure OSGi.

Step 2: Setting up the New Scheduled Jobs
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Like all scheduled jobs, each of these scheduled jobs will register itself to the scheduler, asking the scheduler to call it at some frequency.

.. code-block:: java

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

If you're familiar only with older versions of Liferay, it's important to note that we don't control the frequency of scheduled jobs via portal properties, but rather with the same steps that are outlined in the tutorial, `Making Your Applications Configurable <https://dev.liferay.com/develop/tutorials/-/knowledge_base/7-0/making-your-applications-configurable>`__.

In theory, this would make it easy for you to check configuration values; simply get an instance of the ``Configurable`` object, and away you go. However, in the case of Audience Targeting, Liferay chose to make the configuration class and the implementation class private to the module. This means that we'll need to parse the configuration directly from the properties rather than be able to use nice configuration objects, and we'll have to manually code-in the default value that's listed in the annotation for the configuration interface class.

.. code-block:: java

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

With that boilerplate code out of the way, we assume that our listener will be provided with an implementation of the bulk deletion for our model. For simplicity, we'll call this implementation a ``ScheduledBulkOperation``, which has a method to perform the bulk operation, and a method that tells us how many entries it will attempt to delete at a time.

.. code-block:: java

	public interface ScheduledBulkOperation {

		public void execute() throws PortalException, SQLException;

		public int getBatchSize();

	}

To differentiate between different model classes, we'll assume that the ``ScheduledBulkOperation`` has a property ``model.class`` that tells us which model it's intended to bulk delete. Then, each of the scheduled jobs asks for its specific ``ScheduledBulkOperation`` by specifying a ``target`` attribute on its ``@Reference`` annotation.

.. code-block:: java

	@Override
	@Reference(
		target = "(model.class=abc.def.XYZ)"
	)
	protected void setScheduledBulkOperation(ScheduledBulkOperation ScheduledBulkOperation) {
		super.setScheduledBulkOperation(ScheduledBulkOperation);
	}

Step 3: Breaking Up ``ActionableDynamicQuery``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

There are a handful of bulk updates in Liferay that don't actually need to be implemented as large transactions, and so as part of `LPS-45839 <https://issues.liferay.com/browse/LPS-45839>`__, we added an (undocumented) feature to allow you to break those a large transaction wrapped inside an ``ActionableDynamicQuery`` into multiple smaller transactions.

This was further simplified with the refactoring for `LPS-46123 <https://issues.liferay.com/browse/LPS-46123>`__, so that you could use a pre-defined constant in `DefaultActionableDynamicQuery <https://github.com/liferay/liferay-portal/blob/7.0.0-ga1/portal-kernel/src/com/liferay/portal/kernel/dao/orm/DefaultActionableDynamicQuery.java#L42>`__ and one method call to get that behavior:

.. code-block:: java

	actionableDynamicQuery.setTransactionConfig(
		DefaultActionableDynamicQuery.REQUIRES_NEW_TRANSACTION_CONFIG);

You can probably guess that as a result of the feature being undocumented, when we implemented the fix for `WCM-1388 <https://issues.liferay.com/browse/WCM-1388>`__ to use an ``ActionableDynamicQuery`` to fix an ``OutOfMemoryError``, we didn't make use of it. So even though we addressed the memory issue, if the transaction was large enough, the transaction was still doomed to be rolled back.

So now we'll look towards our first implementation of a ``ScheduledBulkOperation``: simply taking the existing code that leverages an ``ActionableDynamicQuery``, and make it use a new transaction for each interval of deletions.

For the most part, every implementation of our bulk deletion looks like the following, with a different service being called to get an ``ActionableDynamicQuery`` different name for the date column, and a different implementation of ``ActionableDynamicQuery.PerformAction`` for the individual delete methods.

.. code-block:: java

    ActionableDynamicQuery actionableDynamicQuery =
        xyzLocalService.getActionableDynamicQuery();

    actionableDynamicQuery.setAddCriteriaMethod(
        (DynamicQuery dynamicQuery) -> {
            Property companyIdProperty = PropertyFactoryUtil.forName(
                "companyId");
            Property createDateProperty = PropertyFactoryUtil.forName(
                dateColumnName);

            dynamicQuery.add(companyIdProperty.eq(companyId));
            dynamicQuery.add(createDateProperty.lt(maxDate));
        });

    actionableDynamicQuery.setPerformActionMethod(xyzDeleteMethod);

    actionableDynamicQuery.setTransactionConfig(
        DefaultActionableDynamicQuery.REQUIRES_NEW_TRANSACTION_CONFIG);

    actionableDynamicQuery.setInterval(batchSize);

    actionableDynamicQuery.performActions();

With that base boilerplate code, we can implement several bulk deletions for each model that accounts for each of those differences.

.. code-block:: java

	@Component(
		properties = "model.class=abc.def.XYZ",
		service = ScheduledBulkOperation.class
	)
	public class XYZScheduledBulkOperationByActionableDynamicQuery
		extends ScheduledBulkOperationByActionableDynamicQuery<XYZ> {
	}

Step 4: Bypassing the Persistence Layer
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If you've worked with Liferay service builder, you know that almost all non-upgrade code that lives in Liferay's code base operates on entities one at a time. Naturally, anything implemented with ``ActionableDynamicQuery`` has the same limitation.

This happens partly because there are no foreign keys (I don't know why this is the case either), partly because of an old incompatibility between Weblogic and Hibernate 3 (which was later addressed through a combination of `LPS-29145 <https://issues.liferay.com/browse/LPS-29145>`__ and `LPS-41524 <https://issues.liferay.com/browse/LPS-29145>`__, though the legacy setting lives on in ``hibernate.query.factory_class``), and partly because we still notify model listeners one at a time.

In theory, you can set the value of the legacy property to ``org.hibernate.hql.ast.ASTQueryTranslatorFactory`` to allow for Hibernate queries with bulk updates (among a lot of other nice features that are available in Hibernate by default, but not available in Liferay due to the default value of the portal property), and then use that approach instead of an ``ActionableDynamicQuery``. That's what we're hoping to eventually be able to do with `LPS-86407 <https://issues.liferay.com/browse/LPS-86407>`__.

However, if you know you don't have model listeners on the models you are working with (not always a safe assumption), a new option becomes available. You can choose to write everything with plain SQL outside of the persistence layer and not have to pay the Hibernate ORM cost, because nothing needs to know about the model.

This brings us to our second implementation of a ``ScheduledBulkOperation``: using plain SQL.

With the exception of the deletions for ``CT_Analytics_AnalyticsReferrer`` (which is effectively a cascade delete, emulated with code), the mass deletion of each of the other five tables can be thought of as having the following form:

.. code-block:: sql

	DELETE
	FROM CT_TableName
	WHERE companyId = ? AND dateColumnName < ?

Whether you delete in large batches or you delete in small batches, the query is the same. So let's assume that something provides us with a map where the key is a ``companyId``, and the value is a sorted set of the timestamps you will use for the deletions, where the timestamps are pre-divided into the needed batch size.

.. code-block:: java

	String deleteSQL = String.format(
		"DELETE FROM %s WHERE companyId = ? AND %s < ?", getTableName(),
		getDateColumnName());

    try (Connection connection = dataSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(deleteSQL)) {

        connection.setAutoCommit(true);

        for (Map.Entry<Long, SortedSet<Timestamp>> entry : timestampsMap.entrySet()) {
            long companyId = entry.getKey();
            SortedSet<Timestamp> timestamps = entry.getValue();

            ps.setLong(1, companyId);

            for (Timestamp timestamp : timestamps) {
                ps.setTimestamp(2, timestamp);

                ps.executeUpdate();

                clearCache(getTableName());
            }
        }
    }

So all that's left is to identify the breakpoints. In order to delete in small batches, choose the number of records that you want to delete in each batch (for example, 10000). Then, assuming you're running on a database other than MySQL 5.x, you can fetch the different breakpoints for those batches, though the modulus function will vary from database to database.

.. code-block:: sql

	SELECT companyId, dateColumnName FROM (
		SELECT companyId, dateColumnName, ROW_NUMBER() OVER (PARTITION BY companyId ORDER BY dateColumnName) AS rowNumber
		FROM CT_TableName
		WHERE dateColumnName < ?
	)
	WHERE MOD(rowNumber, 10000) = 0
	ORDER BY companyId, dateColumnName

If you're running a database like MySQL 5.x, you will need something similar to a stored procedure, or you can pull back all the ``companyId, dateColumnName`` records and discard anything that isn't located at a breakpoint. It's wasteful, but it's not that bad.

Finally, you sequentially execute the mass delete query for each of the different breakpoint values (and treat the original value as one extra breakpoint) rather than just the final value by itself. With that, you've effectively broken up one transaction into multiple transactions, and it will happen as fast as the database can manage, without having to pay the ORM penalty.

Expanding the Solution
----------------------

Now suppose you encounter the argument, "What happens if someone manually calls the method outside of the scheduled in order to clean up the older data?" At that point, overriding the sounds looks like a good idea.

Since we already have a ``ScheduledBulkOperation`` implementation, and because `service wrappers <https://dev.liferay.com/develop/tutorials/-/knowledge_base/7-0/customizing-liferay-services-service-wrappers>`__ are implemented as OSGi components, the implementation is trivial.

.. code-block:: java

	@Component(service = ServiceWrapper.class)
	public class CustomXYZEventLocalService extends XYZLocalServiceWrapper {

		public CustomXYZEventLocalService() {
			super(null);
		}

		@Override
		public void checkXYZ() throws PortalException {
			try {
				_scheduledBulkOperation.execute();
			}
			catch (SQLException sqle) {
				throw new PortalException(sqle);
			}
		}

		@Reference(
			target = "(model.class=abc.def.XYZ)"
		)
		private ScheduledBulkOperation _scheduledBulkOperation;
	}

Over-Expanding the Solution
---------------------------

With the code now existing in a service override, we have the following question: should we move the logic to whatever we use to override the service and then have the scheduled job consume the service rather than this extra ``ScheduledBulkOperation`` implementation? And if so, should we just leave the original scheduled job enabled?

With the above solution already put together, it's not obvious why you would ask that question. After all, if you have the choice to not mix Spring and OSGi, why are you choosing to mix them together again?

However, if you didn't declare the scheduled bulk update operation as its own component, and you had originally just embedded the logic inside of the listener, this question is perfectly natural to ask when you're refactoring for code reuse. Do you move the code to the service builder override, or do you create something else that both the scheduled job and the service consume? And it's not entirely obvious that you should almost never attempt the former.

Evaluating Service Wrappers
~~~~~~~~~~~~~~~~~~~~~~~~~~~

In order to know whether it's possible to consume our new service builder override from a scheduled job, you'll need to know the order of events for how a service wrapper is registered:

1. OSGi picks up your component, which declares that it provides the ``ServiceWrapper.class`` service
2. The ``ServiceTracker`` within `ServiceWrapperRegistry <https://github.com/liferay/liferay-portal/blob/7.0.6-ga7/portal-impl/src/com/liferay/portal/deploy/hot/ServiceWrapperRegistry.java#L132-L173>`__ is notified that your component exists
3. The ``ServiceTracker`` creates a ``ServiceBag``, passing your service wrapper as an argument
4. The `ServiceBag <https://github.com/liferay/liferay-portal/blob/7.0.6-ga7/portal-impl/src/com/liferay/portal/deploy/hot/ServiceBag.java#L62-L85>`__ injects your service wrapper implementation into the Spring proxy object

Notice that when you follow the service wrapper tutorial, your service is not registered to OSGi under the interface it implements, because Liferay relies on the Spring proxy (not the original implementation) being published as an OSGi component. This is deliberate, because Liferay hasn't yet implemented a way to proxy OSGi components (though rumor has it that this is being planned for Liferay 7.2), and without that, you lose all of the benefits of the advices that are attached to services.

However, as a side-effect of this, this means that even though no components are notified of a new implementation of the service, all components are transparently updated to use your new service wrapper once the switch completes. What about your scheduled job? Well, until your service wrapper is injected into the Spring proxy, your scheduled job will still be calling the original service. In other words, we're back to having a race condition between all of the dependency management frameworks.

In order to fight against that race condition, you might consider manually registering the scheduled job after a delay, or duplicating the logic that exists in ``ServiceWrapperRegistry`` and ``ServiceBag`` and polling the proxy to find out when your service wrapper registered. However, all of that is really just hiding dependencies.

Evaluating Bundle Fragments
~~~~~~~~~~~~~~~~~~~~~~~~~~~

If you were still convinced that you could override the service and have your scheduled job invoke the service, you might consider overriding the existing service builder bean using a fragment bundle and ``ext-spring.xml``, as described in a past blog entry by David Nebinger on `OSGi Fragment Bundles <https://community.liferay.com/blogs/-/blogs/osgi-fragment-bundles>`__.

However, there are three key limitations of this approach.

1. You need a separate bundle fragment for each of the four bundles.
2. A bundle fragment can't register new OSGi components through Declarative Services.

The second limitation warrants additional discussion, because it's also another part of the OSGi learning curve. Namely, code that would work perfectly in a regular bundle will stop working if you move it to a bundle fragment, because a bundle fragment is never started (it only advances to the ``RESOLVED`` state).

Since it's a service builder plugin, one workaround for DXP is to use a Spring bean, where the Spring bean will get registered to OSGi automatically later in the initialization cycle. However, choosing this strategy means you shouldn't add ``@Component`` to your scheduled job class (otherwise, it gets instantiated by both Spring and OSGi, and that will get messy), and there are a few things you need to keep in mind as you're trying to manage the fact that you're playing with two dependency management frameworks.

* In order to get references to other Spring-managed components within the same bundle (for example, the service builder service you overrode), you should do it with ``ext-spring``
* In order to get references to Spring beans, you should use ``@BeanReference``
* In order to get references to OSGi components, you need to either (a) use static fields and `ServiceProxyFactory <https://github.com/liferay/liferay-portal/blob/7.0.6-ga7/portal-kernel/src/com/liferay/portal/kernel/util/ServiceProxyFactory.java>`__, as briefly mentioned in the tutorial on `Detecting Unresolved OSGi Components <https://dev.liferay.com/develop/tutorials/-/knowledge_base/7-0/detecting-unresolved-osgi-components#serviceproxyfactory>`__, or (b) use the Liferay registry API exported to the global classloader, as mentioned in the tutorial on `Using OSGi Services from EXT Plugins <https://dev.liferay.com/develop/tutorials/-/knowledge_base/7-0/using-osgi-services-from-ext-plugins>`__

Evaluating Marketplace Overrides
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Of course, if you were to inject the new service using ``ext-spring.xml`` using a marketplace override, as described in a past blog entry by David Nebinger on `Extending Liferay OSGi Modules <https://community.liferay.com/blogs/-/blogs/extending-liferay-osgi-modules>`__, you're able to register new components just fine.

However, there are still two key limitations of this approach.

1. You need a separate bundle for each of the four marketplace overrides.
2. Each code change requires a full server restart and a clean ``osgi/state`` folder.
3. You need to be fully aware that the increased flexibility of a marketplace override is similar to the increased flexibility of an EXT plugin.

In theory, the reason the second limitation exists is because marketplace overrides are scanned by the same code that scans ``.lpkg`` folders rather than through a regular bundle scanning mechanism, and that scan happens only once and only happens during the module framework initialization. In theory, you might be able to work around it by adding the ``osgi/marketplace/override`` folder to the ``module.framework.auto.deploy.dirs`` portal property. However, I don't know how this actually works in practice, because I've quietly accepted the documentation that says that restarts are necessary.

Reviewing the Solution
----------------------

To summarize, overriding Liferay scheduled jobs is fairly straightforward once you have all of the dependencies you need, assuming you're willing to accept the following two steps:

1. Disable the existing scheduled job
2. Create a new implementation of the work that scheduled job performs

If you reject these steps and try to play at the boundary of where different dependency management frameworks interact, you need to deal with the race conditions and complications that arise from that interaction.