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
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.KIDDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class ReminderKType extends Type
{
	private static final long serialVersionUID = 1974026838610978297L;
	private static final String LABEL = "Reminder";
	private static final String PLURAL_LABEL = "Reminders";
	
	public ReminderKType()
	{
		super();
	}
	
	public ReminderKType(Type userType, Type userGroupType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.REMINDER_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.REMINDER_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.REMINDER_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		Field fieldField = new Field();
		fieldField.setApiName("referencedField");
		fieldField.setLabel("Referenced Field");
		fieldField.setDataType(new KIDDataType());
		fieldField.setDbColumn("referencedfield");
		fieldField.setRequired(true);
		this.addField(fieldField);
		
		Field titleField = new Field();
		titleField.setApiName("title");
		titleField.setLabel("Title");
		titleField.setDataType(new TextDataType(500));
		titleField.setDbColumn("title");
		titleField.setRequired(true);
		this.addField(titleField);
		
		Field contentField = new Field();
		contentField.setApiName("content");
		contentField.setLabel("Content");
		contentField.setDataType(new TextDataType(1000000));
		contentField.setDbColumn("content");
		contentField.setRequired(true);
		this.addField(contentField);
		
		Field recordField = new Field();
		recordField.setApiName("recordId");
		recordField.setLabel("Record ID");
		recordField.setDataType(new KIDDataType());
		recordField.setDbColumn("recordid");
		recordField.setRequired(true);
		this.addField(recordField);
		
		Field intervalUnitField = new Field();
		intervalUnitField.setApiName("intervalUnit");
		intervalUnitField.setLabel("Interval Unit");
		intervalUnitField.setDataType(new EnumerationDataType("year\nmonth\nweek\nday\nhour\nminute"));
		intervalUnitField.setDbColumn("intervalunit");
		intervalUnitField.setRequired(true);
		this.addField(intervalUnitField);
		
		Field intervalValueField = new Field();
		intervalValueField.setApiName("intervalValue");
		intervalValueField.setLabel("Interval Value");
		intervalValueField.setDataType(new NumberDataType(0, java.lang.Integer.class));
		intervalValueField.setDbColumn("intervalvalue");
		intervalValueField.setRequired(true);
		this.addField(intervalValueField);
		
		Field assignedUserField = new Field();
		assignedUserField.setApiName("assignedUser");
		assignedUserField.setLabel("Assigned User");
		assignedUserField.setDataType(new TypeReference(userType));
		assignedUserField.setDbColumn("assigneduser");
		assignedUserField.setRequired(false);
		this.addField(assignedUserField);
		
		Field assignedGroupField = new Field();
		assignedGroupField.setApiName("assignedGroup");
		assignedGroupField.setLabel("Assigned Group");
		assignedGroupField.setDataType(new TypeReference(userGroupType));
		assignedGroupField.setDbColumn("assignedgroup");
		assignedGroupField.setRequired(false);
		this.addField(assignedGroupField);
		
		Field mediaField = new Field();
		mediaField.setApiName("media");
		mediaField.setLabel("Media");
		mediaField.setDataType(new EnumerationDataType("email\nnotification\nlogin message"));
		mediaField.setDbColumn("media");
		mediaField.setRequired(true);
		this.addField(mediaField);
		
		Field statusField = new Field();
		statusField.setApiName("status");
		statusField.setLabel("Status");
		statusField.setDataType(new EnumerationDataType("sent\nnotsent"));
		statusField.setDbColumn("status");
		statusField.setRequired(true);
		statusField.setDefaultValue("not sent");
		this.addField(statusField);
	}
}
