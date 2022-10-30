/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.rowset.serial.SerialArray;
import javax.sql.rowset.serial.SerialException;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import kommet.dao.dal.AggregateFunctionCall;
import kommet.dao.dal.DALException;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.CriteriaException;
import kommet.dao.queries.DeleteQuery;
import kommet.dao.queries.InsertQuery;
import kommet.dao.queries.QueryResult;
import kommet.dao.queries.SelectQuery;
import kommet.dao.queries.UpdateQuery;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.utils.MiscUtils;

/**
 * Represents a mapping between a Kommet type and native database table/columns. 
 * @author Radek Krawiec
 */
public class TypePersistenceMapping extends PersistenceMapping
{
	protected EnvData envData;
	protected Type type;
	
	// private static final Logger log = LoggerFactory.getLogger(TypePersistenceMapping.class);
	
	public TypePersistenceMapping (Type type, EnvData env) throws KommetException
	{	
		super();
		
		this.columnMappings = new HashMap<String, ColumnMapping>();
		this.propertyMappings = new HashMap<String, ColumnMapping>();
		this.envData = env;
		
		this.table = type.getDbTable();
		this.type = type;
		
		// add field mappings
		for (Field field : type.getFields())
		{
			ColumnMapping colMapping = new ColumnMapping();
			colMapping.setColumn(field.getDbColumn());
			colMapping.setProperty(field.getApiName());
			colMapping.setRequired(field.isRequired());
			
			this.columnMappings.put(colMapping.getColumn(), colMapping);
			this.propertyMappings.put(colMapping.getProperty(), colMapping);
		}
	}
	
