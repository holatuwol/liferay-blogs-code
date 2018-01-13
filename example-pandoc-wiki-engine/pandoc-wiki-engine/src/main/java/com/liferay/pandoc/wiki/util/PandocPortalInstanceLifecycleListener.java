package com.liferay.pandoc.wiki.util;

import com.liferay.expando.kernel.model.ExpandoColumn;
import com.liferay.expando.kernel.model.ExpandoColumnConstants;
import com.liferay.expando.kernel.model.ExpandoTable;
import com.liferay.expando.kernel.service.ExpandoColumnLocalService;
import com.liferay.expando.kernel.service.ExpandoTableLocalService;
import com.liferay.pandoc.wiki.BasePandocWikiEngine;
import com.liferay.portal.instance.lifecycle.BasePortalInstanceLifecycleListener;
import com.liferay.portal.instance.lifecycle.PortalInstanceLifecycleListener;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.wiki.model.WikiPage;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Minhchau Dang
 */
@Component(service = PortalInstanceLifecycleListener.class)
public class PandocPortalInstanceLifecycleListener
	extends BasePortalInstanceLifecycleListener {

	@Override
	public void portalInstanceRegistered(Company company) throws Exception {
		long companyId = company.getCompanyId();

		ExpandoTable table =
			_expandoTableLocalService.addDefaultTable(
				companyId, WikiPage.class.getName());

		long tableId = table.getTableId();

		ExpandoColumn column = _expandoColumnLocalService.getColumn(
			tableId, BasePandocWikiEngine.FIELD_NAME);

		if (column == null) {
			column = _expandoColumnLocalService.addColumn(
				tableId, BasePandocWikiEngine.FIELD_NAME,
				ExpandoColumnConstants.STRING);
		}

		UnicodeProperties properties = column.getTypeSettingsProperties();

		boolean propertyHidden = GetterUtil.getBoolean(
			properties.getProperty(ExpandoColumnConstants.PROPERTY_HIDDEN),
			false);

		if (!propertyHidden) {
			properties.setProperty(
				ExpandoColumnConstants.PROPERTY_HIDDEN, StringPool.TRUE);

			column.setTypeSettingsProperties(properties);

			_expandoColumnLocalService.updateExpandoColumn(column);
		}
	}

	@Reference
	private ExpandoColumnLocalService _expandoColumnLocalService;

	@Reference
	private ExpandoTableLocalService _expandoTableLocalService;

}
