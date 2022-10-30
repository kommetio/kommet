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
import kommet.data.datatypes.DateTimeDataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.TextDataType;
import kommet.utils.AppConfig;

public class EmailKType extends Type
{
	private static final long serialVersionUID = -6160249222953074517L;
	private static final String LABEL = "Email";
	private static final String PLURAL_LABEL = "Emails";
	
	public EmailKType() throws KommetException
	{
		super();
		this.setApiName(SystemTypes.EMAIL_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.EMAIL_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.EMAIL_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		Field msgIdField = new Field();
		msgIdField.setApiName("messageId");
		msgIdField.setLabel("Message ID");
		msgIdField.setDataType(new TextDataType(255));
		msgIdField.setDbColumn("messageid");
		msgIdField.setRequired(false);
		this.addField(msgIdField);
		
		// add content field
		Field plainTextBodyField = new Field();
		plainTextBodyField.setApiName("plainTextBody");
		plainTextBodyField.setLabel("Plain Text Body");
		plainTextBodyField.setDataType(new TextDataType(10000));
		plainTextBodyField.setDbColumn("plaintextbody");
		plainTextBodyField.setRequired(false);
		this.addField(plainTextBodyField);
		
		Field htmlBodyField = new Field();
		htmlBodyField.setApiName("htmlBody");
		htmlBodyField.setLabel("HTML Body");
		htmlBodyField.setDataType(new TextDataType(10000));
		htmlBodyField.setDbColumn("htmlbody");
		htmlBodyField.setRequired(false);
		this.addField(htmlBodyField);
		
		Field subjectField = new Field();
		subjectField.setApiName("subject");
		subjectField.setLabel("Subject");
		subjectField.setDataType(new TextDataType(250));
		subjectField.setDbColumn("subject");
		subjectField.setRequired(false);
		this.addField(subjectField);
		
		Field senderField = new Field();
		senderField.setApiName("sender");
		senderField.setLabel("Sender");
		senderField.setDataType(new TextDataType(100));
		senderField.setDbColumn("sender");
		senderField.setRequired(true);
		this.addField(senderField);
		
		Field recipientsField = new Field();
		recipientsField.setApiName("recipients");
		recipientsField.setLabel("Recipients");
		recipientsField.setDataType(new TextDataType(1000));
		recipientsField.setDbColumn("recipients");
		recipientsField.setRequired(true);
		this.addField(recipientsField);
		
		Field ccField = new Field();
		ccField.setApiName("ccRecipients");
		ccField.setLabel("CC Recipients");
		ccField.setDataType(new TextDataType(1000));
		ccField.setDbColumn("ccrecipients");
		ccField.setRequired(false);
		this.addField(ccField);
		
		Field bccField = new Field();
		bccField.setApiName("bccRecipients");
		bccField.setLabel("BCC Recipients");
		bccField.setDataType(new TextDataType(1000));
		bccField.setDbColumn("bccrecipients");
		bccField.setRequired(false);
		this.addField(bccField);
		
		Field sentDateField = new Field();
		sentDateField.setApiName("sendDate");
		sentDateField.setLabel("Send Date");
		sentDateField.setDataType(new DateTimeDataType());
		sentDateField.setDbColumn("senddate");
		sentDateField.setRequired(false);
		this.addField(sentDateField);
		
		Field statusField = new Field();
		statusField.setApiName("status");
		statusField.setLabel("Status");
		statusField.setDataType(new EnumerationDataType("Draft\nSent"));
		statusField.setDbColumn("status");
		statusField.setRequired(true);
		this.addField(statusField);
	}
}
