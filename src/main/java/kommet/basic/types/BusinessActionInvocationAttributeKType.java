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

public class BusinessActionInvocationAttributeKType extends Type
{
	private static final long serialVersionUID = 1451515042842995757L;
	private static final String LABEL = "Business Action Invocation Attribute";
	private static final String PLURAL_LABEL = "Business Action Invocation Attributes";
	
	public BusinessActionInvocationAttributeKType() throws KommetException
	{
		super();
	}
	
	public BusinessActionInvocationAttributeKType(BusinessActionInvocationKType invType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.BUSINESS_ACTION_INVOCATION_ATTRIBUTE_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.BUSINESS_ACTION_INVOCATION_ATTRIBUTE_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.BUSINESS_ACTION_INVOCATION_ATTRIBUTE_ID_SEQ));
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
		
		// add label field
		Field valueField = new Field();
		valueField.setApiName("value");
		valueField.setLabel("Value");
		valueField.setDataType(new TextDataType(1000));
		valueField.setDbColumn("value");
		valueField.setRequired(true);
		this.addField(valueField);
		
		// add compiled class field
		Field invField = new Field();
		invField.setApiName("invocation");
		invField.setLabel("Invocation");
		TypeReference invRef = new TypeReference(invType);
		invRef.setCascadeDelete(true);
		invField.setDataType(invRef);
		invField.setDbColumn("invocation");
		invField.setRequired(true);
		this.addField(invField);
	}
}

