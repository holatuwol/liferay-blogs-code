package omni.admin.store;

import com.liferay.document.library.kernel.store.DLStore;
import com.liferay.document.library.kernel.store.DLStoreUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.io.ByteArrayInputStream;

import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.ProxyUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = OmniAdminDLStoreWrapper.class)
public class OmniAdminDLStoreWrapper {

	@Activate
	public void activate() {
		_dlStore = DLStoreUtil.getStore();

		DLStore dlStoreWrapper = (DLStore) ProxyUtil.newProxyInstance(
			PortalClassLoaderUtil.getClassLoader(),
			new Class<?>[] { DLStore.class },
			(proxy, method, args) -> {
				String methodName = method.getName();

				if (methodName.equals("getFileAsBytes") || methodName.equals("getFileAsStream")) {
					if (args.length == 3) {
						if (!_dlStore.hasFile((Long)args[0], (Long)args[1], (String)args[2])) {
							return methodName.equals("getFileAsBytes") ? new byte[0] : new ByteArrayInputStream(new byte[0]);
						}
					}
					else if (args.length == 4) {
						if (!_dlStore.hasFile((Long)args[0], (Long)args[1], (String)args[2], (String)args[3])) {
							return methodName.equals("getFileAsBytes") ? new byte[0] : new ByteArrayInputStream(new byte[0]);
						}
					}
				}

				return method.invoke(_dlStore, args);
			});

		_log.fatal("Overriding " + _dlStore);

		new DLStoreUtil().setStore(dlStoreWrapper);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		OmniAdminDLStoreWrapper.class);

	private DLStore _dlStore;

}