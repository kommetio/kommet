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

public class EnvKType extends Type
{
	private static final long serialVersionUID = -392114579344532253L;
	private static final String LABEL = "Environment";
	private static final String PLURAL_LABEL = "Environments";
	
	public EnvKType() throws KommetException
	{
		super();
		this.setApiName(SystemTypes.ENV_API_NAME);
		//this.setDbTable(DB_TABLE);
		this.setKeyPrefix(KeyPrefix.get(KID.ENV_PREFIX));
		this.setKID(KID.get(KID.ENV_PREFIX, SystemTypes.ENV_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add name
		Field nameField = new Field();
		nameField.setApiName("name");
		nameField.setLabel("Name");
		nameField.setDataType(new TextDataType(20));
		nameField.setDbColumn("name");
		nameField.setRequired(true);
		this.addField(nameField);
	}
}
