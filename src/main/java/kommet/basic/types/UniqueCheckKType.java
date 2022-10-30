/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.types;

import kommet.basic.UniqueCheck;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.BooleanDataType;
import kommet.data.datatypes.KIDDataType;
import kommet.data.datatypes.TextDataType;
import kommet.utils.AppConfig;

public class UniqueCheckKType extends Type
{	
	private static final long serialVersionUID = 1270048070670743057L;
	private static final String LABEL = "Unique Check";
	private static final String PLURAL_LABEL = "Unique Checks";
	private static final int ALLOWED_FIELDS_IN_CHECK = 10;
	
	public UniqueCheckKType() throws KommetException
	{
		super();
		this.setApiName(SystemTypes.UNIQUE_CHECK_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.UNIQUE_CHECK_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.UNIQUE_CHECK_ID_SEQ));
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
		
		// add DB name field
		Field dbNameField = new Field();
		dbNameField.setApiName("dbName");
		dbNameField.setLabel("Database Name");
		dbNameField.setDataType(new TextDataType(ALLOWED_FIELDS_IN_CHECK * 13 + UniqueCheck.DB_CHECK_NAME_PREFIX.length() + 14));
		dbNameField.setDbColumn("dbname");
		dbNameField.setRequired(true);
		this.addField(dbNameField);
		
		// add type field
		Field objectRef = new Field();
		objectRef.setApiName("typeId");
		objectRef.setDbColumn("typeid");
		objectRef.setLabel("Type ID");
		objectRef.setDataType(new KIDDataType());
		objectRef.setRequired(true);
		this.addField(objectRef);
		
		Field isSystemField = new Field();
		isSystemField.setApiName("isSystem");
		isSystemField.setDbColumn("issystem");
		isSystemField.setLabel("Is System");
		isSystemField.setDataType(new BooleanDataType());
		isSystemField.setRequired(true);
		this.addField(isSystemField);
		
		// This field will contain a semi-colon separated list of KIDs of fields
		Field fieldListField = new Field();
		fieldListField.setApiName("fieldIds");
		fieldListField.setLabel("Field IDs");
		// up to 10 fields allowed in the check, each field's ID has length 13, so it gives 130 + semi-colons
		fieldListField.setDataType(new TextDataType(ALLOWED_FIELDS_IN_CHECK * 11 + (ALLOWED_FIELDS_IN_CHECK - 1)));
		fieldListField.setDbColumn("fieldids");
		fieldListField.setRequired(true);
		this.addField(fieldListField);
	}
}
