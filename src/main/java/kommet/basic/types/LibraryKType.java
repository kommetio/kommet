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

public class LibraryKType extends Type
{
	private static final long serialVersionUID = -7369305060762741980L;
	private static final String LABEL = "Library";
	private static final String PLURAL_LABEL = "Libraries";

	public LibraryKType()
	{
		super();
	}
	
	public LibraryKType (FileKType fileType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.LIBRARY_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.LIBRARY_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.LIBRARY_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
				
		// add reference to file
		Field nameField = new Field();
		nameField.setApiName("name");
		nameField.setLabel("Name");
		nameField.setDataType(new TextDataType(50));
		nameField.setDbColumn("name");
		nameField.setRequired(true);
		this.addField(nameField);
		
		Field providerField = new Field();
		providerField.setApiName("provider");
		providerField.setLabel("Provider");
		providerField.setDataType(new TextDataType(50));
		providerField.setDbColumn("provider");
		providerField.setRequired(false);
		this.addField(providerField);
		
		Field versionField = new Field();
		versionField.setApiName("version");
		versionField.setLabel("Version");
		versionField.setDataType(new TextDataType(5));
		versionField.setDbColumn("version");
		versionField.setRequired(true);
		this.addField(versionField);
		
		Field descField = new Field();
		descField.setApiName("description");
		descField.setLabel("Description");
		descField.setDataType(new TextDataType(255));
		descField.setDbColumn("description");
		descField.setRequired(false);
		this.addField(descField);
		
		Field sourceField = new Field();
		sourceField.setApiName("source");
		sourceField.setLabel("Source");
		sourceField.setDataType(new EnumerationDataType("Local\nExternal (manual deployment)\nExternal (library repository)\nPlatform required upgrade"));
		sourceField.setDbColumn("source");
		sourceField.setRequired(true);
		this.addField(sourceField);
		
		Field statusField = new Field();
		statusField.setApiName("status");
		statusField.setLabel("Status");
		EnumerationDataType statusEnum = new EnumerationDataType("Installed\nInstalled-Deactivated\nInstallation failed\nNot installed");
		statusEnum.setValidateValues(true);
		statusField.setDataType(statusEnum);
		statusField.setDbColumn("status");
		statusField.setRequired(true);
		this.addField(statusField);
		
		// add isEnabled field
		Field enabledField = new Field();
		enabledField.setApiName("isEnabled");
		enabledField.setLabel("Is Enabled");
		enabledField.setDataType(new BooleanDataType());
		enabledField.setDbColumn("isenabled");
		enabledField.setRequired(true);
		this.addField(enabledField);
		
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
