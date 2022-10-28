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

import kommet.dao.queries.RestrictionOperator;

public class RestrictionOperatorDeserializer extends JsonDeserializer<RestrictionOperator>
{
	public RestrictionOperator deserialize(JsonParser parser, DeserializationContext ctx) throws IOException, JsonProcessingException
	{
		return parser.getValueAsString() != null ? RestrictionOperator.valueOf(parser.getValueAsString().toUpperCase()) : null;
	}
}