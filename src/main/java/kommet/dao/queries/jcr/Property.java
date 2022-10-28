/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries.jcr;

import com.fasterxml.jackson.annotation.JsonProperty;

import kommet.dao.dal.AggregateFunction;
import kommet.data.PIR;

public class Property
{
	private PIR id;
	private String name;
	private String alias;
	private AggregateFunction aggr;

	public void setId(PIR id)
	{
		this.id = id;
	}

	public PIR getId()
	{
		return id;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public void setAggregateFunction(AggregateFunction aggr)
	{
		this.aggr = aggr;
	}

	@JsonProperty("aggr")
	public AggregateFunction getAggregateFunction()
	{
		return aggr;
	}

	public void setAlias(String alias)
	{
		this.alias = alias;
	}

	public String getAlias()
	{
		return alias;
	}
}