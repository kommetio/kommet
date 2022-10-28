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
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class UserSettingsKType extends Type
{
	private static final long serialVersionUID = -8172996954532597596L;
	private static final String LABEL = "User Settings";
	private static final String PLURAL_LABEL = "User Settings";
	
	public UserSettingsKType() throws KommetException
	{
		super();
	}
	
	public UserSettingsKType(Type userType, Type profileType, Type layoutType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.USER_SETTINGS_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.USER_SETTINGS_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.USER_SETTINGS_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add profile field
		Field profileField = new Field();
		profileField.setApiName("profile");
		profileField.setLabel("Profile");
		profileField.setDataType(new TypeReference(profileType));
		profileField.setDbColumn("profile");
		profileField.setRequired(false);
		this.addField(profileField);
		
		// add user field
		Field userField = new Field();
		userField.setApiName("user");
		userField.setLabel("User");
		userField.setDataType(new TypeReference(userType));
		userField.setDbColumn("assigneduser");
		userField.setRequired(false);
		this.addField(userField);
		
		// add layout field
		Field layoutField = new Field();
		layoutField.setApiName("layout");
		layoutField.setLabel("Layout");
		layoutField.setDataType(new TypeReference(layoutType));
		layoutField.setDbColumn("layout");
		layoutField.setRequired(false);
		this.addField(layoutField);
		
		// add landing URL field
		Field landingUrlField = new Field();
		landingUrlField.setApiName("landingURL");
		landingUrlField.setLabel("Landing URL");
		landingUrlField.setDataType(new TextDataType(100));
		landingUrlField.setDbColumn("landingurl");
		landingUrlField.setRequired(false);
		this.addField(landingUrlField);
	}

}