	public Record getRecordFromRowSet (SqlRowSet rowSet, Criteria criteria) throws KommetException
	{
		Record record = new Record(this.type);
		
		// if there are any collections to be fetched, initialize them, because collections will always
		// be returned not null, even if they are empty.
		for (String inverseCollectionProperty : criteria.getInverseCollectionProperties())
		{
			// inverse collection properties list all fields to fetch, e.g. children.id, children.name etc.
			// but we only need the first part ("children") because this is the real name of the collection
			// and nested collections are not allowed
			
			// this check can probably removed because the situation will never occur
			if (inverseCollectionProperty.contains("."))
			{
				throw new KommetException("Inverse collection properties cannot be nested");
			}
			
			// if the collection has not been initialized yet earlier in this loop, initialize it
			if (record.attemptGetField(inverseCollectionProperty) == null)
			{
				// regardless of the collection type, it will always be a list of records
				record.setField(inverseCollectionProperty, new ArrayList<Record>());
			}
		}
		
		// if there are any collections to be fetched, initialize them, because collections will always
		// be returned not null, even if they are empty.
		for (String associationProperty : criteria.getAssociationProperties())
		{
			// inverse collection properties list all fields to fetch, e.g. children.id, children.name etc.
			// but we only need the first part ("children") because this is the real name of the collection
			// and nested collections are not allowed
			
			// this check can probably removed because the situation will never occur
			if (associationProperty.contains("."))
			{
				throw new KommetException("Association properties cannot be nested");
			}
			
			// if the collection has not been initialized yet earlier in this loop, initialize it
			if (record.attemptGetField(associationProperty) == null)
			{
				// regardless of the collection type, it will always be a list of records
				record.setField(associationProperty, new ArrayList<Record>());
			}
		}
		
		// initialize only fields that are in the result set
		for (String fieldName : criteria.getProperties())
		{
			try
			{
				Field field = criteria.getType().getField(fieldName);
				String fieldToSet = field.getApiName();
				
				// if the field is an type reference, instantiate it
				if (field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
				{
					// get the ID of the referenced object - can be null
					Object refObjId = rowSet.getObject(field.getApiName());
					if (refObjId != null)
					{
						KID kid = KID.get((String)refObjId);
						Record refObj = (Record)record.attemptGetField(fieldToSet);
						if (refObj == null)
						{
							refObj = new Record(envData.getType(((TypeReference)field.getDataType()).getType().getKeyPrefix()));
						}
						refObj.setKID(kid);
						record.setField(fieldToSet, refObj, envData);
						//log.debug("Set " + fieldToSet + " (" + mainTableAlias + field.getDbColumn() + ") = " + refObj);
					}
				}
				else if (field.getDataTypeId().equals(DataType.DATETIME) || field.getDataTypeId().equals(DataType.DATE))
				{
					String sReadVal = rowSet.getString(field.getApiName());
					if (sReadVal == null)
					{
						record.setField(fieldToSet, null, this.envData);
					}
					else
					{
						// TODO - the value of the field is extracted twice below - try to think
						// of a proper way to do this. The goal is to have the time in GMT timezone.
						/*Calendar gmtCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
						gmtCalendar.setTime(readVal);
						record.setField(fieldToSet, (Date)rowSet.getTime(mainTableAlias + field.getDbColumn(), gmtCalendar), this.envData);
						*/
						try
						{
							record.setField(fieldToSet, MiscUtils.parseDateTime(sReadVal, MiscUtils.DATE_TIME_FORMAT_FULL), this.envData);
						}
						catch (ParseException e)
						{
							throw new KommetException("Could not parse date/time value " + sReadVal + " for property " + fieldToSet + " on type " + type.getQualifiedName());
						}
					}
				}
				else if (field.getDataTypeId().equals(DataType.NUMBER))
				{
					Object val = null;
					NumberDataType numberDataType = (NumberDataType)field.getDataType();
					if (numberDataType.getJavaType().equals(BigDecimal.class.getName()))
					{
						val = rowSet.getBigDecimal(field.getApiName());
					}
					else if (numberDataType.getJavaType().equals(Integer.class.getName()))
					{
						// need to check if value is null first, because getInt always return non-null values
						val = rowSet.getObject(field.getApiName());
						if (val != null)
						{
							val = rowSet.getInt(field.getApiName());
						}
					}
					else if (numberDataType.getJavaType().equals(Long.class.getName()))
					{
						// need to check if value is null first, because getInt always return non-null values
						val = rowSet.getObject(field.getApiName());
						if (val != null)
						{
							val = rowSet.getLong(field.getApiName());
						}
					}
					else if (numberDataType.getJavaType().equals(Double.class.getName()))
					{
						// need to check if value is null first, because getDouble always return non-null values
						val = rowSet.getObject(field.getApiName());
						if (val != null)
						{
							val = rowSet.getDouble(field.getApiName());
						}
					}
					else
					{
						throw new DALException("Unsupported Java type " + numberDataType.getJavaType() + " for field " + fieldToSet);
					}
					record.setField(fieldToSet, val, this.envData);
				}
				else if (field.getDataTypeId().equals(DataType.MULTI_ENUMERATION))
				{
					Object val = rowSet.getObject(field.getApiName());
					
					if (val != null)
					{
						if (!(val instanceof SerialArray))
						{
							throw new DALException("Values of a multi-enum field should have Java type " + SerialArray.class.getName() + ", but have type " + val.getClass().getName());
						}
						
						try
						{
							Set<String> values = new HashSet<String>();
							CollectionUtils.addAll(values, (Object[])((SerialArray)val).getArray());
							val = values;
						}
						catch (SerialException e)
						{
							throw new DALException("Cannot get result set from serial array multi-enum value");
						}
					}
					
					record.setField(fieldToSet, val, this.envData);
				}
				else
				{
					record.setField(fieldToSet, rowSet.getObject(field.getApiName()), this.envData);
				}
			}
			catch (InvalidResultSetAccessException e)
			{
				// no such column found, but that's OK, probably the column was not included in the query
			}
		}
		
		if (criteria.getNestedProperties() != null)
		{
			for (String nestedProperty : criteria.getNestedProperties())
			{
				// Set the properties value to the field from the result set aliased by some columns.
				// E.g. property "parent.age" may be represented in the DB by columns "parentRef.ageField", so then
				// we need to use the latter value to reference it in the result set, because this is how it appeared
				// in the SELECT list of the query.
				String propertyPath = nestedProperty.substring(0, nestedProperty.lastIndexOf('.'));
				String dbColumn = criteria.getType().getField(nestedProperty, criteria.getEnv()).getDbColumn();
				String fullProperty = criteria.getPropertyAlias(propertyPath) + "." + dbColumn;
				
				Object value = null;
				try
				{
					value = rowSet.getObject(nestedProperty);
				}
				catch (InvalidResultSetAccessException e)
				{
					throw new KommetException("Error reading property " + fullProperty + " from result set. Nested: " + e.getMessage());
				}
				
				// if retrieved value is a serial array, which happens for inverse collections,
				// then we want to convert it to a simple Java array list
				if (value instanceof SerialArray)
				{
					try
					{
						List<Object> values = new ArrayList<Object>();
						
						// TODO - an issue to investigate - when inverse collection is empty, the
						// retrieved object array will contain one object instead of none.
						// This is why we need to put the condition below.
						
						// Find the "count" property that tells whether the collection is empty.
						// The alias of this property (nestedProperty.substring(0, nestedProperty.lastIndexOf('.'))
						// is created in method SelectQuery.execute()
						
						if (!rowSet.getBoolean(nestedProperty.substring(0, nestedProperty.lastIndexOf('.')) + "_empty"))
						{
							CollectionUtils.addAll(values, (Object[])((SerialArray)value).getArray());
						}
						
						// unfortunately as of now we remove the duplicates from the collections in Java, not at the query level
						// queries that use this feature (i.e. add an additional count() select to the query and then filter out the results here)
						// take 10ms longer to execute. These 10ms is a combined time of executing the query and filtering the results on two collections
						// (one with 2 elements, another one with 3 elements).
						// If we turn off the Java rewriting below, but keep the additional count() operations in the query, the whole operation lasts only 2ms longer on average.
						// This means that 80% of the longer execution if due to the Java rewriting below, and only 20% is due to the longer Postgres query execution.
						if (criteria.hasMultipleCollections())
						{
							// get the number of distinct items in this collection
							long uniqueItems = rowSet.getLong(nestedProperty.substring(0, nestedProperty.lastIndexOf('.')) + SelectQuery.DISTINCT_ITEMS_ALIAS_SUFFIX);
							
							values = removeDuplicatesFromCollection(values, uniqueItems);
						}
						
						value = values;
					}
					catch (SerialException e)
					{
						throw new KommetException("Error reading value for field " + nestedProperty + ". Nested: " + e.getMessage());
					}
				}
		
				try
				{
					record.setField(nestedProperty, value, this.envData);
				}
				catch (KommetException e)
				{
					e.printStackTrace();
					throw new KommetException("Error setting field " + nestedProperty + ": " + e.getMessage());
				}
			}
		}
		
		return record;
	}
	
	private List<Object> removeDuplicatesFromCollection(List<Object> values, long uniqueItems)
	{
		List<Object> filteredList = new ArrayList<Object>();
		for (int i = 0; i < values.size(); )
		{
			filteredList.add(values.get(i));
			i += uniqueItems;
		}
		
		return filteredList;
	}

	public Record getQueryResultFromRowSet (SqlRowSet rowSet, Criteria criteria) throws KommetException
	{
		Record record = getRecordFromRowSet(rowSet, criteria);
		QueryResult result = new QueryResult(record);
		
		for (AggregateFunctionCall aggregateFunction : criteria.getAggregateFunctions())
		{
			result.addAggregateValue(aggregateFunction.getStringName(), rowSet.getObject(aggregateFunction.getStringName()));
		}
		
		List<String> queriedProperties = new ArrayList<String>();
		queriedProperties.addAll(criteria.getProperties());
		queriedProperties.addAll(criteria.getNestedProperties());
		
		for (String groupByProperty : queriedProperties)
		{
			try
			{
				result.addGroupByValue(groupByProperty, rowSet.getObject(groupByProperty));
			}
			catch (InvalidResultSetAccessException e)
			{
				// check if the property is among the group by properties
				for (String prop : criteria.getGroupByProperties())
				{
					if (prop.equals(groupByProperty))
					{
						// the property was among the group by properties as it should,
						// so we don't know the real cause of the exception
						throw e;
					}
				}
				
				// we know the cause of the error - property was not listed in the group by clause
				throw new CriteriaException("Property " + groupByProperty + " was used in the select clause but not in the group by clause");
			}
		}
		
		return result;
	}
	
	public static TypePersistenceMapping get (Type type, EnvData env) throws KommetException
	{
		return new TypePersistenceMapping(type, env);
	}
	
	public Type getType()
	{
		return this.type;
	}
	
	public InsertQuery createInsertQuery(EnvData env)
	{
		return new InsertQuery(this, env);
	}
	
	public DeleteQuery createDeleteQuery(EnvData env)
	{
		return new DeleteQuery(this, env);
	}
	
	public UpdateQuery createUpdateQuery(EnvData env)
	{
		return new UpdateQuery(this, env);
	}
}