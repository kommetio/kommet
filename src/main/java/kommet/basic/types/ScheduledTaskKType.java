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
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class ScheduledTaskKType extends Type
{
	private static final long serialVersionUID = -5838339125201522813L;
	private static final String LABEL = "Scheduled Task";
	private static final String PLURAL_LABEL = "Scheduled Tasks";
	
	public ScheduledTaskKType()
	{
		super();
	}
	
	public ScheduledTaskKType(Type classType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.SCHEDULED_TASK_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.SCHEDULED_TASK_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.SCHEDULED_TASK_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add name field
		Field nameField = new Field();
		nameField.setApiName("name");
		nameField.setLabel("Name");
		nameField.setDataType(new TextDataType(50));
		nameField.setDbColumn("name");
		nameField.setRequired(true);
		this.addField(nameField);
		
		Field fileField = new Field();
		fileField.setApiName("file");
		fileField.setLabel("File");
		fileField.setDataType(new TypeReference(classType));
		fileField.setDbColumn("file");
		fileField.setRequired(true);
		this.addField(fileField);
		
		Field methodField = new Field();
		methodField.setApiName("method");
		methodField.setLabel("Method");
		methodField.setDataType(new TextDataType(100));
		methodField.setDbColumn("method");
		methodField.setRequired(true);
		this.addField(methodField);
		
		Field cronField = new Field();
		cronField.setApiName("cronExpression");
		cronField.setLabel("CRON Expression");
		cronField.setDataType(new TextDataType(50));
		cronField.setDbColumn("cronexpression");
		cronField.setRequired(true);
		this.addField(cronField);
	}
}
