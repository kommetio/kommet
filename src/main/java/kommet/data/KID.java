/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

import kommet.utils.BaseConverter;
import kommet.utils.MiscUtils;

public class KID
{
	public static final String ENV_PREFIX = "001";
	public static final String TYPE_PREFIX = "002";
	public static final String FIELD_PREFIX = "003";
	public static final String USER_PREFIX = "004";
	public static final String PERMISSION_SET_PREFIX = "005";
	public static final String PROFILE_PREFIX = "006";
	public static final String TYPE_PERMISSION_PREFIX = "007";
	public static final String FIELD_PERMISSION_PREFIX = "008";
	public static final String ACTION_PERMISSION_PREFIX = "009";
	public static final String VIEW_PREFIX = "00a";
	public static final String CLASS_PREFIX = "00b";
	public static final String ACTION_PREFIX = "00c";
	public static final String STANDARD_ACTION_PREFIX = "00d";
	public static final String UNIQUE_CHECK_PREFIX = "00e";
	public static final String LAYOUT_PREFIX = "00f";
	public static final String SYSTEM_SETTING_PREFIX = "00g";
	public static final String FILE_PREFIX = "00h";
	public static final String FILE_REVISION_PREFIX = "00i";
	public static final String FILE_RECORD_ASSIGNMENT_PREFIX = "00j";
	public static final String TYPE_INFO_PREFIX = "00k";
	public static final String COMMENT_PREFIX = "00l";
	public static final String FIELD_HISTORY_PREFIX = "00m";
	public static final String TYPE_TRIGGER_PREFIX = "00n";
	public static final String USER_RECORD_SHARING_PREFIX = "00o";
	public static final String SCHEDULED_TASK_PREFIX = "00p";
	public static final String USER_SETTINGS_PREFIX = "00q";
	public static final String DOC_TEMPLATE_PREFIX = "00r";
	public static final String TEXT_LABEL_PREFIX = "00s";
	public static final String VALIDATION_RULE_PREFIX = "00t";
	public static final String EMAIL_PREFIX = "00u";
	public static final String NOTIFICATION_PREFIX = "00v";
	public static final String ERROR_LOG_PREFIX = "00w";
	public static final String LOGIN_HISTORY_PREFIX = "00x";
	public static final String REPORT_TYPE_PREFIX = "00y";
	public static final String USER_CASCADE_HIERARCHY_PREFIX = "00z";
	public static final String USER_GROUP_PREFIX = "010";
	public static final String USER_GROUP_ASSIGNMENT_PREFIX = "011";
	public static final String GROUP_RECORD_SHARING_PREFIX = "012";
	public static final String SETTING_VALUE_PREFIX = "013";
	public static final String WEB_RESOURCE_PREFIX = "014";
	public static final String VIEW_RESOURCE_PREFIX = "015";
	public static final String APP_PREFIX = "016";
	public static final String APP_URL_PREFIX = "017";
	public static final String TASK_PREFIX = "018";
	public static final String TASK_DEPENDENCY_PREFIX = "019";
	public static final String LIBRARY_PREFIX = "020";
	public static final String LIBRARY_ITEM_PREFIX = "021";
	public static final String EVENT_PREFIX = "022";
	public static final String ANY_RECORD_PREFIX = "023";
	public static final String EVENT_GUEST_PREFIX = "024";
	public static final String LABEL_PREFIX = "025";
	public static final String LABEL_ASSIGNMENT_PREFIX = "026";
	public static final String SHARING_RULE_PREFIX = "027";
	public static final String BUSINESS_PROCESS_PREFIX = "028";
	public static final String BUSINESS_ACTION_PREFIX = "029";
	public static final String BUSINESS_PROCESS_INPUT_PREFIX = "030";
	public static final String BUSINESS_PROCESS_OUTPUT_PREFIX = "031";
	public static final String BUSINESS_ACTION_INVOCATION_PREFIX = "032";
	public static final String BUSINESS_ACTION_TRANSITION_PREFIX = "033";
	public static final String BUSINESS_PROCESS_PARAM_ASSIGNMENT_PREFIX = "034";
	public static final String BUSINESS_ACTION_INVOCATION_ATTRIBUTE_PREFIX = "035";
	public static final String BUTTON_PREFIX = "036";
	public static final String DICTIONARY_PREFIX = "037";
	public static final String DICTIONARY_ITEM_PREFIX = "038";
	public static final String REMINDER_PREFIX = "039";
	
	// length of the identifier
	public static final int LENGTH = 13;
	
	// the actual string representation of the ID
	private String id;

	public static KID get (String prefix, Long sequence) throws KIDException
	{
		return KID.get(prefix + MiscUtils.padLeft(BaseConverter.convertToKommetBase(sequence), 10, '0'));
	}
	
	public KID (String id) throws KIDException
	{
		if (id == null)
		{
			throw new KIDException("KID is null");
		}
		else if (id.length() != LENGTH)
		{
			throw new KIDException("Invalid string '" + id + "' for KID. It has length " + id.length() + " instead of expected " + LENGTH);
		}
		
		this.id = id;
	}
	
	public static KID get (String id) throws KIDException
	{
		return new KID(id);
	}
	
	public void setId (String id) throws KIDException
	{
		if (id == null)
		{
			throw new KIDException("KID is null");
		}
		else if (id.length() != LENGTH)
		{
			throw new KIDException("KID has length " + id.length() + " instead of expected " + LENGTH);
		}
		
		this.id = id;
	}

	public String getId()
	{
		return id;
	}
	
	@Override
	public boolean equals (Object obj)
	{
		return obj != null && (obj instanceof KID) && ((KID)obj).getId().equals(this.id);
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}
	
	@Override
	public String toString()
	{
		return this.id;
	}

	public KeyPrefix getKeyPrefix() throws KeyPrefixException
	{
		return this.id != null ? KeyPrefix.get(this.id.substring(0, 3)) : null;
	}
}	