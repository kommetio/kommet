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
import kommet.utils.AppConfig;

public class CommentKType extends Type
{
	private static final long serialVersionUID = 4171861058433029892L;
	private static final String LABEL = "Comment";
	private static final String PLURAL_LABEL = "Comments";
	
	public CommentKType()
	{
		super();
	}
	
	public CommentKType(Type userType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.COMMENT_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.COMMENT_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.COMMENT_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add content field
		Field contentField = new Field();
		contentField.setApiName("content");
		contentField.setLabel("Content");
		contentField.setDataType(new TextDataType(500));
		contentField.setDbColumn("content");
		contentField.setRequired(true);
		this.addField(contentField);
		
		// add record id field
		Field recordIdField = new Field();
		recordIdField.setApiName("recordId");
		recordIdField.setLabel("Record ID");
		recordIdField.setDataType(new KIDDataType());
		recordIdField.setDbColumn("recordid");
		recordIdField.setRequired(true);
		this.addField(recordIdField);
	}
}
