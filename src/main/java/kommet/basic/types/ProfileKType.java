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
import kommet.utils.AppConfig;

public class ProfileKType extends Type
{	
	private static final long serialVersionUID = -9086729074753121265L;
	private static final String LABEL = "Profile";
	private static final String PLURAL_LABEL = "Profiles";
	
	public ProfileKType() throws KommetException
	{
		super();
		this.setApiName(SystemTypes.PROFILE_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.PROFILE_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.PROFILE_ID_SEQ));
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
		
		Field labelField = new Field();
		labelField.setApiName("label");
		labelField.setLabel("Label");
		labelField.setDataType(new TextDataType(30));
		labelField.setDbColumn("label");
		labelField.setRequired(true);
		this.addField(labelField);
		
		// add "is system" column that tells if the profile was created by the system (not manually by a user)
		Field systemField = new Field();
		systemField.setApiName("systemProfile");
		systemField.setLabel("Is System Profile");
		systemField.setDataType(new BooleanDataType());
		systemField.setDbColumn("systemprofile");
		systemField.setRequired(true);
		this.addField(systemField);
		
		// type reference to default layout will be added after layout type is created,
		// because when profile type is created, layout type does not exist yet
	}
}
