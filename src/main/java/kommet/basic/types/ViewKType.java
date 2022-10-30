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
import kommet.data.datatypes.KIDDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class ViewKType extends Type
{	
	private static final long serialVersionUID = 5495882192417830423L;
	private static final String LABEL = "View";
	private static final String PLURAL_LABEL = "Views";
	
	public ViewKType() throws KommetException
	{
		super();
	}
	
	public ViewKType(Type layoutType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.VIEW_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.VIEW_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.VIEW_ID_SEQ));
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
		
		// add name field
		Field pathField = new Field();
		pathField.setApiName("path");
		pathField.setLabel("path");
		pathField.setDataType(new TextDataType(50));
		pathField.setDbColumn("path");
		pathField.setRequired(true);
		this.addField(pathField);
		
		// add package field
		Field packageField = new Field();
		packageField.setApiName("packageName");
		packageField.setLabel("Package Name");
		packageField.setDataType(new TextDataType(150));
		packageField.setDbColumn("packagename");
		packageField.setRequired(true);
		this.addField(packageField);
		
		// add contents field
		Field keetleCodeField = new Field();
		keetleCodeField.setApiName("keetleCode");
		keetleCodeField.setLabel("Keetle Code");
		keetleCodeField.setDataType(new TextDataType(128000));
		keetleCodeField.setDbColumn("keetlecode");
		keetleCodeField.setRequired(true);
		this.addField(keetleCodeField);
		
		// add contents field
		Field jspCodeField = new Field();
		jspCodeField.setApiName("jspCode");
		jspCodeField.setLabel("JSP Code");
		jspCodeField.setDataType(new TextDataType(128000));
		jspCodeField.setDbColumn("jspcode");
		jspCodeField.setRequired(true);
		this.addField(jspCodeField);
		
		Field systemField = new Field();
		systemField.setApiName("isSystem");
		systemField.setLabel("Is System");
		systemField.setDataType(new BooleanDataType());
		systemField.setDbColumn("issystem");
		systemField.setRequired(true);
		this.addField(systemField);
		
		// Add type field that is a KID of a type for which this view is a standard view, if any
		Field typeField = new Field();
		typeField.setApiName("typeId");
		typeField.setLabel("Type ID");
		typeField.setDataType(new KIDDataType());
		typeField.setDbColumn("typeid");
		typeField.setRequired(false);
		this.addField(typeField);
		
		Field layoutField = new Field();
		layoutField.setApiName("layout");
		layoutField.setLabel("Layout");
		layoutField.setDataType(new TypeReference(layoutType));
		layoutField.setDbColumn("layout");
		layoutField.setRequired(false);
		this.addField(layoutField);
		
		Field accessField = new Field();
		accessField.setApiName("accessLevel");
		accessField.setLabel("Access Level");
		
		EnumerationDataType enumDT = new EnumerationDataType("Editable\nRead-only\nRead-only methods\nClosed");
		enumDT.setValidateValues(true);
		
		accessField.setDataType(enumDT);
		accessField.setDbColumn("accesslevel");
		accessField.setRequired(true);
		this.addField(accessField);
	}
}
