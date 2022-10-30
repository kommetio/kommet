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

public class EventKType extends Type
{
	private static final long serialVersionUID = -7019123231139091377L;
	private static final String LABEL = "Event";
	private static final String PLURAL_LABEL = "Events";
	
	public EventKType()
	{
		super();
	}
	
	public EventKType(UserKType userType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.EVENT_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.EVENT_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.EVENT_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add name field
		Field nameField = new Field();
		nameField.setApiName("name");
		nameField.setLabel("Name");
		nameField.setDataType(new TextDataType(255));
		nameField.setDbColumn("name");
		nameField.setRequired(true);
		this.addField(nameField);
		
		// add start date field
		Field startDateField = new Field();
		startDateField.setApiName("startDate");
		startDateField.setLabel("Start Date");
		startDateField.setDataType(new DateTimeDataType());
		startDateField.setDbColumn("startdate");
		startDateField.setRequired(true);
		this.addField(startDateField);
		
		// add start date field
		Field endDateField = new Field();
		endDateField.setApiName("endDate");
		endDateField.setLabel("End Date");
		endDateField.setDataType(new DateTimeDataType());
		endDateField.setDbColumn("enddate");
		endDateField.setRequired(true);
		this.addField(endDateField);
		
		// add description field
		Field descField = new Field();
		descField.setApiName("description");
		descField.setLabel("Description");
		descField.setDataType(new TextDataType(32000));
		descField.setDbColumn("description");
		descField.setRequired(false);
		this.addField(descField);
		
		Field ownerField = new Field();
		ownerField.setApiName("owner");
		ownerField.setLabel("Owner");
		ownerField.setDataType(new TypeReference(userType));
		ownerField.setDbColumn("owner");
		ownerField.setRequired(true);
		this.addField(ownerField);
	}
}

