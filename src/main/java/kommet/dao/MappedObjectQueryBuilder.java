/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import kommet.auth.AuthData;
import kommet.dao.dal.DALSyntaxException;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.CriteriaException;
import kommet.dao.queries.Restriction;
import kommet.dao.queries.RestrictionOperator;
import kommet.dao.queries.SelectQuery;
import kommet.data.BasicModel;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.SpecialValue;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.exceptions.NotImplementedException;
import kommet.filters.BasicFilter;
import kommet.utils.MiscUtils;


public class MappedObjectQueryBuilder
{	
	public static String getDeleteQuery (TypePersistenceMapping mapping, KID recordId, AuthData authData, EnvData env, boolean forceAllowDelete) throws KommetException
	{
		StringBuilder query = new StringBuilder();
		query.append("UPDATE " + mapping.getTable() + " SET " + Field.LAST_MODIFIED_BY_FIELD_DB_COLUMN + " = '" + authData.getUserId() + "'");
		
		// We want to let the check_edit_permissions method know that editAll is set
		// so we do this by using the special field "triggerflag"
		// It will be read by the check_edit_permissions and tell it not to check edit permissions.
		if (forceAllowDelete || authData.canDeleteAllType(mapping.getType().getKID(), true, env))
		{
			query.append(", ").append(Field.TRIGGER_FLAG_DB_COLUMN + " = 'EDITDELETEALL'");
		}
		
		query.append(" WHERE + " + Field.ID_FIELD_DB_COLUMN + " = '" + recordId.getId() + "'; ");
		query.append("DELETE FROM " + mapping.getTable() + " WHERE " + Field.ID_FIELD_DB_COLUMN + " = '" + recordId.getId() + "'");
		
		// Wrap query in procedure execute_update().
		// The only reason why we do this is that we want to intercept the exception thrown by
		// the edit_permissions_check() trigger and return a custom error, instead of propagating
		// the SQL error, because that would cause the whole Spring transaction to stop.
		// Instead of stopping the transaction, we just want to catch the error and handle it in Java code.
		return "SELECT execute_update('" + query.toString().replace("'", "''") + "')";
	}
	
	public static <T extends Record> String getBulkDeleteQuery (TypePersistenceMapping mapping, Collection<T> objs, AuthData authData, EnvData env, boolean forceAllowDelete) throws KommetException
	{
		List<KID> ids = MiscUtils.getKIDList(objs);
		String wrappedQuery = null;
		
		boolean isEditDeleteAll = forceAllowDelete || authData.canDeleteAllType(mapping.getType().getKID(), true, env); 
		
		if (isEditDeleteAll)
		{
			wrappedQuery = "SELECT delete_record('" + MiscUtils.implode(ids, ",") + "', 1)";
		}
		else
		{
			StringBuilder query = new StringBuilder();
			query.append("UPDATE " + mapping.getTable() + " SET " + Field.LAST_MODIFIED_BY_FIELD_DB_COLUMN + " = '" + authData.getUserId() + "' ");
			
			// We want to let the check_edit_permissions method know that editAll is set
			// so we do this by using the special field "triggerflag"
			// It will be read by the check_edit_permissions and tell it not to check edit permissions.
			// query.append(", ").append(Field.TRIGGER_FLAG_DB_COLUMN + " = 'EDITDELETEALL'");
			
			query.append(" WHERE " + Field.ID_FIELD_DB_COLUMN + " IN (" + MiscUtils.implode(ids, ",", "'") + "); ");
			query.append("DELETE FROM " + mapping.getTable() + " WHERE " + Field.ID_FIELD_DB_COLUMN + " IN (" + MiscUtils.implode(ids, ",", "'") + ")");
			
			// Wrap query in procedure execute_update().
			// The only reason why we do this is that we want to intercept the exception thrown by
			// the edit_permissions_check() trigger and return a custom error, instead of propagating
			// the SQL error, because that would cause the whole Spring transaction to stop.
			// Instead of stopping the transaction, we just want to catch the error and handle it in Java code.
			
			wrappedQuery = "SELECT execute_update('" + query.toString().replace("'", "''") + "')";
		}
		
		return wrappedQuery;
	}
	
