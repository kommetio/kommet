/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries.jcr;

import com.fasterxml.jackson.annotation.JsonProperty;

import kommet.data.PIR;

public class Grouping
{
	private PIR propertyId;
	private String propertyName;
	private String alias;

	public void setPropertyId(PIR propertyId)
	{
		this.propertyId = propertyId;
	}

	@JsonProperty("property_id")
	public PIR getPropertyId()
	{
		return propertyId;
	}

	public void setAlias(String alias)
	{
		this.alias = alias;
	}

	public String getAlias()
	{
		return alias;
	}

	@JsonProperty("property_name")
	public String getPropertyName()
	{
		return propertyName;
	}

	public void setPropertyName(String propertyName)
	{
		this.propertyName = propertyName;
	}
}