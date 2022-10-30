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
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class BusinessActionKType extends Type
{
	private static final long serialVersionUID = -787886777983363273L;
	private static final String LABEL = "Business Action";
	private static final String PLURAL_LABEL = "Business Actions";
	
	public BusinessActionKType() throws KommetException
	{
		super();
	}
	
	public BusinessActionKType(ClassKType classType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.BUSINESS_ACTION_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.BUSINESS_ACTION_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.BUSINESS_ACTION_ID_SEQ));
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
		
		// add description field
		Field descField = new Field();
		descField.setApiName("description");
		descField.setLabel("Description");
		descField.setDataType(new TextDataType(1000));
		descField.setDbColumn("description");
		descField.setRequired(false);
		this.addField(descField);
		
		Field classField = new Field();
		classField.setApiName("file");
		classField.setLabel("File");
		classField.setDataType(new TypeReference(classType));
		classField.setDbColumn("file");
		classField.setRequired(false);
		this.addField(classField);
		
		Field typeField = new Field();
		typeField.setApiName("type");
		typeField.setLabel("Type");
		EnumerationDataType typeDT = new EnumerationDataType("Action\nIf\nForEach\nRecordCreate\nRecordUpdate\nRecordSave\nStop\nTypeCast\nFieldUpdate\nFieldValue");
		typeDT.setValidateValues(true);
		typeField.setDataType(typeDT);
		typeField.setDbColumn("type");
		typeField.setRequired(true);
		this.addField(typeField);
		
		Field isEntryPointField = new Field();
		isEntryPointField.setApiName("isEntryPoint");
		isEntryPointField.setLabel("Is Entry Point");
		isEntryPointField.setDataType(new BooleanDataType());
		isEntryPointField.setDbColumn("isentrypoint");
		isEntryPointField.setRequired(true);
		this.addField(isEntryPointField);
	}
}

