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
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class TypeTriggerKType extends Type
{
	private static final long serialVersionUID = -6346906311125621748L;
	private static final String LABEL = "Type Trigger";
	private static final String PLURAL_LABEL = "Type Triggers";
	
	public TypeTriggerKType() throws KommetException
	{
		super();
	}
	
	public TypeTriggerKType(Type classType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.TYPE_TRIGGER_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.TYPE_TRIGGER_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.TYPE_TRIGGER_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add type field
		Field typeIdField = new Field();
		typeIdField.setApiName("typeId");
		typeIdField.setDbColumn("typeid");
		typeIdField.setLabel("Type ID");
		typeIdField.setDataType(new KIDDataType());
		typeIdField.setRequired(true);
		this.addField(typeIdField);
		
		// add koll file reference
		Field classField = new Field();
		classField.setApiName("triggerFile");
		classField.setDbColumn("triggerfile");
		classField.setLabel("Trigger File");
		TypeReference dt = new TypeReference(classType);
		// when file is deleted, the trigger type association should also be deleted
		// TODO write tests for this
		dt.setCascadeDelete(true);
		classField.setDataType(dt);
		classField.setRequired(true);
		this.addField(classField);
		
		Field systemField = new Field();
		systemField.setApiName("isSystem");
		systemField.setLabel("Is System");
		systemField.setDataType(new BooleanDataType());
		systemField.setDbColumn("issystem");
		systemField.setRequired(true);
		this.addField(systemField);
		
		Field activeField = new Field();
		activeField.setApiName("isActive");
		activeField.setLabel("Is Active");
		activeField.setDataType(new BooleanDataType());
		activeField.setDbColumn("isactive");
		activeField.setRequired(true);
		this.addField(activeField);
		
		Field beforeInsertField = new Field();
		beforeInsertField.setApiName("isBeforeInsert");
		beforeInsertField.setLabel("Is Before Insert");
		beforeInsertField.setDataType(new BooleanDataType());
		beforeInsertField.setDbColumn("isbeforeinsert");
		beforeInsertField.setRequired(true);
		this.addField(beforeInsertField);
		
		Field beforeUpdateField = new Field();
		beforeUpdateField.setApiName("isBeforeUpdate");
		beforeUpdateField.setLabel("Is Before Update");
		beforeUpdateField.setDataType(new BooleanDataType());
		beforeUpdateField.setDbColumn("isbeforeupdate");
		beforeUpdateField.setRequired(true);
		this.addField(beforeUpdateField);
		
		Field beforeDeleteField = new Field();
		beforeDeleteField.setApiName("isBeforeDelete");
		beforeDeleteField.setLabel("Is Before Delete");
		beforeDeleteField.setDataType(new BooleanDataType());
		beforeDeleteField.setDbColumn("isbeforedelete");
		beforeDeleteField.setRequired(true);
		this.addField(beforeDeleteField);
		
		Field afterInsertField = new Field();
		afterInsertField.setApiName("isAfterInsert");
		afterInsertField.setLabel("Is After Insert");
		afterInsertField.setDataType(new BooleanDataType());
		afterInsertField.setDbColumn("isafterinsert");
		afterInsertField.setRequired(true);
		this.addField(afterInsertField);
		
		Field afterUpdateField = new Field();
		afterUpdateField.setApiName("isAfterUpdate");
		afterUpdateField.setLabel("Is After Update");
		afterUpdateField.setDataType(new BooleanDataType());
		afterUpdateField.setDbColumn("isafterupdate");
		afterUpdateField.setRequired(true);
		this.addField(afterUpdateField);
		
		Field afterDeleteField = new Field();
		afterDeleteField.setApiName("isAfterDelete");
		afterDeleteField.setLabel("Is After Delete");
		afterDeleteField.setDataType(new BooleanDataType());
		afterDeleteField.setDbColumn("isafterdelete");
		afterDeleteField.setRequired(true);
		this.addField(afterDeleteField);
	}

}
