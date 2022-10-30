/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.types;

import java.util.HashSet;
import java.util.Set;

import kommet.data.Type;
import kommet.utils.AppConfig;

public class SystemTypes
{
	public static final String USER_API_NAME = "User";
	public static final String TYPE_PERMISSION_API_NAME = "TypePermission";
	public static final String FIELD_PERMISSION_API_NAME = "FieldPermission";
	public static final String ACTION_PERMISSION_API_NAME = "ActionPermission";
	public static final String PERMISSION_SET_API_NAME = "PermissionSet";
	public static final String PROFILE_API_NAME = "Profile";
	public static final String VIEW_API_NAME = "View";
	public static final String FILE_API_NAME = "File";
	public static final String FILE_REVISION_API_NAME = "FileRevision";
	public static final String CLASS_API_NAME = "Class";
	public static final String LAYOUT_API_NAME = "Layout";
	public static final String SYSTEM_SETTING_API_NAME = "SystemSetting";
	public static final String ACTION_API_NAME = "Action";
	public static final String STANDARD_ACTION_API_NAME = "StandardAction";
	public static final String UNIQUE_CHECK_API_NAME = "UniqueCheck";
	public static final String ENV_API_NAME = "Env";
	public static final String PROFILE_PERMISSION_SET_ASSIGNMENT_API_NAME = "ProfilePermissionSetAssignment";
	public static final String FILE_RECORD_ASSIGNMENT_API_NAME = "FileRecordAssignment";
	public static final String TYPE_INFO_API_NAME = "TypeInfo";
	public static final String COMMENT_API_NAME = "Comment";
	public static final String FIELD_HISTORY_API_NAME = "FieldHistory";
	public static final String TYPE_TRIGGER_API_NAME = "TypeTrigger";
	public static final String USER_RECORD_SHARING_API_NAME = "UserRecordSharing";
	public static final String SCHEDULED_TASK_API_NAME = "ScheduledTask";
	public static final String USER_SETTINGS_API_NAME = "UserSettings";
	public static final String DOC_TEMPLATE_API_NAME = "DocTemplate";
	public static final String TEXT_LABEL_API_NAME = "TextLabel";
	public static final String VALIDATION_RULE_API_NAME = "ValidationRule";
	public static final String EMAIL_API_NAME = "Email";
	public static final String NOTIFICATION_API_NAME = "Notification";
	public static final String ERROR_LOG_API_NAME = "ErrorLog";
	public static final String LOGIN_HISTORY_API_NAME = "LoginHistory";
	public static final String REPORT_TYPE_API_NAME = "ReportType";
	public static final String USER_CASCADE_HIERARCHY_API_NAME = "UserCascadeHierarchy";
	public static final String USER_GROUP_API_NAME = "UserGroup";
	public static final String USER_GROUP_ASSIGNMENT_API_NAME = "UserGroupAssignment";
	public static final String GROUP_RECORD_SHARING_API_NAME = "GroupRecordSharing";
	public static final String SETTING_VALUE_API_NAME = "SettingValue";
	public static final String WEB_RESOURCE_API_NAME = "WebResource";
	public static final String VIEW_RESOURCE_API_NAME = "ViewResource";
	public static final String APP_API_NAME = "App";
	public static final String APP_URL_API_NAME = "AppUrl";
	public static final String TASK_API_NAME = "Task";
	public static final String REMINDER_API_NAME = "Reminder";
	public static final String TASK_DEPENDENCY_API_NAME = "TaskDependency";
	public static final String LIBRARY_API_NAME = "Library";
	public static final String LIBRARY_ITEM_API_NAME = "LibraryItem";
	public static final String EVENT_GUEST_API_NAME = "EventGuest";
	public static final String EVENT_API_NAME = "Event";
	public static final String ANY_RECORD_API_NAME = "AnyRecord";
	public static final String LABEL_API_NAME = "Label";
	public static final String LABEL_ASSIGNMENT_API_NAME = "LabelAssignment";
	public static final String SHARING_RULE_API_NAME = "SharingRule";
	public static final String BUSINESS_PROCESS_API_NAME = "BusinessProcess";
	public static final String BUSINESS_ACTION_API_NAME = "BusinessAction";
	public static final String BUSINESS_PROCESS_INPUT_API_NAME = "BusinessProcessInput";
	public static final String BUSINESS_PROCESS_OUTPUT_API_NAME = "BusinessProcessOutput";
	public static final String BUSINESS_ACTION_INVOCATION_API_NAME = "BusinessActionInvocation";
	public static final String BUSINESS_ACTION_TRANSITION_API_NAME = "BusinessActionTransition";
	public static final String BUSINESS_PROCESS_PARAM_ASSIGNMENT_API_NAME = "BusinessProcessParamAssignment";
	public static final String BUSINESS_ACTION_INVOCATION_ATTRIBUTE_API_NAME = "BusinessActionInvocationAttribute";
	public static final String BUTTON_API_NAME = "Button";
	public static final String DICTIONARY_API_NAME = "Dictionary";
	public static final String DICTIONARY_ITEM_API_NAME = "DictionaryItem";
	public static final long USER_ID_SEQ = 0;
	public static final long PERMISSION_SET_ID_SEQ = 1;
	public static final long ENV_ID_SEQ = 2;
	public static final long PROFILE_PERMISSION_SET_ASSIGNMENT_ID_SEQ = 3;
	public static final long PROFILE_ID_SEQ = 4;
	public static final long TYPE_PERMISSION_ID_SEQ = 5;
	public static final long FIELD_PERMISSION_ID_SEQ = 6;
	public static final long ACTION_PERMISSION_ID_SEQ = 7;
	public static final long VIEW_ID_SEQ = 8;
	public static final long CLASS_ID_SEQ = 9;
	public static final long ACTION_ID_SEQ = 10;
	public static final long STANDARD_ACTION_ID_SEQ = 11;
	public static final long UNIQUE_CHECK_ID_SEQ = 12;
	public static final long LAYOUT_ID_SEQ = 13;
	public static final long SYSTEM_SETTING_ID_SEQ = 14;
	public static final long FILE_ID_SEQ = 15;
	public static final long FILE_REVISION_ID_SEQ = 16;
	public static final long FILE_OBJECT_ASSIGNMENT_ID_SEQ = 17;
	public static final long TYPE_INFO_ID_SEQ = 18;
	public static final long COMMENT_ID_SEQ = 19;
	public static final long FIELD_HISTORY_ID_SEQ = 20;
	public static final long TYPE_TRIGGER_ID_SEQ = 21;
	public static final long USER_RECORD_SHARING_ID_SEQ = 22;
	public static final long SCHEDULED_TASK_ID_SEQ = 23;
	public static final long USER_SETTINGS_ID_SEQ = 24;
	public static final long DOC_TEMPLATE_ID_SEQ = 25;
	public static final long TEXT_LABEL_ID_SEQ = 26;
	public static final long VALIDATION_RULE_ID_SEQ = 27;
	public static final long EMAIL_ID_SEQ = 28;
	public static final long NOTIFICATION_ID_SEQ = 29;
	public static final long ERROR_LOG_ID_SEQ = 30;
	public static final long LOGIN_HISTORY_ID_SEQ = 31;
	public static final long REPORT_TYPE_ID_SEQ = 32;
	public static final long USER_CASCADE_HIERARCHY_ID_SEQ = 33;
	public static final long USER_GROUP_ID_SEQ = 34;
	public static final long USER_GROUP_ASSIGNMENT_ID_SEQ = 35;
	public static final long GROUP_RECORD_SHARING_ID_SEQ = 36;
	public static final long SETTING_VALUE_ID_SEQ = 37;
	public static final long WEB_RESOURCE_ID_SEQ = 38;
	public static final long VIEW_RESOURCE_ID_SEQ = 39;
	public static final long APP_ID_SEQ = 40;
	public static final long APP_URL_ID_SEQ = 41;
	public static final long TASK_ID_SEQ = 42;
	public static final long TASK_DEPENDENCY_ID_SEQ = 43;
	public static final long LIBRARY_ID_SEQ = 44;
	public static final long LIBRARY_ITEM_ID_SEQ = 45;
	public static final long EVENT_ID_SEQ = 46;
	public static final long ANY_RECORD_ID_SEQ = 47;
	public static final long EVENT_GUEST_ID_SEQ = 48;
	public static final long LABEL_ID_SEQ = 49;
	public static final long LABEL_ASSIGNMENT_ID_SEQ = 50;
	public static final long SHARING_RULE_ID_SEQ = 51;
	public static final long BUSINESS_PROCESS_ID_SEQ = 52;
	public static final long BUSINESS_ACTION_ID_SEQ = 53;
	public static final long BUSINESS_PROCESS_INPUT_ID_SEQ = 54;
	public static final long BUSINESS_PROCESS_OUTPUT_ID_SEQ = 55;
	public static final long BUSINESS_ACTION_INVOCATION_ID_SEQ = 56;
	public static final long BUSINESS_ACTION_TRANSITION_ID_SEQ = 57;
	public static final long BUSINESS_PROCESS_PARAM_ASSIGNMENT_ID_SEQ = 58;
	public static final long BUSINESS_ACTION_INVOCATION_ATTRIBUTE_ID_SEQ = 59;
	public static final long BUTTON_ID_SEQ = 60;
	public static final long DICTIONARY_ID_SEQ = 61;
	public static final long DICTIONARY_ITEM_ID_SEQ = 62;
	public static final long REMINDER_ID_SEQ = 63;
	
