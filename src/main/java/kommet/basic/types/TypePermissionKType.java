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
import kommet.data.datatypes.BooleanDataType;
import kommet.data.datatypes.KIDDataType;
import kommet.utils.AppConfig;

public class TypePermissionKType extends PermissionKType
{	
	private static final long serialVersionUID = -890500362317631171L;
	private static final String LABEL = "Type Permission";
	private static final String PLURAL_LABEL = "Type Permissions";
	
	public TypePermissionKType()
	{
		super();
	}
	
	public TypePermissionKType(ProfileKType profileType, PermissionSetKType permissionSetObj) throws KommetException
	{
		super(profileType, permissionSetObj);
		this.setApiName(SystemTypes.TYPE_PERMISSION_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.TYPE_PERMISSION_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.TYPE_PERMISSION_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add read field
		Field readField = new Field();
		readField.setApiName("read");
		readField.setLabel("Read");
		readField.setDataType(new BooleanDataType());
		readField.setDbColumn("read");
		readField.setRequired(true);
		this.addField(readField);
		
		// add write field
		Field editField = new Field();
		editField.setApiName("edit");
		editField.setLabel("Edit");
		editField.setDataType(new BooleanDataType());
		editField.setDbColumn("edit");
		editField.setRequired(true);
		this.addField(editField);
		
		// add delete field
		Field deleteField = new Field();
		deleteField.setApiName("delete");
		deleteField.setLabel("Delete");
		deleteField.setDataType(new BooleanDataType());
		deleteField.setDbColumn("delete");
		deleteField.setRequired(true);
		this.addField(deleteField);
		
		// add view all field
		Field readAllField = new Field();
		readAllField.setApiName("readAll");
		readAllField.setLabel("Read All");
		readAllField.setDataType(new BooleanDataType());
		readAllField.setDbColumn("readall");
		readAllField.setRequired(true);
		this.addField(readAllField);
		
		// add view all field
		Field editAllField = new Field();
		editAllField.setApiName("editAll");
		editAllField.setLabel("Edit All");
		editAllField.setDataType(new BooleanDataType());
		editAllField.setDbColumn("editall");
		editAllField.setRequired(true);
		this.addField(editAllField);
		
		Field deleteAllField = new Field();
		deleteAllField.setApiName("deleteAll");
		deleteAllField.setLabel("Delete All");
		deleteAllField.setDataType(new BooleanDataType());
		deleteAllField.setDbColumn("deleteall");
		deleteAllField.setRequired(true);
		this.addField(deleteAllField);
		
		// add create field
		Field createField = new Field();
		createField.setApiName("create");
		createField.setLabel("Create");
		createField.setDataType(new BooleanDataType());
		// Use column name "new" because "create" is a reserved keyword
		createField.setDbColumn("new");
		createField.setRequired(true);
		this.addField(createField);
		
		// add reference to type
		Field typeRef = new Field();
		typeRef.setApiName("typeId");
		typeRef.setDbColumn("typeid");
		typeRef.setLabel("Type ID");
		typeRef.setDataType(new KIDDataType());
		typeRef.setRequired(true);
		this.addField(typeRef);
	}
}
