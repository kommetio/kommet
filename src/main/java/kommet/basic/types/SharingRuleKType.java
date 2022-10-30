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
import kommet.data.datatypes.KIDDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class SharingRuleKType extends Type
{
	private static final long serialVersionUID = 7136231394702461711L;
	private static final String LABEL = "Sharing Rule";
	private static final String PLURAL_LABEL = "Sharing Rules";
	
	public SharingRuleKType() throws KommetException
	{
		super();
	}
	
	public SharingRuleKType(ClassKType classType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.SHARING_RULE_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.SHARING_RULE_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.SHARING_RULE_ID_SEQ));
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
		
		Field typeField = new Field();
		typeField.setApiName("type");
		typeField.setLabel("Type");
		EnumerationDataType typeDT = new EnumerationDataType("Code\nCriteria");
		typeDT.setValidateValues(true);
		typeField.setDataType(typeDT);
		typeField.setDbColumn("type");
		typeField.setRequired(true);
		this.addField(typeField);
		
		Field classField = new Field();
		classField.setApiName("file");
		classField.setLabel("File");
		classField.setDataType(new TypeReference(classType));
		classField.setDbColumn("file");
		classField.setRequired(false);
		this.addField(classField);
		
		Field methodField = new Field();
		methodField.setApiName("method");
		methodField.setLabel("Method");
		methodField.setDataType(new TextDataType(255));
		methodField.setDbColumn("method");
		methodField.setRequired(false);
		this.addField(methodField);
		
		Field editField = new Field();
		editField.setApiName("isEdit");
		editField.setLabel("Is Edit");
		editField.setDataType(new BooleanDataType());
		editField.setDbColumn("isedit");
		editField.setRequired(true);
		this.addField(editField);
		
		Field deleteField = new Field();
		deleteField.setApiName("isDelete");
		deleteField.setLabel("Is Delete");
		deleteField.setDataType(new BooleanDataType());
		deleteField.setDbColumn("isdelete");
		deleteField.setRequired(true);
		this.addField(deleteField);
		
		Field referencedTypeField = new Field();
		referencedTypeField.setApiName("referencedType");
		referencedTypeField.setLabel("Referenced Type");
		referencedTypeField.setDataType(new KIDDataType());
		referencedTypeField.setDbColumn("referencedtype");
		referencedTypeField.setRequired(true);
		this.addField(referencedTypeField);
		
		Field dependentTypesField = new Field();
		dependentTypesField.setApiName("dependentTypes");
		dependentTypesField.setLabel("Dependent Types");
		dependentTypesField.setDataType(new TextDataType(10000));
		dependentTypesField.setDbColumn("dependenttypes");
		dependentTypesField.setRequired(false);
		this.addField(dependentTypesField);
		
		Field sharedWithField = new Field();
		sharedWithField.setApiName("sharedWith");
		sharedWithField.setLabel("Shared With");
		EnumerationDataType sharedWithDT = new EnumerationDataType("User\nUserGroup");
		sharedWithDT.setValidateValues(true);
		sharedWithField.setDataType(sharedWithDT);
		sharedWithField.setDbColumn("sharedwith");
		sharedWithField.setRequired(true);
		this.addField(sharedWithField);
	}
}

