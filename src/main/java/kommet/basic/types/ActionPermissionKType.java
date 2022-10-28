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
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class ActionPermissionKType extends PermissionKType
{
	private static final long serialVersionUID = 8201249604823391473L;
	private static final String LABEL = "Action Permission";
	private static final String PLURAL_LABEL = "Action Permissions";
	
	public ActionPermissionKType()
	{
		super();
	}
	
	public ActionPermissionKType(ProfileKType profileType, PermissionSetKType permissionSetType, ActionKType actionType) throws KommetException
	{
		super(profileType, permissionSetType);
		this.setApiName(SystemTypes.ACTION_PERMISSION_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.ACTION_PERMISSION_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.ACTION_PERMISSION_ID_SEQ));
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
		
		// add reference to action
		Field actionField = new Field();
		actionField.setApiName("action");
		actionField.setLabel("Action");
		actionField.setDataType(new TypeReference(actionType));
		actionField.setDbColumn("action");
		actionField.setRequired(true);
		this.addField(actionField);
	}
}
