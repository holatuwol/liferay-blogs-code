package omni.admin.store;

import com.liferay.document.library.kernel.model.DLProcessorConstants;
import com.liferay.document.library.kernel.util.DLProcessor;
import com.liferay.document.library.kernel.util.DLProcessorRegistryUtil;
import com.liferay.document.library.kernel.util.PDFProcessor;
import com.liferay.document.library.kernel.util.PDFProcessorUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.module.framework.ModuleServiceLifecycle;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.repository.liferayrepository.model.LiferayFileVersion;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.ByteArrayInputStream;
import java.io.InputStream;


@Component(immediate = true, service = OmniAdminPDFProcessorWrapper.class)
public class OmniAdminPDFProcessorWrapper {

	@Activate
	public void activate() throws Exception {
		_pdfProcessor = PDFProcessorUtil.getPDFProcessor();

		DLProcessor pdfProcessorWrapper = (DLProcessor) ProxyUtil.newProxyInstance(
			PortalClassLoaderUtil.getClassLoader(),
			new Class<?>[] { DLProcessor.class, PDFProcessor.class },
			(proxy, method, args) -> {
				String methodName = method.getName();

				if (methodName.equals("hasImages")) {
					if (args[0] instanceof LiferayFileVersion) {
						LiferayFileVersion liferayFileVersion = (LiferayFileVersion)args[0];

						try (InputStream inputStream = liferayFileVersion.getContentStream(false)) {
							if (inputStream == null) {
								return false;
							}
							else if (inputStream.available() == 0) {
								return false;
							}
						}
					}
				}

				return method.invoke(_pdfProcessor, args);
			});

		_log.fatal("Overriding " + _pdfProcessor);

		DLProcessorRegistryUtil.register(pdfProcessorWrapper);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		OmniAdminPDFProcessorWrapper.class);

	private PDFProcessor _pdfProcessor;

	@Reference(target = "(module.service.lifecycle=system.check)")
	private ModuleServiceLifecycle _moduleServiceLifecycle;

}