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

public class LayoutKType extends Type
{
	private static final long serialVersionUID = -5189189265107123906L;
	private static final String LABEL = "Layout";
	private static final String PLURAL_LABEL = "Layouts";
	
	public LayoutKType() throws KommetException
	{
		super();
		this.setApiName(SystemTypes.LAYOUT_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.LAYOUT_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.LAYOUT_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add name field
		Field nameField = new Field();
		nameField.setApiName("name");
		nameField.setLabel("Name");
		nameField.setDataType(new TextDataType(50));
		nameField.setDbColumn("name");
		nameField.setRequired(true);
		this.addField(nameField);
		
		Field codeField = new Field();
		codeField.setApiName("code");
		codeField.setLabel("Code");
		codeField.setDataType(new TextDataType(32000));
		codeField.setDbColumn("code");
		codeField.setRequired(false);
		this.addField(codeField);
	}

}
