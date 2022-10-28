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

public class ViewResourceKType extends Type
{	
	private static final long serialVersionUID = -8095740925160300468L;
	private static final String LABEL = "View Resource";
	private static final String PLURAL_LABEL = "View Resources";
	
	public ViewResourceKType() throws KommetException
	{
		super();
		this.setApiName(SystemTypes.VIEW_RESOURCE_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.VIEW_RESOURCE_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.VIEW_RESOURCE_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add name field
		Field nameField = new Field();
		nameField.setApiName("name");
		nameField.setLabel("Name");
		nameField.setDataType(new TextDataType(100));
		nameField.setDbColumn("name");
		nameField.setRequired(true);
		this.addField(nameField);
		
		// add koll code field
		Field contentField = new Field();
		contentField.setApiName("content");
		contentField.setLabel("Content");
		contentField.setDataType(new TextDataType(128000));
		contentField.setDbColumn("content");
		contentField.setRequired(false);
		this.addField(contentField);
		
		Field pathField = new Field();
		pathField.setApiName("path");
		pathField.setLabel("Path");
		pathField.setDataType(new TextDataType(255));
		pathField.setDbColumn("path");
		pathField.setRequired(true);
		this.addField(pathField);
		
		// add koll code field
		Field mimeTypeField = new Field();
		mimeTypeField.setApiName("mimeType");
		mimeTypeField.setLabel("MIME Type");
		mimeTypeField.setDataType(new TextDataType(30));
		mimeTypeField.setDbColumn("mimetype");
		mimeTypeField.setRequired(true);
		this.addField(mimeTypeField);
	}
}
