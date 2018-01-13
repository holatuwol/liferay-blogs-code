Keeping Customizations Up to Date with Liferay Source
=====================================================

.. contents:: :local:

Welcome to the fourth entry in a series about what to keep in mind when building Liferay from source. First, to recap the previous entries in this series from last year:

* `Getting Started with Building Liferay from Source <https://web.liferay.com/web/minhchau.dang/blog/-/blogs/getting-started-with-building-liferay-from-source>`__: How to get a clone of the Liferay central repository and how to build Liferay from source. Also some tools that can help you setup your IDE (whether it's Netbeans, Eclipse, or IntelliJ) to navigate that portal source.
* `Troubleshooting Liferay from Source <https://web.liferay.com/web/minhchau.dang/blog/-/blogs/troubleshooting-liferay-from-source>`__: How to make changes to the source code in Liferay's central repository and deploy changes to individual Liferay modules. Also some tips and tricks in troubleshooting issues within Liferay itself.
* `Deploying CE Clustering from Subrepositories <https://web.liferay.com/web/minhchau.dang/blog/-/blogs/deploy-ce-clustering-from-subrepositories>`__: How to take advantage of smaller repositories to deploy clustering to a CE bundle. How to maintain changes to the code base without having to clone or build the massive Liferay central repository.

Continuing onward from there, one of the practical reasons why would you might want to be able to build Liferay from source is simply to be able to keep your Liferay installation up to date.

However, keeping your Liferay installation up to date involves a lot more than just rebuilding Liferay from source. After all, while many of Liferay's customers start with the noble goal of staying up to date with Liferay releases (and they're given binaries that don't require them to build from source), for a variety of reasons, these updates wind up delayed.

Among these reasons is that Liferay is a platform you can customize. Yet, when you customize Liferay with an incomplete understanding of Liferay's release artifacts (and as a consequence, an incomplete understanding of your own release artifacts if they depend on Liferay's release artifacts), your customizations will mysteriously stop working when you apply an update. When this happens, your ability to apply the update gets stalled.

This entry will talk about some of the struggles that you are likely to encounter whenever you try to keep your own customizations up to date whenever you update Liferay. In order to provide you with a more concrete example that you can use to understand the different roadblocks, this entry will walk you through a customization that's compiled against an initial release of Liferay, and the hardships you're likely to face if you tried to deploy it with an incomplete understanding of Liferay's release artifacts.

A Minimal Customization, Part 1
-------------------------------

In this entry, we'll go over a simple customization. I say simple, but it's one that will have failed to initialize in all 34 of the past 34 fix packs. Additionally, even if you managed to make it work the first fix pack using naive approaches, you would have needed to update it again in 24 out of the following 33 fix packs. This would occur even if you wrote the code *exactly* as Liferay intended for you to write it.

This customization is deceptively simple: modifying Liferay's terms of use.

Why You Modify Terms of Use
~~~~~~~~~~~~~~~~~~~~~~~~~~~

There are many reasons to customize a terms of use, but one very prominent reason is fast approaching.

On May 28, 2018, the `General Data Protection Regulation (GDPR) <http://eur-lex.europa.eu/legal-content/EN/TXT/HTML/?uri=CELEX:32016R0679>`__, will go into effect. There are a great many things involved in compliance, and among them is the fact that those who collect data on citizens of the member countries of the European Union will be held to a higher standard when it comes to obtaining consent.

Chapter I, Article 4 of the GDPR defines consent as "any freely given, specific, informed and unambiguous indication of the data subject's wishes by which he or she, by a statement or by a clear affirmative action, signifies agreement to the processing of personal data relating to him or her".

With that in mind, a terms of use agreement is a simple way to receive consent, even though there are `internet memes <http://www.biggestlie.com>`__ about whether people actually read them, and `academic studies <https://papers.ssrn.com/sol3/papers.cfm?abstract_id=2757465>`__ showing that many college students probably do not.

How Terms of Use is Implemented
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

By default, Liferay records whether you have agreed to its terms of use through a single boolean flag in the database: ``agreedToTermsOfUse`` in the ``User_`` table.

When dispatching any portal request, `PortalRequestProcessor <https://github.com/liferay/liferay-portal/blob/7.0.4-ga5/portal-impl/src/com/liferay/portal/struts/PortalRequestProcessor.java#L730-L732>`__ will force any authenticated user who has not agreed to the terms of use through to the Terms of Use page.

To present the user with for the terms of use agreement, you can trace the logic for 7.0.x to a single JSP named `terms_of_use_default.jsp <https://github.com/liferay/liferay-portal/blob/7.0.4-ga5/portal-web/docroot/html/portal/terms_of_use_default.jsp>`__ that you can modify directly in the Liferay portal source code (or even from the release artifact binary).

