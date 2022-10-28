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

public class SystemSettingKType extends Type
{
	private static final long serialVersionUID = -7262876235226596748L;
	private static final String LABEL = "Setting";
	private static final String PLURAL_LABEL = "Settings";
	
	public SystemSettingKType() throws KommetException
	{
		super();
		this.setApiName(SystemTypes.SYSTEM_SETTING_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.SYSTEM_SETTING_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.SYSTEM_SETTING_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add key field
		Field keyField = new Field();
		keyField.setApiName("key");
		keyField.setLabel("Key");
		keyField.setDataType(new TextDataType(50));
		keyField.setDbColumn("key");
		keyField.setRequired(true);
		this.addField(keyField);
		
		// add value field
		Field valueField = new Field();
		valueField.setApiName("value");
		valueField.setLabel("Value");
		valueField.setDataType(new TextDataType(255));
		valueField.setDbColumn("value");
		valueField.setRequired(false);
		this.addField(valueField);
	}

}
