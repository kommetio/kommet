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
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.TextDataType;
import kommet.utils.AppConfig;

public class AppKType extends Type
{	
	private static final long serialVersionUID = -1459416085123713437L;
	private static final String LABEL = "App";
	private static final String PLURAL_LABEL = "Apps";

	
	public AppKType() throws KommetException
	{
		super();
		this.setApiName(SystemTypes.APP_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.APP_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.APP_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add user name field
		Field nameField = new Field();
		nameField.setApiName("name");
		nameField.setLabel("Name");
		nameField.setDataType(new TextDataType(255));
		nameField.setDbColumn("name");
		nameField.setRequired(true);
		this.addField(nameField);
		
		Field labelField = new Field();
		labelField.setApiName("label");
		labelField.setLabel("Label");
		labelField.setDataType(new TextDataType(50));
		labelField.setDbColumn("label");
		labelField.setRequired(true);
		this.addField(labelField);
		
		Field typeField = new Field();
		typeField.setApiName("type");
		typeField.setLabel("Type");
		EnumerationDataType typeDT = new EnumerationDataType("Internal app\nWebsite");
		typeDT.setValidateValues(true);
		typeField.setDataType(typeDT);
		typeField.setDbColumn("type");
		typeField.setRequired(true);
		this.addField(typeField);
		
		Field landingUrlField = new Field();
		landingUrlField.setApiName("landingUrl");
		landingUrlField.setLabel("Landing URL");
		landingUrlField.setDataType(new TextDataType(255));
		landingUrlField.setDbColumn("landingurl");
		landingUrlField.setRequired(false);
		this.addField(landingUrlField);
	}
}