How Terms of Use is Customized
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If modifying this single JSP inside of the Liferay web application archive is insufficient for your needs (for example, you want to use services provided by a module), Liferay provides a mechanism for more elaborate customizations: a `TermsOfUseContentProvider <https://github.com/liferay/liferay-portal/blob/7.0.4-ga5/portal-kernel/src/com/liferay/portal/kernel/util/TermsOfUseContentProvider.java>`__.

By default, Liferay provides you with a single example of this that allows you to configure a piece of web content to serve as the terms of use in place of the default terms of use.

In theory, because you can embed portlets inside of web content in the same way you embed them in a theme or layout template (`Embedding Portlets in Themes and Layout Templates <https://dev.liferay.com/develop/tutorials/-/knowledge_base/7-0/embedding-portlets-in-themes-and-layout-templates>`__), the default implementation of ``TermsOfUseContentProvider`` provided in ``journal-terms-of-use`` can be very flexible from a user interface perspective. In practice, until you've actually agreed to terms of use, a lot of requests that dispatch to pages do not work until you've agreed to those terms of use, and portlet requests always go to pages.

We can start creating a new one inside of a Liferay workspace using the following command:

.. code-block:: bash

	blade create -t service \
		-s com.liferay.portal.kernel.util.TermsOfUseContentProvider \
		-p com.example.termsofuse \
		-c ExampleTermsOfUseContentProvider \
		example-terms-of-use

If you check the interface (or you let your IDE populate all the methods in the interface so that it can compile), you find that ``TermsOfUseContentProvider`` requires implementing three methods:

* ``includeConfig``: This expects for you to use a ``RequestDispatcher`` to ``include`` a JSP. It is called from `portal-settings-web <https://github.com/liferay/liferay-portal/blob/7.0.4-ga5/modules/apps/foundation/portal-settings/portal-settings-web/src/main/resources/META-INF/resources/terms_of_use.jsp#L33>`__, and you can view the area that renders it by navigating to Control Panel > Instance Settings, and in the Configuration tab, scroll down to the Terms of Use section. The one you see by default comes from the `com.liferay.journal.terms.of.use <https://github.com/liferay/liferay-portal/tree/7.0.4-ga5/modules/apps/web-experience/journal/journal-terms-of-use>`__ module.
* ``includeView``: This expects for you to use a ``RequestDispatcher`` to ``include`` a JSP. It is called from `portal-web <https://github.com/liferay/liferay-portal/blob/7.0.4-ga5/portal-web/docroot/html/portal/terms_of_use.jsp#L39>`__, and you can view the area that renders it if you have a user that has not agreed to the terms of use or by navigating directly to ``/c/portal/terms_of_use``.
* ``getClassName``: On the surface, the method name suggests that one day, Liferay might allow you to have different terms of use for different types of assets (such as a separate terms of use for document library). However, at this time, this hasn't been implemented, and the lack of stable ``Map`` iteration also means that if you have multiple content providers with different class names, Liferay presents what is functionally equivalent to a random terms of use content provider for both view and configuration (`source code <https://github.com/liferay/liferay-portal/blob/7.0.4-ga5/portal-kernel/src/com/liferay/portal/kernel/util/TermsOfUseContentProviderRegistryUtil.java#L87-L96>`__).

As noted in the ``getClassName`` note above, the first thing you have to do before you even customize it is disable the existing implementation.

* If you are building from source, you can achieve this by deleting ``osgi/modules/com.liferay.journal.terms.of.use.jar`` and then removing the file ``modules/apps/web-experience/journal/journal-terms-of-use/.lfrbuild-portal`` so that it doesn't get deployed again when you rebuild Liferay from source.
* If you are using an older release rather than building from source, you can achieve this with an empty marketplace override of ``com.liferay.journal.terms.of.use.jar`` (namely, just a JAR with no classes), as described in `Overriding LPKG Files <https://dev.liferay.com/develop/tutorials/-/knowledge_base/7-0/overriding-lpkg-files>`__.
* If you are using an up to date release rather than building from source, you can achieve this in later versions of Liferay with `Blacklisting OSGi Modules <https://dev.liferay.com/discover/portal/-/knowledge_base/7-0/blacklisting-osgi-modules>`__, and either using the GUI or using a configuration file to blacklist the ``com.liferay.journal.terms.of.use`` module.

With that in mind, let's assume that we've done that, and that we'd create a new implementation of ``TermsOfUseContentProvider``. Here is what a set of empty method implementations might look like, which we would add to ``ExampleTermsOfUseContentProvider.java``:

