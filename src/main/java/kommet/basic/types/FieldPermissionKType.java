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

public class FieldPermissionKType extends PermissionKType
{	
	private static final long serialVersionUID = 5882061671585716687L;
	private static final String LABEL = "Field Permission";
	private static final String PLURAL_LABEL = "Field Permissions";
	
	public FieldPermissionKType()
	{
		super();
	}
	
	public FieldPermissionKType(ProfileKType profileObj, PermissionSetKType permissionSetObj) throws KommetException
	{
		super(profileObj, permissionSetObj);
		this.setApiName(SystemTypes.FIELD_PERMISSION_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.FIELD_PERMISSION_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.FIELD_PERMISSION_ID_SEQ));
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
		
		// add reference to type
		Field fieldRef = new Field();
		fieldRef.setApiName("fieldId");
		fieldRef.setLabel("Field ID");
		fieldRef.setDataType(new KIDDataType());
		fieldRef.setRequired(true);
		this.addField(fieldRef);
	}
}
