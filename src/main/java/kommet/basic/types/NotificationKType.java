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
import kommet.data.datatypes.DateTimeDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class NotificationKType extends Type
{
	private static final long serialVersionUID = -3090994224563225566L;
	private static final String LABEL = "Notification";
	private static final String PLURAL_LABEL = "Notifications";
	
	public NotificationKType()
	{
		super();
	}
	
	public NotificationKType(UserKType userType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.NOTIFICATION_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.NOTIFICATION_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.NOTIFICATION_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add content field
		Field textField = new Field();
		textField.setApiName("text");
		textField.setLabel("Text");
		textField.setDataType(new TextDataType(255));
		textField.setDbColumn("text");
		textField.setRequired(true);
		this.addField(textField);
		
		Field titleField = new Field();
		titleField.setApiName("title");
		titleField.setLabel("Title");
		titleField.setDataType(new TextDataType(100));
		titleField.setDbColumn("title");
		titleField.setRequired(false);
		this.addField(titleField);
		
		// add user field
		Field userField = new Field();
		userField.setApiName("assignee");
		userField.setLabel("Assignee");
		userField.setDataType(new TypeReference(userType));
		userField.setDbColumn("assignee");
		userField.setRequired(true);
		this.addField(userField);
	
		// add isViewed field
		Field displayedField = new Field();
		displayedField.setApiName("viewedDate");
		displayedField.setLabel("Viewed Date");
		displayedField.setDataType(new DateTimeDataType());
		displayedField.setDbColumn("vieweddate");
		displayedField.setRequired(false);
		this.addField(displayedField);
	}
}
