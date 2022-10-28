/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries.jcr;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import kommet.data.KeyPrefix;
import kommet.data.KeyPrefixException;

public class KeyPrefixDeserializer extends JsonDeserializer<KeyPrefix>
{
	public KeyPrefix deserialize(JsonParser parser, DeserializationContext ctx) throws IOException, JsonProcessingException
	{
		try
		{
			return parser.getValueAsString() != null ? KeyPrefix.get(parser.getValueAsString()) : null;
		}
		catch (KeyPrefixException e)
		{
			throw new JcrSerializationException("Invalid key prefix value " + parser.getValueAsString());
		}
	}
}