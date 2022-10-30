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
import kommet.data.datatypes.BooleanDataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.TextDataType;
import kommet.utils.AppConfig;

public class ClassKType extends Type
{	
	private static final long serialVersionUID = 5495882192417830423L;
	private static final String LABEL = "Class";
	private static final String PLURAL_LABEL = "Classes";
	
	public ClassKType() throws KommetException
	{
		super();
		this.setApiName(SystemTypes.CLASS_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.CLASS_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.CLASS_ID_SEQ));
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
		
		// add package field
		Field packageField = new Field();
		packageField.setApiName("packageName");
		packageField.setLabel("Package Name");
		packageField.setDataType(new TextDataType(150));
		packageField.setDbColumn("packagename");
		packageField.setRequired(true);
		this.addField(packageField);
		
		// add koll code field
		Field codeField = new Field();
		codeField.setApiName("kollCode");
		codeField.setLabel("Koll Code");
		codeField.setDataType(new TextDataType(200000));
		codeField.setDbColumn("kollcode");
		codeField.setRequired(true);
		this.addField(codeField);
		
		// add java code field
		Field javaCodeField = new Field();
		javaCodeField.setApiName("javaCode");
		javaCodeField.setLabel("Java Code");
		javaCodeField.setDataType(new TextDataType(200000));
		javaCodeField.setDbColumn("javacode");
		javaCodeField.setRequired(true);
		this.addField(javaCodeField);
		
		Field systemField = new Field();
		systemField.setApiName("isSystem");
		systemField.setLabel("Is System");
		systemField.setDataType(new BooleanDataType());
		systemField.setDbColumn("issystem");
		systemField.setRequired(true);
		this.addField(systemField);
		
		Field accessField = new Field();
		accessField.setApiName("accessLevel");
		accessField.setLabel("Access Level");
		
		EnumerationDataType enumDT = new EnumerationDataType("Editable\nRead-only\nRead-only methods\nClosed");
		enumDT.setValidateValues(true);
		
		accessField.setDataType(enumDT);
		accessField.setDbColumn("accesslevel");
		accessField.setRequired(true);
		this.addField(accessField);
		
		Field draftField = new Field();
		draftField.setApiName("isDraft");
		draftField.setLabel("Is Draft");
		draftField.setDataType(new BooleanDataType());
		draftField.setDbColumn("isdraft");
		draftField.setRequired(true);
		draftField.setDefaultValue("false");
		this.addField(draftField);
	}
}
