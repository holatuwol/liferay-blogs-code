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
	implements BundleListener, Runnable {

	public static final String BUNDLE_TO_RESTART =
		"com.liferay.portal.security.sso.ntlm";
	public static final File LOG_FILE_TO_POLL =
		new File(System.getProperty("liferay.home"), "logs/ntlm.log");
	public static final long POLLING_DELAY = 10;

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

		_ntlmBundle = null;
	}

	@Override
	public void run() {
		while (_ntlmBundle != null) {
			handle("");

			try {
				synchronized (this) {
					this.wait(POLLING_DELAY * 1000);
				}
			}
			catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}

		_lastBundleRestartFileLastModified = 0;

		_tailer = null;
	}

	public synchronized void tail(Bundle ntlmBundle) {
		_ntlmBundle = ntlmBundle;

		if (_tailer != null) {
			return;
		}

		try {
			if (!LOG_FILE_TO_POLL.exists()) {
				LOG_FILE_TO_POLL.createNewFile();
			}

			_tailer = new Thread(this);
			_tailer.setDaemon(true);
			_tailer.start();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	@Override
	public void handle(String line) {
		long fileLastModified = LOG_FILE_TO_POLL.lastModified();

		if (fileLastModified == _lastBundleRestartFileLastModified) {
			return;
		}

		_lastBundleRestartFileLastModified = fileLastModified;

		try {
			_ntlmBundle.stop();
			_ntlmBundle.start();
		}
		catch (BundleException be) {
			be.printStackTrace();
		}
	}

	private File _file;
	private long _lastBundleRestartFileLastModified;
	private Thread _tailer;
	private Bundle _ntlmBundle;

}