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

public class TextLabelKType extends Type
{
	private static final long serialVersionUID = 1165371647823028573L;
	private static final String LABEL = "Text Label";
	private static final String PLURAL_LABEL = "Text Labels";
	
	public TextLabelKType() throws KommetException
	{
		super();
		this.setApiName(SystemTypes.TEXT_LABEL_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.TEXT_LABEL_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.TEXT_LABEL_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add key field
		Field keyField = new Field();
		keyField.setApiName("key");
		keyField.setLabel("Key");
		keyField.setDataType(new TextDataType(100));
		keyField.setDbColumn("key");
		keyField.setRequired(true);
		this.addField(keyField);
		
		// add value field
		Field valField = new Field();
		valField.setApiName("value");
		valField.setLabel("Value");
		valField.setDataType(new TextDataType(500));
		valField.setDbColumn("value");
		valField.setRequired(true);
		this.addField(valField);
		
		// add locale field
		Field localeField = new Field();
		localeField.setApiName("locale");
		localeField.setLabel("Locale");
		localeField.setDataType(new TextDataType(5));
		localeField.setDbColumn("locale");
		this.addField(localeField);
	}
}
