/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic.types;

import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KeyPrefix;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.BooleanDataType;
import kommet.data.datatypes.DateTimeDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class ApiTokenKType extends Type
{
	private static final long serialVersionUID = 1L;

	public ApiTokenKType()
	{
        super();
	}

	public ApiTokenKType(Type userType) throws KommetException
	{
		setApiName(SystemTypes.API_TOKEN_API_NAME);
		setKeyPrefix(KeyPrefix.get(KID.API_TOKEN_PREFIX));
		setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.API_TOKEN_ID_SEQ));
		setLabel("API Token");
		setPluralLabel("API Tokens");
		setPackage(AppConfig.BASE_TYPE_PACKAGE);
		setBasic(true);

		addField("name", "Name", new TextDataType(100), true, "name");
		addField("tokenHash", "Token Hash", new TextDataType(64), true, "tokenhash");
		addField("tokenPrefix", "Token Prefix", new TextDataType(12), true, "tokenprefix");
		addField("systemUser", "System User", new TypeReference(userType), true, "systemuser");
		addField("expirationDate", "Expiration Date", new DateTimeDataType(), false, "expirationdate");

		Field readPermission = addField("isReadPermission", "Read Permission", new BooleanDataType(), true, "isreadpermission");
		readPermission.setDefaultValue("true");
		Field writePermission = addField("isWritePermission", "Write Permission", new BooleanDataType(), true, "iswritepermission");
		writePermission.setDefaultValue("true");
	}

	private Field addField(String apiName, String label, kommet.data.datatypes.DataType dataType, boolean required, String dbColumn) throws KommetException
	{
		Field field = new Field();
		field.setApiName(apiName);
		field.setLabel(label);
		field.setDataType(dataType);
		field.setRequired(required);
		field.setDbColumn(dbColumn);
		addField(field);
		return field;
	}
}