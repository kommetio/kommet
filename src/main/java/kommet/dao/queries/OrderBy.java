/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries;

public class OrderBy
{
	private SortDirection order;
	private String property;
	private String propertySQL;
	
	public OrderBy (SortDirection order, String property)
	{
		this.order = order;
		this.property = property;
	}
	
	public OrderBy (SortDirection order, String property, String propertySQL)
	{
		this.order = order;
		this.property = property;
		this.propertySQL = propertySQL;
	}
	
	public OrderBy (SortDirection order)
	{
		this.order = order;
	}
	
	public OrderBy()
	{
		// empty
	}

	public void setOrder(SortDirection order)
	{
		this.order = order;
	}
	public SortDirection getOrder()
	{
		return order;
	}
	public void setProperty(String property)
	{
		this.property = property;
	}
	public String getProperty()
	{
		return property;
	}
	
	@Override
	public String toString()
	{
		return property + " " + (SortDirection.ASC.equals(order) ? " ASC" : " DESC");
	}

	public void setPropertySQL(String propertySQL)
	{
		this.propertySQL = propertySQL;
	}

	/**
	 * Returns the SQL representation of the property, including aliases used in the SQL query.
	 * @return
	 */
	public String getPropertySQL()
	{
		return propertySQL;
	}
}