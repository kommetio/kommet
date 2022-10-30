/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.dal;

public class AggregateFunctionCall
{
	private AggregateFunction function;
	private String property;

	public void setFunction(AggregateFunction function)
	{
		this.function = function;
	}

	public AggregateFunction getFunction()
	{
		return function;
	}

	public void setProperty(String property)
	{
		this.property = property;
	}

	public String getProperty()
	{
		return property;
	}

	public String getStringName()
	{
		return this.function + "(" + this.property + ")";
	}
}