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
import kommet.data.datatypes.KIDDataType;
import kommet.data.datatypes.TextDataType;
import kommet.utils.AppConfig;

public class ValidationRuleKType extends Type
{
	private static final long serialVersionUID = -5130987518795696849L;
	private static final String LABEL = "Validation Rule";
	private static final String PLURAL_LABEL = "Validation Rules";
	
	public ValidationRuleKType() throws KommetException
	{
		super();
		this.setApiName(SystemTypes.VALIDATION_RULE_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.VALIDATION_RULE_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.VALIDATION_RULE_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add type field - references the file for which info is stored in this object
		Field typeField = new Field();
		typeField.setApiName("typeId");
		typeField.setLabel("Type ID");
		typeField.setDataType(new KIDDataType());
		typeField.setDbColumn("typeid");
		typeField.setRequired(true);
		this.addField(typeField);
		
		// add name field
		Field nameField = new Field();
		nameField.setApiName("name");
		nameField.setLabel("Name");
		nameField.setDataType(new TextDataType(100));
		nameField.setDbColumn("name");
		nameField.setRequired(true);
		this.addField(nameField);
		
		// add name field
		Field codeField = new Field();
		codeField.setApiName("code");
		codeField.setLabel("Code");
		codeField.setDataType(new TextDataType(1024));
		codeField.setDbColumn("code");
		codeField.setRequired(true);
		this.addField(codeField);
				
		// add is active field
		Field isActiveField = new Field();
		isActiveField.setApiName("active");
		isActiveField.setLabel("Active");
		isActiveField.setDataType(new BooleanDataType());
		isActiveField.setDbColumn("isactive");
		isActiveField.setRequired(true);
		this.addField(isActiveField);
		
		// add error message field
		Field msgField = new Field();
		msgField.setApiName("errorMessage");
		msgField.setLabel("Error Message");
		msgField.setDataType(new TextDataType(256));
		msgField.setDbColumn("errormessage");
		msgField.setRequired(false);
		this.addField(msgField);
		
		// add error message label field
		Field msgLabelField = new Field();
		msgLabelField.setApiName("errorMessageLabel");
		msgLabelField.setLabel("Error Message Label");
		msgLabelField.setDataType(new TextDataType(100));
		msgLabelField.setDbColumn("errormessagelabel");
		msgLabelField.setRequired(false);
		this.addField(msgLabelField);
		
		Field isSystemField = new Field();
		isSystemField.setApiName("isSystem");
		isSystemField.setLabel("Is System");
		isSystemField.setDataType(new BooleanDataType());
		isSystemField.setDbColumn("issystem");
		isSystemField.setRequired(true);
		this.addField(isSystemField);
		
		Field referencedFieldsField = new Field();
		referencedFieldsField.setApiName("referencedFields");
		referencedFieldsField.setLabel("Referenced Fields");
		referencedFieldsField.setDataType(new TextDataType(255));
		referencedFieldsField.setDbColumn("referencedfields");
		referencedFieldsField.setRequired(true);
		this.addField(referencedFieldsField);
	}

}