	public static String getBulkDeleteQueryByIds (TypePersistenceMapping mapping, Collection<KID> ids, AuthData authData, EnvData env, boolean forceAllowDelete) throws KommetException
	{
		StringBuilder query = new StringBuilder();
		query.append("UPDATE " + mapping.getTable() + " SET " + Field.LAST_MODIFIED_BY_FIELD_DB_COLUMN + " = '" + authData.getUserId() + "'");
		
		// We want to let the check_edit_permissions method know that editAll is set
		// so we do this by using the special field "triggerflag"
		// It will be read by the check_edit_permissions and tell it not to check edit permissions.
		if (forceAllowDelete || authData.canDeleteAllType(mapping.getType().getKID(), true, env))
		{
			query.append(", ").append(Field.TRIGGER_FLAG_DB_COLUMN + " = 'EDITDELETEALL'");
		}
		
		query.append(" WHERE " + Field.ID_FIELD_DB_COLUMN + " IN (" + MiscUtils.implode(ids, ",", "'") + "); ");
		query.append("DELETE FROM " + mapping.getTable() + " WHERE " + Field.ID_FIELD_DB_COLUMN + " IN (" + MiscUtils.implode(ids, ",", "'") + ")");
		
		// Wrap query in procedure execute_update().
		// The only reason why we do this is that we want to intercept the exception thrown by
		// the edit_permissions_check() trigger and return a custom error, instead of propagating
		// the SQL error, because that would cause the whole Spring transaction to stop.
		// Instead of stopping the transaction, we just want to catch the error and handle it in Java code.
		return "SELECT execute_update('" + query.toString().replace("'", "''") + "')";
	}
	
	public static <T extends Record> String getCountQuery (PersistenceMapping mapping, BasicFilter<T> filter)
	{
		StringBuilder sb = new StringBuilder("SELECT COUNT(id) FROM " + mapping.getTable());
		sb.append(getWhereClause(filter));
		return sb.toString();
	}
	
	public static String getInsertQuery (TypePersistenceMapping mapping, Record record) throws KommetException
	{
		StringBuilder sb = new StringBuilder("INSERT INTO " + mapping.getTable() + "(");
		
		List<String> columnNames = new ArrayList<String>();
		List<String> columnValues = new ArrayList<String>();
		
		Type type = mapping.getType();
		
		for (ColumnMapping colMapping : mapping.getColumnMappings().values())
		{
			// set the field, unless its data type is transient
			if (type.getField(colMapping.getProperty()).getDataType().isTransient())
			{
				continue;
			}
			
			// null values are skipped
			// if we want to nullify a column, we need to use SpecialValue.NULL
			if (record.attemptGetField(colMapping.getProperty()) != null)
			{
				columnNames.add(colMapping.getColumn());
				
				try
				{
					columnValues.add((record.getFieldValueForPostgres(colMapping.getProperty())));
				}
				catch (Exception e)
				{
					throw new KommetException("Error reading value for property '" + colMapping.getProperty() + "'. Nested: " + e.getMessage());
				}
			}
		}
		
		sb.append(MiscUtils.implode(columnNames, ", ", null) + ") VALUES (");
		sb.append(MiscUtils.implode(columnValues, ", ", null));
		sb.append(") RETURNING kid");
		
		return "SELECT execute_insert('" + sb.toString().replace("'", "''") + "')";
	}
	
