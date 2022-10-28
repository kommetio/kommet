/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.LibraryKType;
import kommet.basic.types.SystemTypes;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.KIDDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class LibraryItemKType extends Type
{
	private static final long serialVersionUID = -7369305060762741980L;
	private static final String LABEL = "Library Item";
	private static final String PLURAL_LABEL = "Library Items";

	public LibraryItemKType()
	{
		super();
	}
	
	public LibraryItemKType (LibraryKType libType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.LIBRARY_ITEM_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.LIBRARY_ITEM_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.LIBRARY_ITEM_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
				
		// add record ID field
		Field recordIdField = new Field();
		recordIdField.setApiName("recordId");
		recordIdField.setLabel("Record ID");
		recordIdField.setDataType(new KIDDataType());
		recordIdField.setDbColumn("recordid");
		recordIdField.setRequired(false);
		this.addField(recordIdField);
		
		Field libField = new Field();
		libField.setApiName("library");
		libField.setLabel("Library");
		TypeReference libDT = new TypeReference(libType);
		libDT.setCascadeDelete(true);
		libField.setDataType(libDT);
		libField.setDbColumn("library");
		libField.setRequired(true);
		this.addField(libField);
		
		Field nameField = new Field();
		nameField.setApiName("apiName");
		nameField.setLabel("API Name");
		nameField.setDataType(new TextDataType(255));
		nameField.setDbColumn("apiname");
		nameField.setRequired(true);
		this.addField(nameField);
		
		Field defField = new Field();
		defField.setApiName("definition");
		defField.setLabel("Definition");
		defField.setDataType(new TextDataType(32000));
		defField.setDbColumn("definition");
		defField.setRequired(true);
		this.addField(defField);
		
		Field componentTypeField = new Field();
		componentTypeField.setApiName("componentType");
		componentTypeField.setLabel("Component Type");
		componentTypeField.setDataType(new NumberDataType(0, Integer.class));
		componentTypeField.setDbColumn("componenttype");
		componentTypeField.setRequired(true);
		this.addField(componentTypeField);
		
		Field accessField = new Field();
		accessField.setApiName("accessLevel");
		accessField.setLabel("Access Level");
		
		EnumerationDataType enumDT = new EnumerationDataType("Editable\nRead-only\nRead-only methods\nClosed");
		enumDT.setValidateValues(true);
		
		accessField.setDataType(enumDT);
		accessField.setDbColumn("accesslevel");
		
		// access level is not required, because its inherited from the library
		accessField.setRequired(false);
		this.addField(accessField);
	}
}