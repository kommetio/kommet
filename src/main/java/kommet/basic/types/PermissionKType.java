/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.types;

import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.TypeReference;

public abstract class PermissionKType extends Type
{
	private static final long serialVersionUID = -5710547147201896828L;
	
	public PermissionKType()
	{
		super();
	}

	public PermissionKType (ProfileKType profileType, PermissionSetKType permissionSetType) throws KommetException
	{
		// add reference to profile
		Field profileRef = new Field();
		profileRef.setApiName("profile");
		profileRef.setLabel("Profile");
		
		TypeReference profileRefDT = new TypeReference(profileType);
		profileRefDT.setCascadeDelete(true);
		
		profileRef.setDataType(profileRefDT);
		profileRef.setDbColumn("profile");
		profileRef.setRequired(false);
		this.addField(profileRef);
		
		// add reference to permission set
		Field permissionSetRef = new Field();
		permissionSetRef.setApiName("permissionSet");
		permissionSetRef.setLabel("Permission Set");
		permissionSetRef.setDataType(new TypeReference(permissionSetType));
		permissionSetRef.setDbColumn("permissionset");
		permissionSetRef.setRequired(false);
		this.addField(permissionSetRef);
	}
}