	private static Set<String> inaccessibleSystemTypeApiNames;
	
	/**
	 * Inaccessible system types are basic (built-in) types that cannot be viewed or modified.
	 * Most system types belong to them, however, some system types are customizable and thus are not inaccessible.
	 * These types are:
	 * - files
	 * - users
	 * 
	 * @param type
	 * @return
	 */
	public static boolean isInaccessibleSystemType (Type type)
	{	
		if (inaccessibleSystemTypeApiNames == null)
		{
			inaccessibleSystemTypeApiNames = new HashSet<String>();
			inaccessibleSystemTypeApiNames.add(PERMISSION_SET_API_NAME);
			inaccessibleSystemTypeApiNames.add(ENV_API_NAME);
			inaccessibleSystemTypeApiNames.add(PROFILE_PERMISSION_SET_ASSIGNMENT_API_NAME);
			inaccessibleSystemTypeApiNames.add(TYPE_PERMISSION_API_NAME);
			inaccessibleSystemTypeApiNames.add(FIELD_PERMISSION_API_NAME);
			inaccessibleSystemTypeApiNames.add(ACTION_PERMISSION_API_NAME);
			inaccessibleSystemTypeApiNames.add(VIEW_API_NAME);
			inaccessibleSystemTypeApiNames.add(CLASS_API_NAME);
			inaccessibleSystemTypeApiNames.add(ACTION_API_NAME);
			inaccessibleSystemTypeApiNames.add(STANDARD_ACTION_API_NAME);
			inaccessibleSystemTypeApiNames.add(UNIQUE_CHECK_API_NAME);
			inaccessibleSystemTypeApiNames.add(LAYOUT_API_NAME);
			inaccessibleSystemTypeApiNames.add(SYSTEM_SETTING_API_NAME);
			inaccessibleSystemTypeApiNames.add(TYPE_INFO_API_NAME);
			inaccessibleSystemTypeApiNames.add(FIELD_HISTORY_API_NAME);
			inaccessibleSystemTypeApiNames.add(TYPE_TRIGGER_API_NAME);
			inaccessibleSystemTypeApiNames.add(USER_RECORD_SHARING_API_NAME);
			inaccessibleSystemTypeApiNames.add(SCHEDULED_TASK_API_NAME);
			inaccessibleSystemTypeApiNames.add(USER_SETTINGS_API_NAME);
			inaccessibleSystemTypeApiNames.add(DOC_TEMPLATE_API_NAME);
			inaccessibleSystemTypeApiNames.add(TEXT_LABEL_API_NAME);
			inaccessibleSystemTypeApiNames.add(VALIDATION_RULE_API_NAME);
			inaccessibleSystemTypeApiNames.add(EMAIL_API_NAME);
			inaccessibleSystemTypeApiNames.add(NOTIFICATION_API_NAME);
			inaccessibleSystemTypeApiNames.add(ERROR_LOG_API_NAME);
			inaccessibleSystemTypeApiNames.add(LOGIN_HISTORY_API_NAME);
			inaccessibleSystemTypeApiNames.add(REPORT_TYPE_API_NAME);
			inaccessibleSystemTypeApiNames.add(USER_CASCADE_HIERARCHY_API_NAME);
			inaccessibleSystemTypeApiNames.add(GROUP_RECORD_SHARING_API_NAME);
			inaccessibleSystemTypeApiNames.add(SETTING_VALUE_API_NAME);
			inaccessibleSystemTypeApiNames.add(APP_API_NAME);
			inaccessibleSystemTypeApiNames.add(APP_URL_API_NAME);
			inaccessibleSystemTypeApiNames.add(TASK_DEPENDENCY_API_NAME);
			inaccessibleSystemTypeApiNames.add(LIBRARY_API_NAME);
			inaccessibleSystemTypeApiNames.add(LIBRARY_ITEM_API_NAME);
			inaccessibleSystemTypeApiNames.add(ANY_RECORD_API_NAME);
			inaccessibleSystemTypeApiNames.add(SHARING_RULE_API_NAME);
			inaccessibleSystemTypeApiNames.add(BUSINESS_PROCESS_API_NAME);
			inaccessibleSystemTypeApiNames.add(BUSINESS_ACTION_API_NAME);
			inaccessibleSystemTypeApiNames.add(BUSINESS_PROCESS_INPUT_API_NAME);
			inaccessibleSystemTypeApiNames.add(BUSINESS_PROCESS_OUTPUT_API_NAME);
			inaccessibleSystemTypeApiNames.add(BUSINESS_ACTION_INVOCATION_API_NAME);
			inaccessibleSystemTypeApiNames.add(BUSINESS_ACTION_TRANSITION_API_NAME);
			inaccessibleSystemTypeApiNames.add(BUSINESS_PROCESS_PARAM_ASSIGNMENT_API_NAME);
			inaccessibleSystemTypeApiNames.add(BUSINESS_ACTION_INVOCATION_ATTRIBUTE_API_NAME);
			inaccessibleSystemTypeApiNames.add(BUTTON_API_NAME);
			inaccessibleSystemTypeApiNames.add(DICTIONARY_API_NAME);
			inaccessibleSystemTypeApiNames.add(DICTIONARY_ITEM_API_NAME);
		}
		
		return type.isBasic() && inaccessibleSystemTypeApiNames.contains(type.getApiName());
	}
	
	/**
	 * Returns the qualified name of a basic system object
	 * @param apiName
	 * @return
	 */
	public static String getSystemTypeQualifiedName(String apiName)
	{
		return AppConfig.BASE_TYPE_PACKAGE + "." + apiName;
	}
}

