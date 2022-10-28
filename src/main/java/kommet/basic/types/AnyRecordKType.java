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
import kommet.data.datatypes.KIDDataType;
import kommet.utils.AppConfig;

public class AnyRecordKType extends Type
{
	private static final long serialVersionUID = -7103705271229433970L;
	private static final String LABEL = "Any Record";
	private static final String PLURAL_LABEL = "Any Records";
	
	public AnyRecordKType() throws KommetException
	{
		super();
		this.setApiName(SystemTypes.ANY_RECORD_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.ANY_RECORD_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.ANY_RECORD_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add name field
		Field nameField = new Field();
		nameField.setApiName("recordId");
		nameField.setLabel("Record ID");
		nameField.setDataType(new KIDDataType());
		nameField.setDbColumn("recordid");
		nameField.setRequired(true);
		this.addField(nameField);
	}
}