.. code-block:: java

	import javax.servlet.http.HttpServletRequest;
	import javax.servlet.http.HttpServletResponse;

	// ...

	@Override
	public String getClassName() {
		System.out.println("Called getClassName()");

		return "";
	}

	@Override
	public void includeConfig(
			HttpServletRequest request, HttpServletResponse response)
		throws Exception {

		System.out.println("Called includeConfig(HttpServletRequest, HttpServletResponse)");
	}

	@Override
	public void includeView(
			HttpServletRequest request, HttpServletResponse response)
		throws Exception {

		System.out.println("Called includeView(HttpServletRequest, HttpServletResponse)");
	}

To get it to compile, we will need to update ``build.gradle`` to provide the dependencies that we need in order to compile these empty method implementations:

.. code-block:: groovy

	dependencies {
		compileOnly group: "com.liferay.portal", name: "com.liferay.portal.kernel", version: "2.0.0"
		compileOnly group: "javax.servlet", name: "javax.servlet-api", version: "3.0.1"
		compileOnly group: "org.osgi", name: "org.osgi.service.component.annotations", version: "1.3.0"
	}

Attempt to Deploy the Stub Implementation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

At this point, we have completed a stub implementation.

In general, whenever you work with a new extension point for the first time, you should stop as soon as you have a stub implementation and try a few small things to see if the extension point will work the way you expect it to. For a ``TermsOfUseContentProvider``. But as you will soon see, your first unwieldy obstacle is getting it to deploy at all.

If you invoke ``blade gw jar``, it will create the file ``build/libs/com.example.termsofuse-1.0.0.jar``. If you're using a Blade workspace, you can set ``liferay.workspace.home.dir`` in ``gradle.properties`` and use ``blade gw deploy`` to have it be copied to ``${liferay.home}/osgi/modules``, or you can manually copy this file to ``${liferay.home}/deploy``.

When you do so, you will see a message saying that the bundle is being processed, but the bundle never starts.

