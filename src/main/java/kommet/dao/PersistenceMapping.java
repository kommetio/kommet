/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.util.Map;

import kommet.basic.RecordProxy;

/**
 * Represents a mapping between any two types, one of which acts as a proxy in some persistence engine
 * whereas the other acts as a database representation.
 * 
 * @author Radek Krawiec
 */
public abstract class PersistenceMapping
{	
	protected Class<? extends RecordProxy> clazz;
	
	protected Map<String, ColumnMapping> columnMappings;
	protected Map<String, ColumnMapping> propertyMappings;
	protected String table;
	
	//private static final Logger log = LoggerFactory.getLogger(ObjectMapping.class);

	public Map<String, ColumnMapping> getColumnMappings()
	{
		return columnMappings;
	}

	public Map<String, ColumnMapping> getPropertyMappings()
	{
		return propertyMappings;
	}

	public void setTable(String table)
	{
		this.table = table;
	}

	public String getTable()
	{
		return table;
	}
}