	public static <T extends BasicModel<Long>> String getUpdateQuery (TypePersistenceMapping mapping, Record record, Criteria criteria, AuthData authData, boolean forceAllowEdit) throws KommetException
	{
		StringBuilder query = new StringBuilder("UPDATE " + mapping.getTable() + " SET ");
		
		List<String> setClauses = new ArrayList<String>();
		
		Type type = mapping.getType();
		
		for (ColumnMapping colMapping : mapping.getColumnMappings().values())
		{
			Object propertyValue = record.attemptGetField(colMapping.getProperty());
			
			if (propertyValue == null)
			{
				// do not update non-set properties
				continue;
			}
			
			// only nullify objects if they are explicitly set to special null value
			if (SpecialValue.NULL.equals(propertyValue))
			{
				propertyValue = null;
			}
			
			Field field = type.getField(colMapping.getProperty());
			
			// set the field, unless its data type is transient
			if (!field.getDataType().isTransient())
			{
				setClauses.add(colMapping.getColumn() + " = " + field.getDataType().getPostgresValue(propertyValue));
			}
		}
		
		// We want to let the check_edit_permissions method know that editAll is set
		// so we do this by using the special field "triggerflag"
		// It will be read by the check_edit_permissions and tell it not to check edit permissions.
		if (forceAllowEdit || authData.canEditAllType(criteria.getType().getKID(), true, criteria.getEnv()))
		{
			setClauses.add(Field.TRIGGER_FLAG_DB_COLUMN + " = 'EDITALL'");
		}
		
		query.append(MiscUtils.implode(setClauses, ", ", null));
		
		// TODO implement where conditions from criteria
		if (criteria != null && criteria.isNotEmpty())
		{
			query.append(" WHERE ");
			query.append(buildCriteriaSQL(criteria, mapping));
		}
		
		// Wrap query in procedure execute_update().
		// The only reason why we do this is that we want to intercept the exception thrown by
		// the edit_permissions_check() trigger and return a custom error, instead of propagating
		// the SQL error, because that would cause the whole Spring transaction to stop.
		// Instead of stopping the transaction, we just want to catch the error and handle it in Java code.
		return "SELECT execute_update('" + query.toString().replace("'", "''") + "')";
	}
	
	/**
	 * Creates Postgres SQL where clause for the given criteria and the given object mapping.
	 * @param criteria
	 * @param mapping
	 * @return
	 * @throws KommetException 
	 */
	public static String buildCriteriaSQL (Criteria criteria, TypePersistenceMapping mapping) throws KommetException
	{
		if (criteria.getBaseRestriction() != null)
		{
			return buildRestrictionSQL(criteria.getBaseRestriction(), mapping, criteria);
		}
		else
		{
			return null;
		}
		//first-level condition does not need to be enclosed in brackets
		//return MiscUtils.trimLeft(MiscUtils.trimRight(sql, ')'), '(');
	}

