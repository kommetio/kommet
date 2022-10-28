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
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class LoginHistoryKType extends Type
{
	private static final long serialVersionUID = 6338450622885036644L;
	private static final String LABEL = "Login History";
	private static final String PLURAL_LABEL = "Login History";
	
	public LoginHistoryKType()
	{
		super();
	}
	
	public LoginHistoryKType(Type userType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.LOGIN_HISTORY_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.LOGIN_HISTORY_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.LOGIN_HISTORY_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		Field userField = new Field();
		userField.setApiName("loginUser");
		userField.setLabel("Login User");
		userField.setDataType(new TypeReference(userType));
		userField.setDbColumn("loginuser");
		userField.setRequired(true);
		this.addField(userField);
		
		Field methodField = new Field();
		methodField.setApiName("method");
		methodField.setLabel("Method");
		methodField.setDataType(new EnumerationDataType("Browser\nMobile\nREST API\nWeb Service API"));
		methodField.setDbColumn("method");
		methodField.setRequired(true);
		this.addField(methodField);
		
		Field resultField = new Field();
		resultField.setApiName("result");
		resultField.setLabel("Result");
		resultField.setDataType(new EnumerationDataType("Success\nInvalid password\nUser inactive"));
		resultField.setDbColumn("result");
		resultField.setRequired(true);
		this.addField(resultField);
		
		Field ip4Field = new Field();
		ip4Field.setApiName("ip4Address");
		ip4Field.setLabel("IPv4 Address");
		ip4Field.setDataType(new TextDataType(15));
		ip4Field.setDbColumn("ip4address");
		ip4Field.setRequired(true);
		this.addField(ip4Field);
		
		Field ip6Field = new Field();
		ip6Field.setApiName("ip6Address");
		ip6Field.setLabel("IPv6 Address");
		ip6Field.setDataType(new TextDataType(50));
		ip6Field.setDbColumn("ip6address");
		ip6Field.setRequired(false);
		this.addField(ip6Field);
	}
}
