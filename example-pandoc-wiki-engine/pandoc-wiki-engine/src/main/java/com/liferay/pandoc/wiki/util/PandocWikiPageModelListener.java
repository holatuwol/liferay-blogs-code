package com.liferay.pandoc.wiki.util;

import com.liferay.expando.kernel.model.ExpandoBridge;
import com.liferay.pandoc.wiki.BasePandocWikiEngine;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.ModelListener;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.wiki.model.WikiPage;

import org.osgi.service.component.annotations.Component;

/**
 * @author Minhchau Dang
 */
@Component(service = ModelListener.class)
public class PandocWikiPageModelListener extends BaseModelListener<WikiPage> {

	public void onAfterUpdate(WikiPage wikiPage) {
		ExpandoBridge expandoBridge = wikiPage.getExpandoBridge();

		expandoBridge.setAttribute(
			BasePandocWikiEngine.FIELD_NAME, StringPool.BLANK);
	}

}