	private static String buildRestrictionSQL (Restriction restriction, TypePersistenceMapping mapping, Criteria parentCriteria) throws KommetException
	{
		// check if the operator is a simple arithmetic operator >, <, =, >= or <=, IN, LIKE, ILIKE
		String simpleOperator = getBinaryOperator(restriction.getOperator());
		
		// if it's an arithmetic operator, handle it
		if (simpleOperator != null)
		{
			// get the name under which the property is represented in the SQL query
			String propertySQL = getPropertySQL(restriction.getProperty(), parentCriteria, mapping);
			
			// if it's a nested property, add its SQL to the map, so that we know how to reference its
			// value in the result set
			if (restriction.getProperty().contains("."))
			{
				parentCriteria.addPropertyAlias(restriction.getProperty(), propertySQL);
			}
			
			String sql = propertySQL + " " + simpleOperator + " ";
			Field field = mapping.getType().getField(restriction.getProperty(), parentCriteria.getEnv());
			if (field == null)
			{
				throw new CriteriaException("Invalid property " + restriction.getProperty() + " on object " + mapping.getType().getApiName());
			}
			
			// if it's an IN operator, treat it differently
			if (!RestrictionOperator.IN.equals(restriction.getOperator()))
			{	
				try
				{
					if ((RestrictionOperator.ILIKE.equals(restriction.getOperator()) || RestrictionOperator.LIKE.equals(restriction.getOperator())) && field.getDataType().getId().equals(DataType.KOMMET_ID))
					{
						sql += (String)restriction.getValue();
					}
					else
					{
						sql += field.getDataType().getPostgresValue(restriction.getValue());
					}
				}
				catch (KommetException e)
				{
					throw new KommetException("Error creating SQL for criteria for property " + restriction.getProperty() + ": " + e.getMessage(), e);
				}
			}
			else
			{
				// the IN operator can have two types of arguments - a list of values, or a subquery (a criteria object)
				if (restriction.getValue() != null)
				{
					if (restriction.getValue() instanceof Criteria)
					{
						Criteria subqueryCriteria = (Criteria)restriction.getValue();
						validateSubquery(subqueryCriteria, parentCriteria.getType().getField(restriction.getProperty(), parentCriteria.getEnv()));
						
						// generate SQL for the subquery and append it to the actual parent query
						sql += "(" + SelectQuery.buildFromCriteria(subqueryCriteria, subqueryCriteria.getNestedProperties(), parentCriteria.getEnv()).getSqlQuery() + ")";
					}
					else
					{
						throw new KommetException("Invalid type " + restriction.getValue().getClass().getName() + " of IN restriction argument");
					}
				}
				// if the argument of the IN restriction is a list of values
				else
				{
					// build a list of values enclosed by a bracket for the IN operator
					List<String> postgresValues = new ArrayList<String>();
					for (Object value : restriction.getValues())
					{
						postgresValues.add(field.getDataType().getPostgresValue(value));
					}
					sql += "(" + MiscUtils.implode(postgresValues, ",") + ")";
				}
			}
			
			return "(" + sql + ")";
		}
		else if (restriction.getOperator().equals(RestrictionOperator.AND))
		{
			List<String> subconditions = new ArrayList<String>();
			for (Restriction r : restriction.getSubrestrictions())
			{
				subconditions.add(buildRestrictionSQL(r, mapping, parentCriteria));
			}
			return "(" + MiscUtils.implode(subconditions, " AND ", null) + ")";
		}
		else if (restriction.getOperator().equals(RestrictionOperator.OR))
		{
			List<String> subconditions = new ArrayList<String>();
			for (Restriction r : restriction.getSubrestrictions())
			{
				subconditions.add(buildRestrictionSQL(r, mapping, parentCriteria));
			}
			return "(" + MiscUtils.implode(subconditions, " OR ", null) + ")";
		}
		else if (restriction.getOperator().equals(RestrictionOperator.NOT))
		{	
			if (restriction.getSubrestrictions() == null || restriction.getSubrestrictions().isEmpty())
			{
				throw new CriteriaException("NOT restriction must have at least one subrestriction");
			}
			else if (restriction.getSubrestrictions().size() > 1)
			{
				throw new CriteriaException("NOT restriction has " + restriction.getSubrestrictions().size() + " subrestrictions, should have exactly 1");
			}
			
			String conditionSQL = buildRestrictionSQL(restriction.getSubrestrictions().get(0), mapping, parentCriteria);
			return "(NOT (" + conditionSQL + "))";
		}
		else if (restriction.getOperator().equals(RestrictionOperator.ISNULL))
		{
			return "(" + getPropertySQL(restriction.getProperty(), parentCriteria, mapping) + " ISNULL)";
		}
		else
		{
			throw new NotImplementedException("Illegal operator " + restriction.getOperator() + ". Operators other than EQ not supported in criteria");
		}
	}
	
	private static void validateSubquery(Criteria subcriteria, Field restrictionField) throws KommetException
	{
		// make sure that the only property returned by the subcriteria matches the field data type of the field of the IN operator
		Field singleSubqueryField = subcriteria.getSelectedFieldForSubquery();
		if (singleSubqueryField == null)
		{
			throw new DALSyntaxException("More than one field in subquery");
		}
		
		if (singleSubqueryField.getDataTypeId().equals(DataType.FORMULA))
		{
			// TODO unit test for this
			throw new DALSyntaxException("Field " + singleSubqueryField.getApiName() + " cannot be queries in subquery because it is a formula field");
		}
		
		if (restrictionField.getDataTypeId().equals(DataType.FORMULA))
		{
			// TODO unit test for this
			throw new DALSyntaxException("Field " + singleSubqueryField.getApiName() + " cannot be used with IN operator because it is a formula field");
		}
		
		if (!restrictionField.getDataTypeId().equals(singleSubqueryField.getDataTypeId()))
		{
			throw new DALSyntaxException("Data type of field queried in subcriteria does not match the field in the IN condition"); 
		}
	}

