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
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class FileRecordAssignmentKType extends Type
{
	private static final long serialVersionUID = -2424406836587393854L;
	private static final String LABEL = "File Record Assignment";
	private static final String PLURAL_LABEL = "File Record Assignments";
	
	public FileRecordAssignmentKType ()
	{
		super();
	}
	
	public FileRecordAssignmentKType (FileKType fileType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.FILE_RECORD_ASSIGNMENT_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.FILE_RECORD_ASSIGNMENT_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.FILE_OBJECT_ASSIGNMENT_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add file field
		Field fileField = new Field();
		fileField.setApiName("file");
		fileField.setLabel("File");
		fileField.setDataType(new TypeReference(fileType));
		fileField.setDbColumn("file");
		fileField.setRequired(true);
		this.addField(fileField);
		
		// add object id field
		Field recordIdField = new Field();
		recordIdField.setApiName("recordId");
		recordIdField.setLabel("Record ID");
		recordIdField.setDataType(new KIDDataType());
		recordIdField.setDbColumn("recordid");
		recordIdField.setRequired(true);
		this.addField(recordIdField);
		
		// add comment field
		Field commentField = new Field();
		commentField.setApiName("comment");
		commentField.setLabel("Comment");
		commentField.setDataType(new TextDataType(255));
		commentField.setDbColumn("comment");
		commentField.setRequired(false);
		this.addField(commentField);
	}

}
