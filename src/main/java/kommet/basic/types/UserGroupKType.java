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
import kommet.utils.AppConfig;

public class UserGroupKType extends Type
{
	private static final long serialVersionUID = 4990837953065665136L;
	private static final String LABEL = "User Group";
	private static final String PLURAL_LABEL = "User Groups";
	
	public UserGroupKType() throws KommetException
	{
		super();
		
		this.setApiName(SystemTypes.USER_GROUP_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.USER_GROUP_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.USER_GROUP_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		Field nameField = new Field();
		nameField.setApiName("name");
		nameField.setLabel("Name");
		nameField.setDataType(new TextDataType(255));
		nameField.setDbColumn("name");
		nameField.setRequired(true);
		this.addField(nameField);
		
		Field descField = new Field();
		descField.setApiName("description");
		descField.setLabel("Description");
		descField.setDataType(new TextDataType(255));
		descField.setDbColumn("description");
		descField.setRequired(false);
		this.addField(descField);
	}

}