If you check with the Gogo shell (`Felix Gogo Shell <https://dev.liferay.com/develop/reference/-/knowledge_base/7-0/using-the-felix-gogo-shell>`__) with the ``lb -s | grep example``, you will see that it has stayed in the ``INSTALLED`` state. If you note the bundle ID that comes back (it's the first column in the list of results) then use ``diag #``, where you replace ``#`` with the bundle ID, it will tell you why it's not in the ``ACTIVE`` state:

.. code-block:: text

	Unresolved requirement: Import-Package: com.liferay.portal.kernel.util; version="[7.0.0,7.1.0)"

If this is the first time you've seen an error message like this, you will want to read up on `Resolving Bundle Requirements <https://dev.liferay.com/develop/tutorials/-/knowledge_base/7-0/resolving-bundle-requirements>`__ and `Detecting Unresolved OSGi Components <https://dev.liferay.com/develop/tutorials/-/knowledge_base/7-0/detecting-unresolved-osgi-components>`__ for a little bit of background before continuing.

The Naive Bundle Manifest
-------------------------

The previously linked documentation talks about how you can resolve the error, but if you're building up expertise rather than troubleshooting, I think it's also useful to understand what's causing the problem, and thus reach an understanding of why certain steps can fix that problem.

So, why does this error arise in the first place? Well, if you open up ``build/tmp/jar/MANIFEST.MF`` (which we describe in more detail in `OSGi and Modularity for Liferay Portal 6 Developers <https://dev.liferay.com/develop/tutorials/-/knowledge_base/7-0/osgi-and-modularity-for-liferay-6-developers>`__), you should see the following lines:

.. code-block:: text

	Import-Package: com.liferay.portal.kernel.util;version="[7.0,7.1)",jav
	 ax.servlet.http;version="[3.0,4)"

These lines are why the ``com.example.termsofuse-1.0.0.jar`` bundle asks for ``com.liferay.portal.kernel.util`` with the specified version range. This leaves us with two unanswered questions: (1) why does it ask for version 7.0 (inclusive) as a lower part of the range, and (2) why does it ask for version 7.1 (exclusive) as the upper part of the range?

Default Import-Package Lower Bound
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

First, that our bundle imports the ``com.liferay.portal.kernel.util`` package at all is because it is the package containing interface we implement, ``com.liferay.portal.kernel.util.TermsOfUseContentProvider``.

This class comes from the ``com.liferay.portal.kernel`` dependency specified in ``build.gradle``, and since we've specified version 2.0.0 of this dependency, we can find it in one of the subfolders of ``${user.home}/.gradle/caches/modules-2/files-2.1/com.liferay.portal/com.liferay.portal.kernel/2.0.0``.

**Note**: If this is the first time you've needed to check inside a ``.gradle`` cache, the folder layout is similar to a Maven cache except it uses the SHA1 as the folder name rather than as a separate file, and one SHA1 corresponds to a ``.pom`` file while the other corresponds to a ``.jar`` file. In some cases, there may be a third SHA1 that corresponds to the source code for the artifact.

If we check inside the ``META-INF/MANIFEST.MF`` file within the ``.jar`` file artifact, we'll find the following lines buried inside of it:

.. code-block:: text

	Export-Package: com.liferay.admin.kernel.util;version="1.0.0";uses:="c
	  ...
	 feray.portal.kernel.url;version="1.0.0";uses:="javax.servlet",com.lif
	 eray.portal.kernel.util;version="7.0.0";uses:="com.liferay.expando.ke
	  ...

And this is where we get the ``7.0`` as the lower bound on the version range: version 2.0.0 of the ``com.liferay.portal.kernel`` artifact exports version 7.0.0 of the ``com.liferay.portal.kernel.util`` package.

Default Import-Package Upper Bound
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For many package import packages like the ``javax.servlet.http`` import, you'll notice that they take the form ``[<x>.<y>, <x+1>)``, where the upper part of the range essentially asks for the next major version. However, our ``com.liferay.portal.kernel.util`` is a lot less optimistic, instead choosing to have a version range of ``[<x>.<y>, <x>.<y+1>)``. The reason lies in how our code uses the classes from the packages we import.

In the case of ``javax.servlet.http``, we're just using objects that implement the ``HttpServletRequest`` and ``HttpServletResponse`` interfaces. Whenever you simply consume an interface (or consume a class), the default accepted version range will be set to ``[<x>.<y>, <x+1>)``.

In the case of ``com.liferay.portal.kernel.util``, we're implementing the ``TermsOfUseContentProvider`` interface. If you implement an interface, then the ``Import-Package`` statement will sometimes be optimistic by default and specify ``[<x>.<y>, <x+1>)`` and sometimes be more pessimistic by default and specify ``[<x>.<y>, <x>.<y+1>)``. In our case, it's chosen the more pessimistic default.

There are technical details that the creator of the interface needs to consider when deciding whether implementors need to be optimistic or pessimistic (`The Needs of the Many Outweigh the Needs of the Few <http://blog.hargrave.io/2011/09/needs-of-many-outweigh-needs-of-few.html>`__). These details center around fairly nebulous concepts like whether the interface is intended to be implemented by an "API provider" or by an "API consumer" (`Semantic Versioning Technical Whitepaper <http://www.osgi.org/wiki/uploads/Links/SemanticVersioning.pdf>`__), where an API provider and an API consumer are very abstractly defined.

However, from the side of someone implementing an interface, we can simply look at the end result, which is a commitment on the stability of the interface:

* If it is marked as a ``ConsumerType`` (or not marked at all, since ``ConsumerType`` is assumed if no explicit annotation is provided), this interface is not allowed to change during a minor version increment to its package, so implementors **do not** need to worry about minor version changes
* If it is marked as a ``ProviderType``, this interface is allowed to change during a minor version increment to its package, so implementors **do** need to worry about minor version changes

This leads to the following default behavior when setting the upper version range on a package import involving an implemented interface:

* If the interface we implement is annotated with the ``ConsumerType`` annotation (or not annotated at all, since ``ConsumerType`` is assumed if no explicit annotation is provided), we can be optimistic, and the default accepted version range will be set to ``[<x>.<y>, <x+1>)``
* If the interface we implement is annotated with the ``ProviderType`` annotation, we should be pessimistic, and the default accepted version range will be set to ``[<x>.<y>, <x>.<y+1>)``

And this is where we get the ``7.1`` as the upper bound on the version range: `TermsOfUseContentProvider <https://github.com/liferay/liferay-portal/blob/7.0.4-ga5/portal-kernel/src/com/liferay/portal/kernel/util/TermsOfUseContentProvider.java#L25>`__ is annotated with the ``ProviderType`` annotation, which means that minor version changes to the package *might* also include an update to the package we implement, so we should be conservative when specifying the accepted version ranges.

The Improved Bundle Manifest
----------------------------

So now that we know that the default behavior for our package import is ``[<x>.<y>, <x>.<y+1>)``, we have two options for getting our bundle to deploy. Either we can (a) choose a different dependency to generate a version range compatible with our installation automatically, or (b) set a broader version range manually.

Automatically Set Import-Package
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In the case of (a), now that you know where the lower part of the range ``<x>.<y>`` comes from, you can change the dependency version of ``com.liferay.portal.kernel`` so that exports the same version of the package that is exported in your Liferay installation. For example, if you know that your version of ``com.liferay.portal.kernel`` is a snapshot release of ``2.57.1``, you can specify the following in your ``build.gradle``:

.. code-block:: groovy

	compileOnly group: "com.liferay.portal", name: "com.liferay.portal.kernel", version: "2.57.0"

However, how exactly do you find that value?

If you've built from source, all the versions are computed during the build initialization (specifically the ``ant setup-sdk`` step) and copied to ``.gradle/gradle.properties``. If you open up that file, you'll find something that looks like this, which will give you both the module name and the module version.

.. code-block:: properties

	com.liferay.portal.impl.version=x.y.z-SNAPSHOT
	com.liferay.portal.kernel.version=x.y.z-SNAPSHOT
	com.liferay.portal.test.version=x.y.z-SNAPSHOT
	com.liferay.portal.test.integration.version=x.y.z-SNAPSHOT
	com.liferay.util.bridges.version=x.y.z-SNAPSHOT
	com.liferay.util.java.version=x.y.z-SNAPSHOT
	com.liferay.util.taglib.version=x.y.z-SNAPSHOT

If you're curious where that information comes from, the bundle name is found inside ``build.xml`` as the ``manifest.bundle.symbolic.name`` build property (`example here <https://github.com/liferay/liferay-portal/blob/7.0.4-ga5/portal-kernel/build.xml#L10>`__), while the bundle version is found inside ``bnd.bnd`` as the ``Bundle-Version`` (`example here <https://github.com/liferay/liferay-portal/blob/7.0.4-ga5/portal-kernel/bnd.bnd#L3>`__).

If you're working with a release artifact, then as documented in `Configuring Dependencies <https://dev.liferay.com/develop/tutorials/-/knowledge_base/7-0/configuring-dependencies>`__, open up the ``portal-kernel.jar`` provided with your version of the Liferay distribution and check inside of ``META-INF/MANIFEST.MF`` for its version. This will provide you with what the version was at build time for ``portal-kernel.jar``. If constantly unzipping ``.jar`` files gets to be too tedious, you can also look it up using a tool I created for seeing how Liferay's module versions have evolved over time: `Module Version Changes Since DXP Release <https://s3-us-west-2.amazonaws.com/mdang.grow/dxpmodules.html?sourceVersion=7010-de-34&targetVersion=7010-de-34&nameFilter=com.liferay.portal.kernel>`__

However, the automatic approach has a limitation: Liferay does not release ``com.liferay.portal.kernel`` with every release of Liferay, but rather, each Liferay release uses a snapshot release of ``com.liferay.portal.kernel``.

This isn't a big deal if the snapshot has a minor version like ``.1``, because a ``packageinfo`` minor version increment will also trigger a bundle minor version increment, and so a ``.1`` snapshot will have the same minor versions on its exports as the original ``.0`` release.

However, when the snapshot has a minor version of ``.0``, things get murky because of the fact that it's a snapshot: there was some package change between the previous minor version and the snapshot version, but it's not guaranteed to have been the package we are using. Additionally, if it wasn't our package that was update, our package might update between the snapshot used for the Liferay release, and the actual ``.0`` for the artifact is published, because the `Baseline Plugin <https://dev.liferay.com/develop/reference/-/knowledge_base/7-0/baseline-gradle-plugin>`__ allows all packages to experience a minor version increment up until the version is published and the baseline version changes.

As a result, you have to check both the release version and one minor version below to see which one you need to use in order to get the correct version range generated automatically. If you are implementing multiple interfaces from different packages within the same artifact, it's also theoretically possible that there is no version you can use to have the correct version range generated automatically.

* DE-15 was released with a snapshot of 2.28.0. The snapshot version exports 7.22, version 2.27.0 exports 7.22, and version 2.28.0 exports 7.22.
* DE-27 was released with a snapshot of 2.42.0. The snapshot version exports 7.30, version 2.41.0 exports 7.29, and version 2.42.0 exports 7.30.
* DE-28 was released with a snapshot of 2.43.0. The snapshot version exports 7.31, version 2.42.0 exports 7.30, and version 2.43.0 exports 7.31.

Manually Set Import-Package
~~~~~~~~~~~~~~~~~~~~~~~~~~~

At this point, we've discovered that the automatic approach is hardly automatic at all, because we're still investigating the package versions of different artifacts. We also know that an automatic approach might fail. Given that we'll need to investigate all the artifacts and package versions *anyway*, how do we achieve (b)?

Since you're setting a version range, you will want to set the broadest version range that is known to compile successfully. To that end, from the OSGi perspective, you update ``bnd.bnd`` with a new ``Import-Package`` statement that is known to work, and this ``Import-Package`` will automatically be added to the generated ``META-INF/MANIFEST.MF``. We also add ``*`` to tell the ``bnd`` to also include everything else it was planning to add.

In the case of a ``ProviderType`` (which is really the only time when this kind of problem happens), its API can change for *any* minor release. Therefore, we should only include version ranges where we know the package has not yet changed, and we should not project into the future beyond that. Therefore, if we know that our interface has its current set of methods at ``<a>.<i>``, and it still has not changed as of ``<b>.<j>``, we would choose the version range ``[<a>.<i>, <b>.<j+1>)``.

In the specific case of ``TermsOfUseContentProvider``, it started with the current interface methods at version 7.0 of the package, and if you check the source code of the interface within ``DE-34`` to confirm that it is still unchanged in the version of Liferay you are using, and you unzip ``portal-kernel.jar`` to check the ``META-INF/MANIFEST.MF`` to find its corresponding export version at 7.40.0. This means that we can use the following ``Import-Package`` statement of our ``bnd.bnd``.

.. code-block:: text

	Import-Package: com.liferay.portal.kernel.util;version="[7.0,7.41)",*

If this process gets to be too tedious, you can also look it up using a tool I created for seeing how Liferay's package versions have evolved over time: `Package Breaking Changes Since DXP Release <https://s3-us-west-2.amazonaws.com/mdang.grow/dxppackages.html?sourceVersion=7010-de-34&targetVersion=7010-de-34&nameFilter=com.liferay.portal.kernel.util>`__

History of a Similar Customization
----------------------------------

If the ``7.0`` to ``7.41`` version range did not immediately clue you in, then if you were to scan through the evolution of ``com.liferay.portal.kernel.util`` across different versions of Liferay, you'll have discovered that while the ``TermsOfUseContentProvider`` interface itself has not changed at all since the initial DXP release, the package it resides in is *very* frequently updated. In fact, it has changed in 25 of the past 34 fix pack releases.

Because both the automatic process and the manual process rely on minor versions, this means that no matter which route you chose, you would have needed to modify either your ``build.gradle`` or your ``bnd.bnd`` for each one of those releases, or your custom terms of use would have failed to deploy in 25 out of the past 34 fix packs.

This leads us to the following question.

Liferay has its own ``journal-terms-of-use``, which we mentioned earlier in this entry, that implements the ``TermsOfUseContentProvider`` interface. Obviously it should run into the same issue. So, how has Liferay been keeping ``journal-terms-of-use`` up to date?

Import-Package Version Range Macro
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

At the beginning, ``journal-terms-of-use`` started with trying to solve the reverse problem: if we know that we aren't changing the API, how do we ensure that the bundle can deploy on *older* versions? The idea was built on a concept where we'd release the Web Experience package separate from the rest of Liferay, and we wanted this package to be able to deploy against older versions of Liferay.

With `LPS-64350 <https://issues.liferay.com/browse/LPS-64350>`__, Liferay decided to achieve this using `version range macros <http://bnd.bndtools.org/macros/range.html>`__ inside of the ``bnd.bnd``:

.. code-block:: text

	Import-Package: com.liferay.portal.kernel.util;version="${range;[=,=+)}",*

Essentially, this says that we know it works as of the initial major version release, and we know it will work up until the next minor version. From there, we'd update ``build.gradle`` with every release whenever we confirmed that we had not changed the interface with ``com.liferay.portal.kernel``, and the version range macro would allow it to be compatible with all previous releases without us having to explicitly lookup the package version for the current release.

Gradle Dependency Version Range
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

However, after awhile, this got to be extremely tedious, because we were updating ``build.gradle`` with every release of ``com.liferay.portal.kernel``.

From there, we came up with a seemingly clever idea. Essentially, Liferay was rebuilt at release time anyway, we could tell Gradle to fetch the latest release of ``com.liferay.portal.kernel``. As a result, we'd simply re-compile Liferay, and this latest release combined with the version range macro would give us the desired version range automatically. This is functionally equivalent to replace the ``com.liferay.portal.kernel`` dependency with the following:

.. code-block:: groovy

	compileOnly group: "com.liferay.portal", name: "com.liferay.portal.kernel", version: "[2.0.0,3.0.0)"

We later learned that this approach had two critical problems.

First, Gradle is not guaranteed to try to use the latest version of a dependency whenever you specify a range. Therefore, you might run into a situation where your portal would fail to deploy ``journal-terms-of-use`` simply because Gradle happened to choose something earlier than the latest dependency version.

Second, we might implement multiple interfaces that come from multiple packages published by the ``com.liferay.portal.kernel`` artifact. Because we only had a version range macro set for the ``com.liferay.portal.kernel.util`` package, the ``journal-terms-of-use`` module would suddenly fail to deploy if we were to rebuild it and deploy the bundle to an older Liferay release (such as when building for a hotfix) due to the other ``ProviderType`` interfaces it might have implemented, or if Liferay converted a ``ConsumerType`` interface into a ``ProviderType`` interface without incrementing the major version on the package (it's not required, similar to changing the byte-code compilation level, and so Liferay never does so).

Dependency Version as a Property
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

As a temporary stop-gap measure for building against older versions of Liferay, we needed a way to retain the old manifests. As noted before, there's a problem: the version of ``com.liferay.portal.kernel`` that accompanies a past release is an unpublished snapshot.

In theory, we could simply publish a snapshot to our local Gradle cache at build time and reference it, but internally at Liferay, our source formatter rules disallow using an un-dated snapshot as part of a dependency version. Luckily, there's a work around for that: because it's simply checking for the string, as long as something else provides the snapshot version (like a variable or a build property), we are allowed to use it.

So, our work around at the time was to take advantage of something that was automatically set inside of ``gradle.properties`` whenever you build Liferay from source. This can also be set manually for your own Blade workspace. Ultimately, the net effect of using a build property is that you update a single file and it can be referenced by all other custom modules within the same workspace, which is the same idea as using a Groovy variable or a Maven build property.

.. code-block:: properties

	com.liferay.portal.kernel.version=2.57.1-SNAPSHOT

Once this property is set, there is an additional shorthand for using it. The Liferay Gradle plugin uses a `LiferayExtension <https://github.com/liferay/liferay-portal/blob/7.0.4-ga5/modules/sdk/gradle-plugins/src/main/java/com/liferay/gradle/plugins/extensions/LiferayExtension.java#L131-L152>`__ that allows us to use the name ``default`` in order to reference this property value, or substitutes the Apache Ivy alias ``latest.release`` (which Gradle happens to recognize) if the property has not been set.

As a result, our ``com.liferay.portal.kernel`` dependency looks like this:

.. code-block:: groovy

	compileOnly group: "com.liferay.portal", name: "com.liferay.portal.kernel", version: "default"

If we then deploy the resulting ``.jar``, all of our modules will compile against the specified version of ``com.liferay.portal.kernel``. Compiled with the original values inside of ``bnd.bnd``, it will fail at compile time for all modules with this pattern. We can then just update this property any time we're updating to a later fix pack or rebuilding Liferay from source to confirm that the ``ProviderType`` interfaces have not changed.

Treat it Like a ConsumerType
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In order to fix the bug introduced with the Gradle versions while also retaining the intended result of being able to deploy a module like ``journal-terms-of-use`` on multiple versions of Liferay, the answer we arrived at in `LPS-70519 <https://issues.liferay.com/browse/LPS-70519>`__ was to simply treat ``TermsOfUseContentProvider`` like a ``ConsumerType`` when specifying version ranges, even though it's been marked as a ``ProviderType``.

In other words, we manually set the lower bound to be the version of ``com.liferay.portal.kernel.util`` that is exported by the minimum ``com.liferay.portal.kernel`` that provides other API that ``journal-terms-of-use`` needs, and we set the upper bound to be the next major version after that, just as would happen automatically with implementing a ``ConsumerType`` interface or any other regular class usage.

.. code-block:: text

	Import-Package: com.liferay.portal.kernel.util;version="[7.15.0,8)",*

There are two downsides to this, both of which Liferay excepts.

The first is that the module advertises something that is technically untrue. Because it is a ``ProviderType``, Liferay can modify ``TermsOfUseContentProvider`` before the next major release, and even though the module declares that it will work with every version of the package up through 8, this won't be true if the interface gets updated.

The second is that this approach results in ``journal-terms-of-use`` being unable to detect when we make binary-incompatible changes to ``TermsOfUseContentProvider``. However, in practice, Liferay can get away treating this particular ``ProviderType`` as though it were a ``ConsumerType`` for ``journal-terms-of-use``, because Liferay itself maintains both the interface and the implementation, and therefore a code reviewer would know if we changed the interface and know to update our implementation of that interface.

A Minimal Customization, Part 2
-------------------------------

With all of that background information, we can now come back to our module and make it work.

Choose a Long-Term Solution
~~~~~~~~~~~~~~~~~~~~~~~~~~~

At this point, we have two exactly opposite solutions that we can use over the long term: (a) add the dependency version as a build property, or (b) treat the ``ProviderType`` interface as though it were a ``ConsumerType`` interface. With the former solution, you accept the idea that you will need to check each time you update, but you do it once per workspace rather than once per module. With the latter solution, you reject that as being too tedious and accept the risk that Liferay might one day change the ``ProviderType`` and your module will stop working.

If we'd like to accept the downside of constantly updating our ``com.liferay.portal.kernel`` version in order to ensure that the ``TermsOfUseContentProvider`` interface has not changed, we can set our dependency version as ``default`` and maintain ``gradle.properties`` with an up to date value of ``com.liferay.portal.kernel.version`` for each Liferay release you update to. This allows us to handle all ``ProviderType`` interfaces in one place at compile time and leads to the following ``bnd.bnd`` entry for ``Import-Package``:

.. code-block:: text

	Import-Package: com.liferay.portal.kernel.util;version="${range;[=,=+)}",*

Because the version of ``com.liferay.portal.kernel`` has changed at compile time, it's likely that the manifest is also changing. Therefore, if you go with this solution, you will want to increment the ``Bundle-Version`` each time you update the properties value just as you might do with Maven artifacts that depend on changing properties values, because the binary artifact produced by the compilation will be changing alongside the properties value change.

If we'd prefer not to accept the downside of constantly updating our ``com.liferay.portal.kernel`` version, you can choose to treat ``TermsOfUseContentProvider`` as a ``ConsumerType``. In this case, you'd leave ``com.liferay.portal.kernel`` at whichever minimum version you need for API compatibility, and add the following ``bnd.bnd`` entry for ``Import-Package``:

.. code-block:: text

	Import-Package: com.liferay.portal.kernel.util;version="${range;[==,+)}",*

As noted earlier, you essentially give up the ability to check for binary compatibility at build time, and you will need to periodically check in on the ``ProviderType`` interfaces to make sure that they have not changed, because those changes will not be detected at build time and they will not be noticed at deployment time. You will likely only notice if you coincidentally wrote a functional test that happens to hit a page that invokes the new methods on the interface.

Successfully Deploy the Stub Implementation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Whichever route you choose, we have completed our updates to the stub implementation.

Just as before, if you invoke ``blade gw jar``, it will create the file ``build/libs/com.example.termsofuse-1.0.0.jar``. If you're using a Blade workspace, you can set ``liferay.workspace.home.dir`` in ``gradle.properties`` and use ``blade gw deploy`` to have it be copied to ``${liferay.home}/osgi/modules``, or you can manually copy this file to ``${liferay.home}/deploy``.

When you do so, you will see a message saying that the bundle is being processed, and then you will see a message saying that the bundle has started.

If you then navigate to ``/c/portal/terms_of_use``, then assuming that you also disabled the ``com.liferay.journal.terms.of.use`` module as documented earlier, it will show you a completely empty terms of use rather than the default terms of use, and all you can do is agree or disagree to the empty page.

Just Beyond the Stub Implementation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

While this article is focused on understanding road blocks rather than providing sample code, it would be a little disingenous to stop here and say that we have a functioning implementation of a terms of use override, so we'll take a few steps forward and bring in some additional information.

Just like any other component that needs to include JSPs, we'll need a reference to the appropriate ``ServletContext``. You can find an example of this in `Customizing the Control Menu <https://dev.liferay.com/develop/tutorials/-/knowledge_base/7-0/customizing-the-control-menu>`__ and `Customizing the Product Menu <https://dev.liferay.com/develop/tutorials/-/knowledge_base/7-0/customizing-the-product-menu>`__.

For our specific example, we might add the following to our ``bnd.bnd`` so that we can have a ``ServletContext``:

.. code-block:: text

	Web-ContextPath: /example-terms-of-use

We would then then add the following imports and replace the content of the ``includeView`` method in ``ExampleTermsOfUseContentProvider.java``, assuming that the bundle symbolic name from the steps so far is ``com.example.termsofuse`` (which it should be, by default):

.. code-block:: java

	import javax.servlet.RequestDispatcher;
	import javax.servlet.ServletContext;
	import org.osgi.service.component.annotations.Reference;

	// ...

	@Override
	public void includeView(
			HttpServletRequest request, HttpServletResponse response)
		throws Exception {

		System.out.println("Called includeView(HttpServletRequest, HttpServletResponse)");

		_servletContext.getRequestDispatcher("/terms_of_use.jsp").include(request, response);
	}

	@Reference(target = "(osgi.web.symbolicname=com.example.termsofuse)")
	private ServletContext _servletContext;

If we then create a file named ``src/main/resources/META-INF/resources/terms_of_use.jsp`` with the content, ``<h1>TODO</h1>`` and redeploy our module, we'll now see the words "TODO" just above the agree and disagree buttons if you navigate to ``/c/portal/terms_of_use``.