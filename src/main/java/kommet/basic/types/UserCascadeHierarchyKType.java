/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.types;

import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.BooleanDataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class UserCascadeHierarchyKType extends Type
{
	private static final long serialVersionUID = -9180797168109452928L;
	private static final String LABEL = "User Cascade Hierarchy";
	private static final String PLURAL_LABEL = "User Cascade Hierarchies";
	
	public UserCascadeHierarchyKType() throws KommetException
	{
		super();
	}
	
	public UserCascadeHierarchyKType(ProfileKType profileType, UserKType userType, UserGroupKType userGroupType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.USER_CASCADE_HIERARCHY_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.USER_CASCADE_HIERARCHY_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.USER_CASCADE_HIERARCHY_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		Field envField = new Field();
		envField.setApiName("env");
		envField.setLabel("Environment");
		envField.setDataType(new BooleanDataType());
		envField.setDbColumn("env");
		envField.setRequired(false);
		this.addField(envField);
		
		Field profileField = new Field();
		profileField.setApiName("profile");
		profileField.setLabel("Profile");
		profileField.setDataType(new TypeReference(profileType));
		profileField.setDbColumn("profile");
		profileField.setRequired(false);
		this.addField(profileField);
		
		Field localeField = new Field();
		localeField.setApiName("localeName");
		localeField.setLabel("Locale Name");
		localeField.setDataType(new TextDataType(5));
		localeField.setDbColumn("localename");
		localeField.setRequired(false);
		this.addField(localeField);
		
		Field userField = new Field();
		userField.setApiName("contextUser");
		userField.setLabel("User");
		userField.setDataType(new TypeReference(userType));
		userField.setDbColumn("contextuser");
		userField.setRequired(false);
		this.addField(userField);
		
		Field userGroupField = new Field();
		userGroupField.setApiName("userGroup");
		userGroupField.setLabel("User Group");
		userGroupField.setDataType(new TypeReference(userGroupType));
		userGroupField.setDbColumn("usergroup");
		userGroupField.setRequired(false);
		this.addField(userGroupField);
		
		Field activeCtxField = new Field();
		activeCtxField.setApiName("activeContextName");
		activeCtxField.setLabel("Active Context Name");
		activeCtxField.setDataType(new EnumerationDataType("environment\napplication\nprofile\nlocale\nuser group\nuser"));
		activeCtxField.setDbColumn("activecontextname");
		activeCtxField.setRequired(true);
		this.addField(activeCtxField);
		
		Field activeCtxRankField = new Field();
		activeCtxRankField.setApiName("activeContextRank");
		activeCtxRankField.setLabel("Active Context Rank");
		activeCtxRankField.setDataType(new NumberDataType(0, Integer.class));
		activeCtxRankField.setDbColumn("activecontextrank");
		activeCtxRankField.setRequired(true);
		this.addField(activeCtxRankField);
	}
}
