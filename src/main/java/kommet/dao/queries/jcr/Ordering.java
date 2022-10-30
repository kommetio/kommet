/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries.jcr;

import com.fasterxml.jackson.annotation.JsonProperty;

import kommet.data.PIR;

public class Ordering
{
	private PIR propertyId;
	private String propertyName;
	private String sortDirection;

	public void setPropertyId(PIR propertyId)
	{
		this.propertyId = propertyId;
	}

	@JsonProperty("property_id")
	public PIR getPropertyId()
	{
		return propertyId;
	}

	public void setSortDirection(String sortDirection)
	{
		this.sortDirection = sortDirection;
	}

	@JsonProperty("direction")
	public String getSortDirection()
	{
		return sortDirection;
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