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
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.KIDDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class StandardActionKType extends Type
{	
	private static final long serialVersionUID = -4245609764240946235L;
	private static final String LABEL = "Standard Action";
	private static final String PLURAL_LABEL = "Standard Actions";
	
	public StandardActionKType()
	{
		super();
	}
	
	public StandardActionKType(Type profileType, Type actionType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.STANDARD_ACTION_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.STANDARD_ACTION_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.STANDARD_ACTION_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add type field
		Field typeField = new Field();
		typeField.setApiName("type");
		typeField.setLabel("Type");
		typeField.setDataType(new EnumerationDataType("View\nList\nEdit"));
		typeField.setDbColumn("type");
		typeField.setRequired(true);
		this.addField(typeField);
		
		// add reference to profile
		Field profileRef = new Field();
		profileRef.setApiName("profile");
		profileRef.setLabel("Profile");
		profileRef.setDataType(new TypeReference(profileType));
		profileRef.setDbColumn("profile");
		profileRef.setRequired(true);
		this.addField(profileRef);
		
		// add reference to action
		Field actionRef = new Field();
		actionRef.setApiName("action");
		actionRef.setLabel("Action");
		actionRef.setDataType(new TypeReference(actionType));
		actionRef.setDbColumn("action");
		actionRef.setRequired(true);
		this.addField(actionRef);
		
		// add type field
		Field ktypeField = new Field();
		ktypeField.setApiName("typeId");
		ktypeField.setLabel("Type ID");
		ktypeField.setDataType(new KIDDataType());
		ktypeField.setDbColumn("typeid");
		ktypeField.setRequired(true);
		this.addField(ktypeField);
	}
}
