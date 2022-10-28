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

public class FileRevisionKType extends Type
{	
	private static final long serialVersionUID = 5875897083449186564L;
	private static final String LABEL = "File Revision";
	private static final String PLURAL_LABEL = "File Revisions";
	
	public FileRevisionKType()
	{
		super();
	}
	
	public FileRevisionKType(Type fileType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.FILE_REVISION_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.FILE_REVISION_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.FILE_REVISION_ID_SEQ));
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
		
		// add revision number field
		Field revNumField = new Field();
		revNumField.setApiName("revisionNumber");
		revNumField.setLabel("Revision Number");
		revNumField.setDataType(new NumberDataType(0, Integer.class));
		revNumField.setDbColumn("revisionNumber");
		revNumField.setRequired(true);
		this.addField(revNumField);
		
		// add size field
		Field sizeField = new Field();
		sizeField.setApiName("size");
		sizeField.setLabel("Size");
		sizeField.setDataType(new NumberDataType(0, Integer.class));
		sizeField.setDbColumn("size");
		sizeField.setRequired(true);
		this.addField(sizeField);
		
		// add file field
		Field fileField = new Field();
		fileField.setApiName("file");
		fileField.setLabel("File");
		fileField.setDataType(new TypeReference(fileType));
		fileField.setDbColumn("file");
		fileField.setRequired(true);
		this.addField(fileField);
		
		// add path field
		Field pathField = new Field();
		pathField.setApiName("path");
		pathField.setLabel("Path");
		pathField.setDataType(new TextDataType(255));
		pathField.setDbColumn("path");
		pathField.setRequired(true);
		this.addField(pathField);
	}

}