	public static String getPropertySQL (String property, Criteria criteria, PersistenceMapping mapping) throws KommetException
	{
		String quote = criteria.isQuoteTableAndColumnNames() ? "\"" : "";
		String alias = criteria.isUseMainTableAlias() ? Criteria.MAIN_TABLE_ALIAS + "." : "";
		
		// treat nested properties that reference only the ID of the type reference object (e.g. "child.id") as simple
		// properties because for them in fact no joins are necessary.
		if (property.endsWith("." + Field.ID_FIELD_NAME) && property.substring(0, property.length() - ("." + Field.ID_FIELD_NAME).length()).indexOf(".") == -1)
		{
			// get the nested property of the property, e.g. for "company.id" this will get "id"
			String objectRefProperty = property.substring(0, property.indexOf('.'));
			
			// if it is a type reference field, convert to simple property, because only the most
			// simple property of the nested property will be put into the SELECT clause
			if (criteria.getType().getField(objectRefProperty).getDataTypeId().equals(DataType.TYPE_REFERENCE))
			{
				// if it's an type reference field, get the type of the type of the reference object
				// to see if we need to apply sharings to it
				Type refType = ((TypeReference)criteria.getType().getField(objectRefProperty).getDataType()).getType();
				
				if (!criteria.isApplySharings(refType.getKID()))
				{
					property = objectRefProperty;
				}
			}
		}
		
		// if it is a nested property, split by last dot, and get the alias for the relationship.
		if (property.contains("."))
		{
			String relationship = property.substring(0, property.lastIndexOf('.'));
			
			// use lower case alias
			alias = criteria.getPropertyAlias(relationship);
			
			if (alias == null)
			{
				throw new KommetException("No alias found for property " + relationship + ". Aliases must be added both for properties in conditions and in the select field list. All aliases defined are " + MiscUtils.implode(criteria.getAliasesToProperties().keySet(), ", "));
			}
			
			alias = alias.toLowerCase();
			
			Field field = criteria.getType().getField(property, criteria.getEnv());
			if (field == null)
			{
				throw new DALSyntaxException("No field " + property + " found on type " + criteria.getType().getApiName());
			}

			return field.getSQL(alias, quote);
		}
		else
		{
			Field field = criteria.getType().getField(property);
			if (field == null)
			{
				throw new DALSyntaxException("No field " + property + " found on object " + criteria.getType().getApiName());
			}
			
			return field.getSQL(criteria.isUseMainTableAlias() ? Criteria.MAIN_TABLE_ALIAS : null, quote);
			
			/*ColumnMapping colMapping = mapping.getPropertyMappings().get(property);
			if (colMapping == null)
			{
				throw new KommetException("Cannot find column mapping for property " + property + " on type " + criteria.getType().getQualifiedName());
			}
			
			return criteria.isUseMainTableAlias() ? Criteria.MAIN_TABLE_ALIAS + "." + colMapping.getColumn() : colMapping.getColumn();*/
		}
	
	}

	private static String getBinaryOperator(RestrictionOperator operator)
	{
		switch (operator)
		{
			case EQ: return "=";
			case GT: return ">";
			case GE: return ">=";
			case LT: return "<";
			case LE: return "<=";
			case NE: return "<>";
			case IN: return "IN";
			case LIKE: return "LIKE";
			case ILIKE: return "ILIKE";
			default: return null;
		}
	}

	private static <T> String getWhereClause (BasicFilter<T> filter)
	{
		if (filter == null)
		{
			filter = new BasicFilter<T>();
		}
		
		List<String> conditions = new ArrayList<String>();
		
		return conditions.isEmpty() ? "" : "WHERE " + MiscUtils.implode(conditions, " AND ", null);
	}
}