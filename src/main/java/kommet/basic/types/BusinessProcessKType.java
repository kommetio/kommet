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
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class BusinessProcessKType extends Type
{
	private static final long serialVersionUID = 1451515042842995757L;
	private static final String LABEL = "Business Process";
	private static final String PLURAL_LABEL = "Business Processes";
	
	public BusinessProcessKType() throws KommetException
	{
		super();
	}
	
	public BusinessProcessKType(ClassKType classType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.BUSINESS_PROCESS_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.BUSINESS_PROCESS_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.BUSINESS_PROCESS_ID_SEQ));
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
		Field labelField = new Field();
		labelField.setApiName("label");
		labelField.setLabel("Label");
		labelField.setDataType(new TextDataType(100));
		labelField.setDbColumn("label");
		labelField.setRequired(true);
		this.addField(labelField);
		
		// add description field
		Field descField = new Field();
		descField.setApiName("description");
		descField.setLabel("Description");
		descField.setDataType(new TextDataType(1000));
		descField.setDbColumn("description");
		descField.setRequired(false);
		this.addField(descField);
		
		Field displayField = new Field();
		displayField.setApiName("displaySettings");
		displayField.setLabel("Display Setting");
		displayField.setDataType(new TextDataType(32000));
		displayField.setDbColumn("displaysettings");
		displayField.setRequired(false);
		this.addField(displayField);
		
		// add invocation order field
		Field invocationOrderField = new Field();
		invocationOrderField.setApiName("invocationOrder");
		invocationOrderField.setLabel("Invocation Order");
		invocationOrderField.setDataType(new TextDataType(32000));
		invocationOrderField.setDbColumn("invocationorder");
		invocationOrderField.setRequired(false);
		this.addField(invocationOrderField);
		
		// add compiled class field
		Field compiledFileField = new Field();
		compiledFileField.setApiName("compiledClass");
		compiledFileField.setLabel("Compiled Class");
		compiledFileField.setDataType(new TypeReference(classType));
		compiledFileField.setDbColumn("compiledclass");
		compiledFileField.setRequired(false);
		this.addField(compiledFileField);
		
		Field isActiveField = new Field();
		isActiveField.setApiName("isActive");
		isActiveField.setLabel("Is Active");
		isActiveField.setDataType(new BooleanDataType());
		isActiveField.setDbColumn("isactive");
		isActiveField.setRequired(true);
		this.addField(isActiveField);
		
		Field isDraftField = new Field();
		isDraftField.setApiName("isDraft");
		isDraftField.setLabel("Is Draft");
		isDraftField.setDataType(new BooleanDataType());
		isDraftField.setDbColumn("isdraft");
		isDraftField.setRequired(true);
		this.addField(isDraftField);
		
		Field isCallableField = new Field();
		isCallableField.setApiName("isCallable");
		isCallableField.setLabel("Is Callable");
		isCallableField.setDataType(new BooleanDataType());
		isCallableField.setDbColumn("iscallable");
		isCallableField.setRequired(true);
		this.addField(isCallableField);
		
		Field isTriggerableField = new Field();
		isTriggerableField.setApiName("isTriggerable");
		isTriggerableField.setLabel("Is Triggerable");
		isTriggerableField.setDataType(new BooleanDataType());
		isTriggerableField.setDbColumn("istriggerable");
		isTriggerableField.setRequired(true);
		this.addField(isTriggerableField);
		
		/*Field invocationTypeField = new Field();
		invocationTypeField.setApiName("invocationType");
		invocationTypeField.setLabel("Invocation Type");
		
		EnumerationDataType itDT = new EnumerationDataType("Triggerable\nCallable");
		itDT.setValidateValues(true);
		invocationTypeField.setDataType(itDT);
		invocationTypeField.setDbColumn("invocationtype");
		invocationTypeField.setRequired(true);
		this.addField(invocationTypeField);*/
	}
}

