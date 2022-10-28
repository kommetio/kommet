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

public class ReportTypeKType extends Type
{
	private static final long serialVersionUID = 2566386295020632261L;
	private static final String LABEL = "Report Type";
	private static final String PLURAL_LABEL = "Report Types";
	
	public ReportTypeKType() throws KommetException
	{
		super();
		this.setApiName(SystemTypes.REPORT_TYPE_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.REPORT_TYPE_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.REPORT_TYPE_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		Field baseTypeField = new Field();
		baseTypeField.setApiName("baseTypeId");
		baseTypeField.setLabel("Base Type ID");
		baseTypeField.setDataType(new KIDDataType());
		baseTypeField.setDbColumn("basetypeid");
		baseTypeField.setRequired(true);
		this.addField(baseTypeField);
		
		Field nameField = new Field();
		nameField.setApiName("name");
		nameField.setLabel("Name");
		nameField.setDataType(new TextDataType(100));
		nameField.setDbColumn("name");
		nameField.setRequired(true);
		this.addField(nameField);
		
		Field dalField = new Field();
		dalField.setApiName("serializedQuery");
		dalField.setLabel("Serialized Query");
		dalField.setDataType(new TextDataType(14000));
		dalField.setDbColumn("serializedquery");
		dalField.setRequired(true);
		this.addField(dalField);
		
		Field descField = new Field();
		descField.setApiName("description");
		descField.setLabel("Description");
		descField.setDataType(new TextDataType(255));
		descField.setDbColumn("description");
		descField.setRequired(false);
		this.addField(descField);
	}
}
