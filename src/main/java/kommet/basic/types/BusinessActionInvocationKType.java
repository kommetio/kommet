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

public class BusinessActionInvocationKType extends Type
{
	private static final long serialVersionUID = -787886777983363273L;
	private static final String LABEL = "Business Action Invocation";
	private static final String PLURAL_LABEL = "Business Action Invocations";
	
	public BusinessActionInvocationKType() throws KommetException
	{
		super();
	}
	
	public BusinessActionInvocationKType(BusinessProcessKType bpType, BusinessActionKType bpaType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.BUSINESS_ACTION_INVOCATION_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.BUSINESS_ACTION_INVOCATION_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.BUSINESS_ACTION_INVOCATION_ID_SEQ));
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
		
		// add business process field
		Field bpField = new Field();
		bpField.setApiName("parentProcess");
		bpField.setLabel("Parent Process");
		
		TypeReference bpRef = new TypeReference(bpType);
		bpRef.setCascadeDelete(true);
		bpField.setDataType(bpRef);
		bpField.setDbColumn("parentprocess");
		bpField.setRequired(true);
		this.addField(bpField);
		
		Field invokedProcessField = new Field();
		invokedProcessField.setApiName("invokedProcess");
		invokedProcessField.setLabel("Invoked Process");
		
		TypeReference invokedProcessRef = new TypeReference(bpType);
		invokedProcessRef.setCascadeDelete(true);
		invokedProcessField.setDataType(invokedProcessRef);
		invokedProcessField.setDbColumn("invokedprocess");
		invokedProcessField.setRequired(false);
		this.addField(invokedProcessField);
		
		// add business process action field
		Field bpaField = new Field();
		bpaField.setApiName("invokedAction");
		bpaField.setLabel("Invoked Action");
		
		TypeReference bpaRef = new TypeReference(bpaType);
		bpaRef.setCascadeDelete(true);
		bpaField.setDataType(bpaRef);
		bpaField.setDbColumn("invokedaction");
		bpaField.setRequired(false);
		this.addField(bpaField);
	}

}
