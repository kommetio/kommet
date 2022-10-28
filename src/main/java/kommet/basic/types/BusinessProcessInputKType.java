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
import kommet.data.datatypes.KIDDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class BusinessProcessInputKType extends Type
{
	private static final long serialVersionUID = -8333987699596691905L;
	private static final String LABEL = "Business Process Input";
	private static final String PLURAL_LABEL = "Business Process Inputs";
	
	public BusinessProcessInputKType() throws KommetException
	{
		super();
	}
	
	public BusinessProcessInputKType(BusinessActionKType bpaType, BusinessProcessKType bpType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.BUSINESS_PROCESS_INPUT_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.BUSINESS_PROCESS_INPUT_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.BUSINESS_PROCESS_INPUT_ID_SEQ));
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
		
		// add data type ID field
		Field dataTypeField = new Field();
		dataTypeField.setApiName("dataTypeName");
		dataTypeField.setLabel("Data Type Name");
		dataTypeField.setDataType(new TextDataType(255));
		dataTypeField.setDbColumn("datatypename");
		dataTypeField.setRequired(false);
		this.addField(dataTypeField);
		
		// add data type name field
		Field dataTypeIdField = new Field();
		dataTypeIdField.setApiName("dataTypeId");
		dataTypeIdField.setLabel("Data Type ID");
		dataTypeIdField.setDataType(new KIDDataType());
		dataTypeIdField.setDbColumn("datatypeid");
		dataTypeIdField.setRequired(false);
		this.addField(dataTypeIdField);
		
		// add description field
		Field descField = new Field();
		descField.setApiName("description");
		descField.setLabel("Description");
		descField.setDataType(new TextDataType(1000));
		descField.setDbColumn("description");
		descField.setRequired(false);
		this.addField(descField);
		
		// add business process field
		Field bpField = new Field();
		bpField.setApiName("businessProcess");
		bpField.setLabel("Business Process");
		
		TypeReference bpRef = new TypeReference(bpType);
		bpRef.setCascadeDelete(true);
		bpField.setDataType(bpRef);
		bpField.setDbColumn("businessprocess");
		bpField.setRequired(false);
		this.addField(bpField);
		
		// add business action field
		Field bpaField = new Field();
		bpaField.setApiName("businessAction");
		bpaField.setLabel("Business Action");
		
		TypeReference bpaRef = new TypeReference(bpaType);
		bpaRef.setCascadeDelete(true);
		bpaField.setDataType(bpaRef);
		bpaField.setDbColumn("businessaction");
		bpaField.setRequired(false);
		this.addField(bpaField);
	}
}

