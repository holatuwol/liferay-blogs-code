package multi.thread.dump.filter;

import com.liferay.portal.kernel.util.FastDateFormatFactory;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;

import java.io.IOException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	immediate = true,
	property = {
		"before-filter=Thread Dump Filter", "dispatcher=REQUEST",
		"servlet-context-name=",
		"servlet-filter-name=Multi Thread Dump Filter", "url-pattern=/",
		"url-pattern=/*"
	},
	service = Filter.class
)
public class MultiThreadDumpFilter implements Filter {
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		_active = true;

		_format = _fastDateFormatFactory.getSimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss:SSS z");

		new Thread(() -> {
			Object waitObject = new Object();

			while (_active) {
				try {
					_generateThreadDump();

					synchronized (waitObject) {
						waitObject.wait(100);
					}
				}
				catch (InterruptedException ioe) {
					_active = false;
				}
			}
		}, getClass().getName()).start();
	}

	@Override
	public void doFilter(
			ServletRequest servletRequest, ServletResponse servletResponse,
			FilterChain filterChain)
		throws IOException, ServletException {

		Thread currentThread = Thread.currentThread();

		boolean takeThreadDumps = GetterUtil.getBoolean(
			servletRequest.getParameter("takeThreadDumps"), false);

		if (takeThreadDumps) {
			synchronized (_threads) {
				_threads.add(currentThread);
			}
		}

		filterChain.doFilter(servletRequest, servletResponse);

		if (takeThreadDumps) {
			synchronized (_threads) {
				_threads.remove(currentThread);
			}
		}
	}

	@Override
	public void destroy() {
		_active = false;
	}

	private void _generateThreadDump() {
		List<Thread> threads = null;

		synchronized (_threads) {
			if (_threads.isEmpty()) {
				return;
			}

			threads = new ArrayList<>(_threads);
		}

		String dateString = _format.format(new Date());

		Map<Thread, StackTraceElement[]> stackTraces =
			Thread.getAllStackTraces();

		StringBundler sb = new StringBundler();

		for (Thread thread : _threads) {
			StackTraceElement[] elements = stackTraces.get(thread);

			sb.append("\n\n\"");
			sb.append(thread.getName());
			sb.append(StringPool.QUOTE);
			sb.append(" @ ");
			sb.append(dateString);

			for (StackTraceElement element : elements) {
				sb.append("\n");
				sb.append(element);
			}
		}

		sb.append("\n\n");

		System.out.println(sb.toString());
	}


	private boolean _active = false;

	@Reference
	private FastDateFormatFactory _fastDateFormatFactory;

	private Format _format;

	private List<Thread> _threads = new ArrayList<>();

}