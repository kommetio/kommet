/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.json;

import java.util.ArrayList;
import java.util.List;

import kommet.data.Field;
import kommet.data.Type;
import kommet.utils.MiscUtils;

public class TypeJSONUtil
{
	public static String serializeType(Type type)
	{
		List<String> props = new ArrayList<String>();
		props.add("\"qualifiedName\": \"" + type.getQualifiedName() + "\"");
		props.add("\"id\": \"" + type.getKID() + "\"");
		props.add("\"keyPrefix\": \"" + type.getKeyPrefix() + "\"");
		props.add("\"label\": \"" + type.getLabel() + "\"");
		props.add("\"pluralLabel\": \"" + type.getPluralLabel() + "\"");
		
		// add fields
		List<String> serializedFields = new ArrayList<String>();
		for (Field field : type.getFields())
		{
			serializedFields.add(serializeField(field));
		}
		
		props.add("\"fields\": [" + MiscUtils.implode(serializedFields, ", ") + "]");
		
		return "{ " + MiscUtils.implode(props, ", ") + " }";
	}

	private static String serializeField(Field field)
	{
		List<String> props = new ArrayList<String>();
		props.add("\"id\": \"" + field.getKID() + "\"");
		props.add("\"apiName\": \"" + field.getApiName() + "\"");
		props.add("\"label\": \"" + field.getLabel() + "\"");
		props.add("\"isSystem\": " + Field.isSystemField(field.getApiName()));
		
		StringBuilder dtJSON = new StringBuilder();
		dtJSON.append("{ \"name\": \"").append(field.getDataType().getName()).append("\" }");
		props.add("\"datatype\": " + dtJSON.toString());
		
		return "{ " + MiscUtils.implode(props, ", ") + " }";
	}
}