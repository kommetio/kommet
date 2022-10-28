/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.types;

import kommet.data.Field;
import kommet.data.FieldValidationException;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.BooleanDataType;
import kommet.data.datatypes.TextDataType;
import kommet.utils.AppConfig;

public class PermissionSetKType extends Type
{
	private static final long serialVersionUID = -4224319765974999660L;
	
	private static final String LABEL = "Permission Set";
	private static final String PLURAL_LABEL = "Permission Sets";
	
	public PermissionSetKType() throws KommetException
	{
		super();
		this.setApiName(SystemTypes.PERMISSION_SET_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.PERMISSION_SET_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.PERMISSION_SET_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add name field
		Field nameField = new Field();
		nameField.setApiName("name");
		nameField.setLabel("Name");
		nameField.setDataType(new TextDataType(30));
		nameField.setDbColumn("name");
		nameField.setRequired(true);
		this.addField(nameField);
		
		// add "is system" column that tells if the permission set was created by the system (not manually by a user)
		Field systemField = new Field();
		systemField.setApiName("systemPermissionSet");
		systemField.setLabel("Is System Permission Set");
		systemField.setDataType(new BooleanDataType());
		systemField.setDbColumn("systempermissionset");
		systemField.setRequired(true);
		this.addField(systemField);
	}

	/**
	 * Checks if the given KID is a valid KGroup ID.
	 * @param userId
	 * @throws FieldValidationException
	 */
	public static void validatePermissionSetId (KID groupId) throws FieldValidationException
	{
		if (!groupId.getId().startsWith(KID.PERMISSION_SET_PREFIX))
		{
			throw new FieldValidationException("KID must be an ID of a permission set in the current context. It should start with " + KID.PERMISSION_SET_PREFIX + ". The actual value is " + groupId);
		}
	}
}
