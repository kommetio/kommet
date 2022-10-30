/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

public class ColumnMapping
{
	private String column;
	private String property;
	private Boolean required;
	
	public void setColumn(String column)
	{
		this.column = column;
	}
	public String getColumn()
	{
		return column;
	}
	public void setProperty(String property)
	{
		this.property = property;
	}
	public String getProperty()
	{
		return property;
	}
	public void setRequired(Boolean required)
	{
		this.required = required;
	}
	public Boolean getRequired()
	{
		return required;
	}
}