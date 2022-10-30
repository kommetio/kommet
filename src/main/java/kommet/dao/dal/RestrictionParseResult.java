/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.dal;

import java.util.Set;

import kommet.dao.queries.Restriction;

public class RestrictionParseResult
{
	private Restriction restriction;
	private Integer currentTokenIndex;
	
	/**
	 * Properties included in the restriction - both nested and direct properties.
	 */
	private Set<String> properties;
	
	public RestrictionParseResult (Restriction restriction, Integer currentIndex, Set<String> properties)
	{
		this.restriction = restriction;
		this.currentTokenIndex = currentIndex;
		this.properties = properties;
	}
	
	public Restriction getRestriction()
	{
		return restriction;
	}

	public Integer getCurrentTokenIndex()
	{
		return currentTokenIndex;
	}

	public Set<String> getProperties()
	{
		return properties;
	}
}