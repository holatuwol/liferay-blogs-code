package com.liferay.pandoc.wiki;

import com.liferay.expando.kernel.model.ExpandoBridge;
import com.liferay.portal.kernel.util.AggregateResourceBundleLoader;
import com.liferay.portal.kernel.util.ResourceBundleLoader;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.xml.Attribute;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReader;
import com.liferay.portal.kernel.xml.UnsecureSAXReaderUtil;
import com.liferay.portal.language.LanguageResources;
import com.liferay.wiki.engine.BaseWikiEngine;
import com.liferay.wiki.exception.PageContentException;
import com.liferay.wiki.model.WikiPage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.portlet.PortletURL;
import javax.servlet.ServletContext;

/**
 * @author Minhchau Dang
 */
public abstract class BasePandocWikiEngine extends BaseWikiEngine {

	public static final String FIELD_NAME = "pandocHTML";

	public static String getHTML(
			long nodeId, String title, String content, String inputFormat)
		throws DocumentException, InterruptedException, IOException {

		String attachmentURLPrefix = "/c/wiki/get_page_attachment?nodeId=" +
			nodeId + "&title=" + URLEncoder.encode(title, "UTF-8") +
				"&fileName=";

		ProcessBuilder processBuilder = new ProcessBuilder(
			"pandoc", "--wrap=none", "--from", inputFormat, "--to", "html");

		Process process = processBuilder.start();

		try (OutputStream os = process.getOutputStream()) {
			os.write(content.getBytes(StandardCharsets.UTF_8));
		}

		process.waitFor();

		Document document = null;

		SAXReader saxReader = UnsecureSAXReaderUtil.getSAXReader();

		try (InputStream is = process.getInputStream()) {
			String html = StringUtil.read(is);

			html = "<div>" + html + "</div>";

			document = saxReader.read(html);
		}

		for (Node node : document.selectNodes("//a[@href]")) {
			Element element = (Element)node;

			Attribute hrefAttribute = element.attribute("href");
			String href = hrefAttribute.getValue();

			if ((href.indexOf("://") == -1) && (href.indexOf('/') != 0) &&
				(href.indexOf('#') != 0)) {

				hrefAttribute.setValue(attachmentURLPrefix + href);
			}
		}

		for (Node node : document.selectNodes("//img[@src]")) {
			Element element = (Element)node;

			Attribute hrefAttribute = element.attribute("src");
			String href = hrefAttribute.getValue();

			if ((href.indexOf("://") == -1) && (href.indexOf('/') != 0) &&
				(href.indexOf('#') != 0)) {

				hrefAttribute.setValue(attachmentURLPrefix + href);
			}
		}

		Element rootElement = document.getRootElement();

		return rootElement.asXML();
	}

	@Override
	public String convert(
			WikiPage page, PortletURL viewPageURL, PortletURL editPageURL,
			String attachmentURLPrefix)
		throws PageContentException {

		try {
			String html = getHTML(page);
			String css = getStandaloneCSS();

			return "<style>" + css + "</style>" + html;
		}
		catch (Exception ioe) {
			return super.convert(
				page, viewPageURL, editPageURL, attachmentURLPrefix);
		}
	}

	public String getEditorName() {
		return null;
	}

	public String getHelpURL() {
		return null;
	}

	protected String getStandaloneCSS() throws IOException {
		if (_standaloneCSS != null) {
			return _standaloneCSS;
		}

		ClassLoader classLoader = BasePandocWikiEngine.class.getClassLoader();

		try (InputStream is = classLoader.getResourceAsStream(_CSS_PATH)) {
			_standaloneCSS = StringUtil.read(is);
		}

		return _standaloneCSS;
	}

	protected String getHTML(WikiPage page)
		throws DocumentException, InterruptedException, IOException {

		ExpandoBridge expandoBridge = page.getExpandoBridge();

		String html = (String)expandoBridge.getAttribute(FIELD_NAME, false);

		if ((html != null) && !html.isEmpty()) {
			return html;
		}

		html = getHTML(
			page.getNodeId(), page.getTitle(), page.getContent(), getFormat());

		expandoBridge.setAttribute(FIELD_NAME, html, false);

		return html;
	}

	@Override
	protected ServletContext getEditPageServletContext() {
		return _editPageServletContext;
	}

	protected ServletContext getHelpPageServletContext() {
		return null;
	}

	@Override
	protected ResourceBundleLoader getResourceBundleLoader() {
		return _resourceBundleLoader;
	}

	protected void setEditPageServletContext(ServletContext servletContext) {
		_editPageServletContext = servletContext;
	}

	protected void setResourceBundleLoader(
		ResourceBundleLoader resourceBundleLoader) {

		_resourceBundleLoader = new AggregateResourceBundleLoader(
			resourceBundleLoader, LanguageResources.RESOURCE_BUNDLE_LOADER);
	}

	private static final String _CSS_PATH = "/standalone.css";

	private String _standaloneCSS;
	private ServletContext _editPageServletContext;
	private ResourceBundleLoader _resourceBundleLoader;

}
