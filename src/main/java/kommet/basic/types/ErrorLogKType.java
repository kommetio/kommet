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
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class ErrorLogKType extends Type
{
	private static final long serialVersionUID = -3502223805104729615L;
	private static final String LABEL = "Error Log";
	private static final String PLURAL_LABEL = "Error Logs";
	
	public ErrorLogKType()
	{
		super();
	}
	
	public ErrorLogKType(Type userType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.ERROR_LOG_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.ERROR_LOG_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.ERROR_LOG_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add message field
		Field messageField = new Field();
		messageField.setApiName("message");
		messageField.setLabel("Message");
		messageField.setDataType(new TextDataType(500));
		messageField.setDbColumn("message");
		messageField.setRequired(true);
		this.addField(messageField);
		
		Field detailsField = new Field();
		detailsField.setApiName("details");
		detailsField.setLabel("Details");
		detailsField.setDataType(new TextDataType(10000));
		detailsField.setDbColumn("details");
		detailsField.setRequired(false);
		this.addField(detailsField);
		
		Field userField = new Field();
		userField.setApiName("affectedUser");
		userField.setLabel("Affected User");
		userField.setDataType(new TypeReference(userType));
		userField.setDbColumn("affecteduser");
		userField.setRequired(true);
		this.addField(userField);
		
		Field classField = new Field();
		classField.setApiName("codeClass");
		classField.setLabel("Code Class");
		classField.setDataType(new TextDataType(100));
		classField.setDbColumn("codeclass");
		classField.setRequired(false);
		this.addField(classField);
		
		Field lineField = new Field();
		lineField.setApiName("codeLine");
		lineField.setLabel("Code Line");
		lineField.setDataType(new NumberDataType(0, Integer.class));
		lineField.setDbColumn("codeline");
		lineField.setRequired(false);
		this.addField(lineField);
		
		Field severityField = new Field();
		severityField.setApiName("severity");
		severityField.setLabel("Severity");
		severityField.setDataType(new EnumerationDataType("Fatal\nError\nWarning\nInfo\nDebug"));
		severityField.setDbColumn("severity");
		severityField.setRequired(true);
		this.addField(severityField);
	}
}
