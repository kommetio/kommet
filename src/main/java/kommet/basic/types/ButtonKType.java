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

public class ButtonKType extends Type
{
	private static final long serialVersionUID = -7175676990522350772L;
	private static final String LABEL = "Button";
	private static final String PLURAL_LABEL = "Buttons";
	
	public ButtonKType() throws KommetException
	{
		super();
	}
	
	public ButtonKType(Type actionType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.BUTTON_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.BUTTON_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.BUTTON_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add content field
		Field nameField = new Field();
		nameField.setApiName("name");
		nameField.setLabel("Name");
		nameField.setDataType(new TextDataType(500));
		nameField.setDbColumn("name");
		nameField.setRequired(true);
		this.addField(nameField);
		
		// add URL field
		Field urlField = new Field();
		urlField.setApiName("url");
		urlField.setLabel("URL");
		urlField.setDataType(new TextDataType(10000));
		urlField.setDbColumn("url");
		urlField.setRequired(false);
		this.addField(urlField);
		
		// add URL field
		Field onClickField = new Field();
		onClickField.setApiName("onClick");
		onClickField.setLabel("On Click");
		onClickField.setDataType(new TextDataType(10000));
		onClickField.setDbColumn("onclick");
		onClickField.setRequired(false);
		this.addField(onClickField);
		
		// add URL field
		Field labelField = new Field();
		labelField.setApiName("label");
		labelField.setLabel("Label");
		labelField.setDataType(new TextDataType(100));
		labelField.setDbColumn("label");
		labelField.setRequired(true);
		this.addField(labelField);
		
		// add URL field
		Field labelKeyField = new Field();
		labelKeyField.setApiName("labelKey");
		labelKeyField.setLabel("Label Key");
		labelKeyField.setDataType(new TextDataType(255));
		labelKeyField.setDbColumn("labelkey");
		labelKeyField.setRequired(false);
		this.addField(labelKeyField);
		
		Field conditionField = new Field();
		conditionField.setApiName("displayCondition");
		conditionField.setLabel("Display Condition");
		conditionField.setDataType(new TextDataType(10000));
		conditionField.setDbColumn("displaycondition");
		conditionField.setRequired(false);
		this.addField(conditionField);
		
		// add type ID field
		Field typeIdField = new Field();
		typeIdField.setApiName("typeId");
		typeIdField.setLabel("Type ID");
		typeIdField.setDataType(new KIDDataType());
		typeIdField.setDbColumn("typeid");
		typeIdField.setRequired(true);
		this.addField(typeIdField);
		
		// add reference to action
		Field actionRef = new Field();
		actionRef.setApiName("action");
		actionRef.setLabel("Action");
		actionRef.setDataType(new TypeReference(actionType));
		actionRef.setDbColumn("action");
		actionRef.setRequired(false);
		this.addField(actionRef);
	}
}
