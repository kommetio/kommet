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
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class DictionaryItemKType extends Type
{
	private static final long serialVersionUID = 4304512443130986669L;
	private static final String LABEL = "Dictionary Item";
	private static final String PLURAL_LABEL = "Dictionary Items";
	
	public DictionaryItemKType() throws KommetException
	{
		super();
	}
	
	public DictionaryItemKType(Type dictionaryType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.DICTIONARY_ITEM_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.DICTIONARY_ITEM_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.DICTIONARY_ITEM_ID_SEQ));
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
		
		Field keyField = new Field();
		keyField.setApiName("key");
		keyField.setLabel("Key");
		keyField.setDataType(new TextDataType(255));
		keyField.setDbColumn("key");
		keyField.setRequired(false);
		this.addField(keyField);
		
		Field orderield = new Field();
		orderield.setApiName("index");
		orderield.setLabel("Index");
		orderield.setDataType(new NumberDataType(0, java.lang.Integer.class));
		orderield.setDbColumn("itemindex");
		orderield.setRequired(true);
		this.addField(orderield);
		
		Field dictField = new Field();
		dictField.setApiName("dictionary");
		dictField.setLabel("Dictionary");
		
		TypeReference ref = new TypeReference(dictionaryType);
		ref.setCascadeDelete(true);
		
		dictField.setDataType(ref);
		dictField.setDbColumn("dictionary");
		dictField.setRequired(true);
		this.addField(dictField);
	}

}

