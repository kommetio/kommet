/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.config;

public class UserSettingKeys
{
	// tells whether the user can log in to the env
	public static final String KM_SYS_CAN_LOGIN = "km.sys.can.login";
	
	public static final String KM_SYS_ENV_DEFAULT_TITLE = "km.sys.env.default.title";
	
	public static final String KM_SYS_SYS_CTX_VAR = "km.sys.sysctx.var";
	
	// tells whether collections should be displayed within the objectDetails tag or not
	public static final String KM_SYS_DISPLAY_COLLECTIONS_IN_RECORD_DETAILS = "km.sys.recorddetails.collections.display";
	
	public static final String KM_SYS_COLLECTION_DISPLAY_MODE = "km.sys.collection.displaymode";
	
	public static final String KM_SYS_FIELD_LAYOUT = "km.sys.fieldlayout";
	
	public static final String KM_BUTTONS_SECTION_CLASS = "km.btns.section.class";
	
	public static final String KM_SYS_LOGIN_URL = "km.sys.login.url";

	public static final String KM_ROOT_SYS_CAN_LOGIN = "km.root.sys.can.login";
	
	public static final String KM_ROOT_MAX_TYPES = "km.root.sys.max.types";
	
	public static final String KM_SYS_NEW_TASK_EMAIL_NOTIFICATION = "km.sys.newtask.emailnotification";
	
	/**
	 * Key of the system setting that defines the default home page of the app.
	 */
	public static final String KM_SYS_HOME_PAGE = "km.home.url";
	
	public static final String KM_SYS_404_URL = "km.sys.404.url";
	
	public static final String KM_SYS_MAX_FILE_SIZE = "km.sys.max.file.size";
	
	public static final String KM_SYS_DEFAULT_LAYOUT_ID = "km.sys.defaultlayout.id";
	
	public static final String KM_ROOT_MAX_TEXTFIELD_LENGTH = "km.root.max.textfield.length";
	
	public static final String KM_SYS_HOST = "km.sys.host";
	
	/**
	 * This setting overrides the default view for the given type. The setting key is just a prefix that should be suffixed with the type KID,
	 * and the value should be the view KID.
	 * 
	 * E.g. "km.sys.default.type.view.list.<type-rid>" = "<view-rid>"
	 */
	public static final String KM_SYS_DEFAULT_TYPE_LIST_VIEW = "km.sys.default.type.view.list";
	public static final String KM_SYS_DEFAULT_TYPE_EDIT_VIEW = "km.sys.default.type.view.edit";
	public static final String KM_SYS_DEFAULT_TYPE_DETAILS_VIEW = "km.sys.default.type.view.details";
	public static final String KM_SYS_DEFAULT_TYPE_CREATE_VIEW = "km.sys.default.type.view.create";
	
	// determines what should be the behaviour of a validation rule evaluator is some fields used in the
	// validation rule have not been initialized for the save action
	// possible values those of enum {@link ValidationRuleUninitializedFieldsMode}
	public static final String KM_ROOT_SYS_VALIDATION_RULE_UNINITIALIZED_FIELDS_MODE = "km.root.sys.vr.uninit.fields.mode";

	public static String getName (String key)
	{
		if (KM_ROOT_SYS_CAN_LOGIN.equals(key))
		{
			return "Is allowed to log in";
		}
		else if (KM_SYS_ENV_DEFAULT_TITLE.equals(key))
		{
			return "Default application title";
		}
		else if (KM_SYS_DISPLAY_COLLECTIONS_IN_RECORD_DETAILS.equals(key))
		{
			return "Default collections on record details";
		}
		else if (KM_SYS_HOME_PAGE.equals(key))
		{
			return "Default home URL";
		}
		else if (KM_SYS_DEFAULT_LAYOUT_ID.equals(key))
		{
			return "Default layout";
		}
		else if (KM_SYS_LOGIN_URL.equals(key))
		{
			return "Login page URL";
		}
		else if (KM_SYS_404_URL.equals(key))
		{
			return "404 action URL";
		}
		else if (KM_SYS_HOST.equals(key))
		{
			return "Host URL";
		}
		else
		{
			return null;
		}
	}
}