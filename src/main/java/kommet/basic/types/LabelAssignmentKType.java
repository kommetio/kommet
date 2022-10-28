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
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class LabelAssignmentKType extends Type
{
	private static final long serialVersionUID = 2119305515330465693L;
	private static final String LABEL = "Label Assignment";
	private static final String PLURAL_LABEL = "Label Assignments";
	
	public LabelAssignmentKType()
	{
		super();
	}
	
	public LabelAssignmentKType(LabelKType labelType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.LABEL_ASSIGNMENT_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.LABEL_ASSIGNMENT_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.LABEL_ASSIGNMENT_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		Field recordField = new Field();
		recordField.setApiName("recordId");
		recordField.setLabel("Record ID");
		recordField.setDataType(new KIDDataType());
		recordField.setDbColumn("recordid");
		recordField.setRequired(true);
		this.addField(recordField);
		
		Field labelField = new Field();
		labelField.setApiName("label");
		labelField.setLabel("Label");
		labelField.setDataType(new TypeReference(labelType));
		labelField.setDbColumn("label");
		labelField.setRequired(true);
		this.addField(labelField);
	}
}

