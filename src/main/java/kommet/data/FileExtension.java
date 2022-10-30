/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

import java.util.Set;

import kommet.utils.MiscUtils;

public class FileExtension
{
	public static final String CLASS_EXT = "koll";
	public static final String VIEW_EXT = "ktl";
	public static final String TYPE_EXT = "type";
	public static final String FIELD_EXT = "fld";
	public static final String LAYOUT_EXT = "layout";
	public static final String VALIDATION_RULE_EXT = "vr";
	public static final String UNIQUE_CHECK_EXT = "uc";
	public static final String APP_EXT = "app";
	public static final String SCHEDULED_TASK_EXT = "stk";
	public static final String PROFILE_EXT = "profile";
	public static final String USER_GROUP_EXT = "group";
	public static final String WEB_RESOURCE_EXT = "webres";
	public static final String VIEW_RESOURCE_EXT = "viewres";
	public static final String ACTION_EXT = "action";
	public static final String RECORD_COLLECTION_EXT = "records";
	
	public static Set<String> allExtensions()
	{
		return MiscUtils.toSet(CLASS_EXT, VIEW_EXT, TYPE_EXT, FIELD_EXT, LAYOUT_EXT, VALIDATION_RULE_EXT, UNIQUE_CHECK_EXT, APP_EXT, SCHEDULED_TASK_EXT, PROFILE_EXT, USER_GROUP_EXT, WEB_RESOURCE_EXT, VIEW_RESOURCE_EXT, ACTION_EXT, RECORD_COLLECTION_EXT);
	}
	
	public static String fromComponentType(ComponentType componentType) throws KommetException
	{
		switch (componentType)
		{
			case CLASS: return CLASS_EXT;
			case VIEW: return VIEW_EXT;
			case TYPE: return TYPE_EXT;
			case FIELD: return FIELD_EXT;
			case LAYOUT: return LAYOUT_EXT;
			case VALIDATION_RULE: return VALIDATION_RULE_EXT;
			case UNIQUE_CHECK: return UNIQUE_CHECK_EXT;
			case APP: return APP_EXT;
			case SCHEDULED_TASK: return SCHEDULED_TASK_EXT;
			case PROFILE: return PROFILE_EXT;
			case USER_GROUP: return USER_GROUP_EXT;
			case VIEW_RESOURCE: return VIEW_RESOURCE_EXT;
			case WEB_RESOURCE: return WEB_RESOURCE_EXT;
			case ACTION: return ACTION_EXT;
			case RECORD_COLLECTION: return RECORD_COLLECTION_EXT;
			default: throw new KommetException("Cannot find extenstion for component type " + componentType);
		}
	}
}