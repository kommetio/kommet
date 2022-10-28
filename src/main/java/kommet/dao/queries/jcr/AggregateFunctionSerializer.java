/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries.jcr;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import kommet.dao.dal.AggregateFunction;

public class AggregateFunctionSerializer extends JsonSerializer<AggregateFunction>
{
	@Override
	public void serialize(AggregateFunction aggr, JsonGenerator generator, SerializerProvider provider) throws IOException, JsonProcessingException
	{
		generator.writeString(aggr != null ? aggr.name().toLowerCase() : "null");
	}
}