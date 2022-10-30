/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.types;

import kommet.basic.File;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.BooleanDataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.TextDataType;
import kommet.utils.AppConfig;

public class FileKType extends Type
{	
	private static final long serialVersionUID = 8592177537299274211L;
	private static final String LABEL = "File";
	private static final String PLURAL_LABEL = "Files";
	
	public FileKType() throws KommetException
	{
		super();
		this.setApiName(SystemTypes.FILE_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.FILE_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.FILE_ID_SEQ));
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
		
		// add sealed field
		Field sealedField = new Field();
		sealedField.setApiName("sealed");
		sealedField.setLabel("Sealed");
		sealedField.setDataType(new BooleanDataType());
		sealedField.setDbColumn("sealed");
		sealedField.setRequired(false);
		this.addField(sealedField);
		
		// add access field
		Field accessField = new Field();
		accessField.setApiName("access");
		accessField.setLabel("Access");
		accessField.setDataType(new EnumerationDataType(File.PUBLIC_ACCESS + "\n" + File.RESTRICTED_ACCESS));
		accessField.setDbColumn("access");
		accessField.setRequired(true);
		this.addField(accessField);
	}

}
