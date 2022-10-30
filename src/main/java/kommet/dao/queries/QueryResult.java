/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries;

import java.util.HashMap;
import java.util.Map;

import kommet.dao.dal.DALException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;

public class QueryResult extends Record
{
	private Map<String, Object> aggregateValues = new HashMap<String, Object>();
	private Map<String, Object> groupByValues = new HashMap<String, Object>();
	
	public QueryResult(Type type) throws KommetException
	{
		super(type);
	}

	public QueryResult (Record record) throws KommetException
	{
		super(record.getType());
		
		for (String field : record.getFieldValues().keySet())
		{
			this.setField(field, record.getField(field));
		}
	}

	public void addAggregateValue(String alias, Object value)
	{
		String normalizedAlias = alias;
		if (alias.contains("("))
		{
			// set the aggregate function name to lower case, but leave the property name as is
			String[] aliasParts = alias.split("\\(");
			normalizedAlias = aliasParts[0].toLowerCase() + "(" + aliasParts[1];
		}

		this.aggregateValues.put(normalizedAlias, value);
	}
	
	public void addGroupByValue(String alias, Object value)
	{
		this.groupByValues.put(alias, value);
	}

	/**
	 * Gets an aggregate value from the query result.
	 * @param alias - the alias of the aggregate property, consisting of the aggregate function and the
	 * aggregated property name, e.g. "count(displayName)".
	 * <p>Aliases are normalized - the aggregate function name is case insensitive, but the property API name
	 * is case sensitive. Due to this, <tt>count(userName)</tt> will be treated the same as <tt>COUNT(userName)</tt>,
	 * but not as <tt>count(username)</tt>.
	 * </p>
	 * @return Value of the aggregated property
	 * @throws InvalidResultSetAccess if a property with the given alias does not exist
	 */
	public Object getAggregateValue(String alias) throws InvalidResultSetAccess
	{
		String normalizedAlias = alias;
		if (alias.contains("("))
		{
			// set the aggregate function name to lower case, but leave the property name as is
			String[] aliasParts = alias.split("\\(");
			normalizedAlias = aliasParts[0].toLowerCase() + "(" + aliasParts[1];
		}
		
		if (aggregateValues.containsKey(normalizedAlias))
		{
			return aggregateValues.get(normalizedAlias);
		}
		else
		{
			throw new InvalidResultSetAccess("No aggregate value '" + alias + "' found");
		}
	}
	
	/**
	 * Returns a value of a grouped property.
	 * @param alias
	 * @return
	 * @throws InvalidResultSetAccess
	 */
	public Object getGroupByValue(String alias) throws InvalidResultSetAccess
	{
		if (groupByValues.containsKey(alias))
		{
			return groupByValues.get(alias);
		}
		else
		{
			throw new InvalidResultSetAccess("No group by value '" + alias + "' found");
		}
	}

	/**
	 * If only one aggregate value exists, its value is returned. This is convenient when a query like
	 * <br/><tt>SELECT count(id) FROM User</tt><br/>is issued and we want to read its value without referencing
	 * the <tt>count(id)</tt> alias.
	 * @return
	 * @throws DALException
	 */
	public Object getSingleAggregateValue() throws DALException
	{
		if (aggregateValues.size() == 1)
		{
			return aggregateValues.get(aggregateValues.keySet().iterator().next());
		}
		else
		{
			throw new DALException("Query result contains more than one aggregate value, so method getSingleAggregateValue cannot be used");
		}
	}
	
	public Map<String, Object> getAggregateValues()
	{
		return this.aggregateValues;
	}
	
	public Map<String, Object> getGroupByValues()
	{
		return this.groupByValues;
	}
}