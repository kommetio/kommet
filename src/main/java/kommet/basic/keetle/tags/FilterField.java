/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

public class FilterField
{
	private String field;
	
	public FilterField(String field, String comparison, String label, String hashId)
	{
		super();
		this.field = field;
		this.comparison = comparison;
		this.label = label;
		this.hashId = hashId;
	}

	private String comparison;
	private String label;
	private String hashId;
	
	public String getField()
	{
		return field;
	}

	public String getComparison()
	{
		return comparison;
	}
	
	public String getLabel()
	{
		return label;
	}
	
	public String getHashId()
	{
		return hashId;
	}
}
