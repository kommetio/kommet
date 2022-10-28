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

public class LabelKType extends Type
{
	private static final long serialVersionUID = -9024825770162646483L;
	private static final String LABEL = "Label";
	private static final String PLURAL_LABEL = "Labels";
	
	public LabelKType() throws KommetException
	{
		super();
		this.setApiName(SystemTypes.LABEL_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.LABEL_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.LABEL_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add name field
		Field nameField = new Field();
		nameField.setApiName("text");
		nameField.setLabel("Text");
		nameField.setDataType(new TextDataType(255));
		nameField.setDbColumn("labeltext");
		nameField.setRequired(true);
		this.addField(nameField);
	}
}

