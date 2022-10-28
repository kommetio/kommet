/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.dal;

import kommet.data.KommetException;
import kommet.data.datatypes.DataType;

public enum AggregateFunction
{
	COUNT,
	SUM,
	AVG,
	MIN,
	MAX;
	
	public String getSQL() throws DALException
	{
		if (this.equals(COUNT))
		{
			return "count";
		}
		else if (this.equals(SUM))
		{
			return "sum";
		}
		else if (this.equals(AVG))
		{
			return "avg";
		}
		else if (this.equals(MIN))
		{
			return "min";
		}
		else if (this.equals(MAX))
		{
			return "max";
		}
		else
		{
			throw new DALException("Cannot translated DAL aggregate function " + this.name() + " to native SQL");
		}
	}
	
	public static AggregateFunction getByName (String name) throws KommetException
	{
		if (name == null)
		{
			return null;
		}
		else if ("count".equals(name.toLowerCase()))
		{
			return AggregateFunction.COUNT;
		}
		else if ("sum".equals(name.toLowerCase()))
		{
			return AggregateFunction.SUM;
		}
		else if ("avg".equals(name.toLowerCase()))
		{
			return AggregateFunction.AVG;
		}
		else if ("min".equals(name.toLowerCase()))
		{
			return AggregateFunction.MIN;
		}
		else if ("max".equals(name.toLowerCase()))
		{
			return AggregateFunction.MAX;
		}
		else
		{
			throw new KommetException("Unrecognized aggregate function " + name);
		}
	}

	public static boolean validate(AggregateFunction aggr, DataType dt) throws KommetException
	{
		if (AggregateFunction.AVG.equals(aggr) || AggregateFunction.MIN.equals(aggr) || AggregateFunction.MAX.equals(aggr) || AggregateFunction.SUM.equals(aggr))
		{
			return DataType.NUMBER == dt.getId();
		}
		else if (AggregateFunction.COUNT.equals(aggr))
		{
			// count is valid for all data type
			return true;
		}
		else
		{
			throw new KommetException("Cannot determine correctness of applying aggregate function " + aggr.name() + " to field");
		}
	}
}