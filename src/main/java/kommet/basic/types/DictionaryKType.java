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

public class DictionaryKType extends Type
{
	private static final long serialVersionUID = 4304512443130986669L;
	private static final String LABEL = "Dictionary";
	private static final String PLURAL_LABEL = "Dictionaries";
	
	public DictionaryKType() throws KommetException
	{
		super();
		this.setApiName(SystemTypes.DICTIONARY_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.DICTIONARY_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.DICTIONARY_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		Field fieldField = new Field();
		fieldField.setApiName("name");
		fieldField.setLabel("Name");
		fieldField.setDataType(new TextDataType(255));
		fieldField.setDbColumn("name");
		fieldField.setRequired(true);
		this.addField(fieldField);
	}

}
