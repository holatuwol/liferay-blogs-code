package com.example.roleconjunction.service.configuration;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

/**
 * @author Minhchau Dang
 */
@ExtendedObjectClassDefinition(
	category = "security-tools",
	scope = ExtendedObjectClassDefinition.Scope.COMPANY
)
@Meta.OCD(
	id = "com.example.roleconjunction.service.configuration.RoleConjunctionConfiguration",
	localization = "content/Language", name = "role-conjunction-configuration"
)
public interface RoleConjunctionConfiguration {

	@Meta.AD(
		deflt = "Location|Department|Position",
		name = "conjunction-role-subtypes", required = false
	)
	public String[] getConjunctionRoleSubtypes();

	@Meta.AD(
		deflt = "com.liferay.journal.model.JournalArticle",
		name = "conjunction-model-names", required = false
	)
	public String[] getConjunctionModelNames();

}