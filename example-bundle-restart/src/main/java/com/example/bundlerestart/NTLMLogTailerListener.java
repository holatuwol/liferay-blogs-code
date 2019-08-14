package com.example.bundlerestart;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Component(immediate = true)
public class NTLMLogTailerListener
	extends TailerListenerAdapter
	implements BundleListener {

	public static final String BUNDLE_TO_RESTART =
		"com.liferay.portal.security.sso.ntlm";
	public static final String LOG_FILE_TO_POLL = "/dev/shm/test.txt";
	public static final long POLLING_DELAY = 1000;

	@Activate
	public void activate(
		BundleContext bundleContext, Map<String, Object> properties) {

		bundleContext.addBundleListener(this);

		for (Bundle bundle : bundleContext.getBundles()) {
			String bundleSymbolicName = bundle.getSymbolicName();

			if (BUNDLE_TO_RESTART.equals(bundleSymbolicName)) {
				tail(bundle);

				return;
			}
		}
	}

	@Override
	public void bundleChanged(BundleEvent bundleEvent) {
		if (bundleEvent.getType() != BundleEvent.INSTALLED) {
			return;
		}

		Bundle bundle = bundleEvent.getBundle();

		String bundleSymbolicName = bundle.getSymbolicName();

		if (BUNDLE_TO_RESTART.equals(bundleSymbolicName)) {
			tail(bundle);
		}
	}

	@Deactivate
	public void deactivate(BundleContext bundleContext) {
		bundleContext.removeBundleListener(this);

		_file = null;

		_lastBundleRestartFileLastModified = 0;

		_ntlmBundle = null;

		if (_tailer != null) {
			_tailer.stop();

			_tailer = null;
		}
	}

	public synchronized void tail(Bundle ntlmBundle) {
		_ntlmBundle = ntlmBundle;

		if (_tailer != null) {
			return;
		}

		try {
			_file = new File(LOG_FILE_TO_POLL);

			if (!_file.exists()) {
				_file.createNewFile();
			}

			_tailer = new Tailer(_file, this, POLLING_DELAY);
			_lastBundleRestartFileLastModified = _file.lastModified();

			Thread thread = new Thread(_tailer);
			thread.setDaemon(true);
			thread.start();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	@Override
	public void handle(String line) {
		long fileLastModified = _file.lastModified();

		if (fileLastModified == _lastBundleRestartFileLastModified) {
			return;
		}

		_lastBundleRestartFileLastModified = fileLastModified;

		try {
			_ntlmBundle.update();
		}
		catch (BundleException be) {
			be.printStackTrace();
		}
	}

	private File _file;
	private long _lastBundleRestartFileLastModified;
	private Bundle _ntlmBundle;
	private Tailer _tailer;

}