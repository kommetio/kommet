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
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class WebResourceKType extends Type
{	
	private static final long serialVersionUID = -8990693080125916778L;
	private static final String LABEL = "Web Resource";
	private static final String PLURAL_LABEL = "Web Resources";
	
	public WebResourceKType()
	{
		super();
	}
	
	public WebResourceKType(Type fileType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.WEB_RESOURCE_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.WEB_RESOURCE_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.WEB_RESOURCE_ID_SEQ));
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
		
		// add file field
		Field mimeTypeField = new Field();
		mimeTypeField.setApiName("mimeType");
		mimeTypeField.setLabel("MIME Type");
		mimeTypeField.setDataType(new TextDataType(30));
		mimeTypeField.setDbColumn("mimetype");
		mimeTypeField.setRequired(true);
		this.addField(mimeTypeField);
		
		// add file field
		Field nameField = new Field();
		nameField.setApiName("name");
		nameField.setLabel("Name");
		nameField.setDataType(new TextDataType(30));
		nameField.setDbColumn("name");
		nameField.setRequired(true);
		this.addField(nameField);
	}
}
