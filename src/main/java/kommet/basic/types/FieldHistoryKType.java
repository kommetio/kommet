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
import kommet.data.datatypes.KIDDataType;
import kommet.data.datatypes.TextDataType;
import kommet.utils.AppConfig;

public class FieldHistoryKType extends Type
{	
	private static final long serialVersionUID = -262133017158351846L;
	private static final String LABEL = "Field History";
	private static final String PLURAL_LABEL = "Field History";
	
	public FieldHistoryKType() throws KommetException
	{
		super();
		this.setApiName(SystemTypes.FIELD_HISTORY_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.FIELD_HISTORY_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.FIELD_HISTORY_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add field id field
		Field fieldIdField = new Field();
		fieldIdField.setApiName("fieldId");
		fieldIdField.setLabel("Field ID");
		fieldIdField.setDataType(new KIDDataType());
		fieldIdField.setDbColumn("fieldid");
		fieldIdField.setRequired(true);
		this.addField(fieldIdField);
		
		// add field id field
		Field recordIdField = new Field();
		recordIdField.setApiName("recordId");
		recordIdField.setLabel("Record ID");
		recordIdField.setDataType(new KIDDataType());
		recordIdField.setDbColumn("recordid");
		recordIdField.setRequired(true);
		this.addField(recordIdField);
		
		// add old value field
		Field oldValueField = new Field();
		oldValueField.setApiName("oldValue");
		oldValueField.setLabel("Old Value");
		oldValueField.setDataType(new TextDataType(1024));
		oldValueField.setDbColumn("oldvalue");
		oldValueField.setRequired(false);
		this.addField(oldValueField);
		
		// add old value field
		Field newValueField = new Field();
		newValueField.setApiName("newValue");
		newValueField.setLabel("New Value");
		newValueField.setDataType(new TextDataType(1024));
		newValueField.setDbColumn("newvalue");
		newValueField.setRequired(false);
		this.addField(newValueField);
		
		// operation type
		Field opTypeField = new Field();
		opTypeField.setApiName("operation");
		opTypeField.setLabel("Operation");
		opTypeField.setDataType(new EnumerationDataType("Update\nAdd\nDelete"));
		opTypeField.setDbColumn("operation");
		opTypeField.setRequired(true);
		this.addField(opTypeField);
	}

}
