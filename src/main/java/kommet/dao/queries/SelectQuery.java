/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.util.StringUtils;

import kommet.dao.MappedObjectQueryBuilder;
import kommet.dao.TypePersistenceMapping;
import kommet.dao.dal.AggregateFunctionCall;
import kommet.dao.dal.DALSyntaxException;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.DataType;
import kommet.env.EnvData;
import kommet.utils.DataUtil;
import kommet.utils.MiscUtils;

public class SelectQuery extends NativeDbQuery
{	
	private String sqlQuery;
	private Criteria criteria;

	private static final String SHARING_TABLE = "userrecordsharing";
	public static final String DISTINCT_ITEMS_ALIAS_SUFFIX = "_distinct_items";
	
	public String getSqlQuery()
	{
		return sqlQuery;
	}

	public void setSqlQuery(String sqlQuery)
	{
		this.sqlQuery = sqlQuery;
	}
	
	public SelectQuery(TypePersistenceMapping objMapping, Criteria criteria)
	{
		super(objMapping, criteria.getEnv());
		this.criteria = criteria;
	}

	public List<Record> execute() throws KommetException
	{	
		if (!StringUtils.hasText(this.sqlQuery))
		{
			throw new KommetException("SQL select query is empty");
		}
		
		// if no auth data is passed, it means we want to run in admin mode - all records and types are accessible
		/*if (authData != null && !authData.canReadType(criteria.getType().getKID(), false, getEnvData()))
		{
			throw new InsufficientPrivilegesException("Insufficient privileges to query type " + MiscUtils.envSpecificBasePackageToUserPackage(criteria.getType().getQualifiedName(), getEnvData()));
		}*/
		
		SqlRowSet rowSet = getEnv().getJdbcTemplate().queryForRowSet(this.sqlQuery);
		
		List<Record> records = new ArrayList<Record>();
		TypePersistenceMapping mapping = getTypeMapping();
		
		if (this.criteria.getAggregateFunctions().isEmpty() && !this.criteria.isGrouped())
		{
			while (rowSet.next())
			{
				records.add(mapping.getRecordFromRowSet(rowSet, this.criteria));
			}
		}
		else
		{
			while (rowSet.next())
			{
				records.add(mapping.getQueryResultFromRowSet(rowSet, this.criteria));
			}
		}
		
		return records;
	}

	/**
	 * Build native SQL query from criteria.
	 * @param criteria
	 * @param nestedProperties
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public static SelectQuery buildFromCriteria(Criteria criteria, Collection<String> nestedProperties, EnvData env) throws KommetException
	{
		StringBuilder sql = new StringBuilder("SELECT ");
		
		if (criteria.isSubquery())
		{
			if (!criteria.isIdExplicitlyAddedToSelectClause())
			{
				criteria.getProperties().remove(Field.ID_FIELD_NAME);
			}
			
			if ((criteria.getProperties().size() + criteria.getNestedProperties().size()) > 1)
			{
				throw new DALSyntaxException("More than one field in subquery");
			}
		}
		
		// SQL column expressions used in the SELECT clause
		List<String> columns = new ArrayList<String>();
		
		String quote = criteria.isQuoteTableAndColumnNames() ? "\"" : "";
		String aliasQuote = "\"";
		
		Grouping grouping = Grouping.NONE;
		if (!criteria.getInverseCollectionProperties().isEmpty() || !criteria.getAssociationProperties().isEmpty())
		{
			//hasMultipleCollections = (criteria.getInverseCollectionProperties().size() + criteria.getAssociationProperties().size()) > 1;
			
			// If more than one collection is queries, then because of multiple joins we will get duplicate rows.
			// By adding a distinct keyword we make sure that each value combination will be retrieved exactly once.
			// This however assumes, that neither inverse collections nor associations will contain duplicates.
			
			// if the criteria contains inverse collections or associations, an aggregate function will be used to group
			// their values, so all other properties will need to be listed in the "group by" clause.
			grouping = Grouping.ALL_NON_COLLECTION_FIELDS;
		}
		
		Set<String> userGroupByFieldSet = new HashSet<String>();
		
		if (criteria.isGrouped())
		{
			grouping = Grouping.USER_DEFINED_GROUP_BY_FIELDS;
			userGroupByFieldSet.addAll(criteria.getGroupByProperties());
		}
		
		List<String> groupByFields = new ArrayList<String>();
		
		// If some order direction is specified in the criteria, use it, otherwise order by KID.
		// Some ordering has to be used if inverse collections or associations are fetched, because values of the
		// aggregate function array_agg need to be sorted
		List<OrderBy> orderings = null;
		boolean hasCriteriaOrderings = false;
		
		if ((criteria.getOrderings() != null && !criteria.getOrderings().isEmpty()))
		{
			orderings = new ArrayList<OrderBy>();
			for (OrderBy ordering : criteria.getOrderings())
			{
				// translate order by properties to their SQL representation
				
				// nested properties are not allowed, unless they are type reference IDs
				if (ordering.getProperty().contains("."))
				{
					ordering.setPropertySQL(MappedObjectQueryBuilder.getPropertySQL(ordering.getProperty(), criteria, env.getTypeMapping(criteria.getType().getKID())));
					
					// Column names can have table and column names quoted, e.g. "table"."column", but aliases
					// are all wrapped in quotes ("table.column"), so we must get rid of those internal quotes
					// otherwise we would end up with ""table"."column""
					//alias = propertySQL.replaceAll("\"", "");
					
					//throw new DALSyntaxException("Invalid order by property " + ordering.getProperty() + ". ORDER BY clause can only contain direct properties of the selected type " + MiscUtils.userPackageToEnvSpecificBasePackage(criteria.getType().getQualifiedName(), env));
				}
				else
				{
					Field orderByField = criteria.getType().getField(ordering.getProperty());
					
					if (orderByField == null)
					{
						throw new DALSyntaxException("Unknown property " + ordering.getProperty());
					}
					
					// do not allow ordering by field that don't have DB representation
					if (!Field.hasDatabaseRepresenation(orderByField.getDataType()))
					{
						throw new DALSyntaxException("Field " + ordering.getProperty() + " of type " + orderByField.getDataType().getName() + " cannot be used in the ORDER BY clause");
					}
					
					ordering.setPropertySQL(quote + Criteria.MAIN_TABLE_ALIAS + quote + "." + quote + orderByField.getDbColumn() + quote);
				}
				
				orderings.add(ordering);
				
				if (grouping == Grouping.ALL_NON_COLLECTION_FIELDS)
				{
					// if grouping is applied to all simple properties, it also needs to be applied to the "order by" properties
					groupByFields.add(ordering.getPropertySQL());
				}
			}
			hasCriteriaOrderings = true;
		}
		else if (grouping == Grouping.ALL_NON_COLLECTION_FIELDS)
		{
			orderings = new ArrayList<OrderBy>();
			// sort by KID starting with the newest record
			orderings.add(new OrderBy(SortDirection.DESC, Criteria.MAIN_TABLE_ALIAS + "." + Field.ID_FIELD_DB_COLUMN, Criteria.MAIN_TABLE_ALIAS + "." + Field.ID_FIELD_DB_COLUMN));
		}
		
		// add aggregate functions to the query
		if (!criteria.getAggregateFunctions().isEmpty())
		{
			for (AggregateFunctionCall function : criteria.getAggregateFunctions())
			{
				String aggregatePropTypeAlias = Criteria.MAIN_TABLE_ALIAS;
				
				if (function.getProperty().contains("."))
				{
					// nested properties are not allowed in aggregate functions
					if (DataUtil.isCollection(function.getProperty(), criteria.getType()))
					{
						//throw new KommetException("Property " + function.getProperty() + " cannot be used in aggregate function because it is a collection");
					}
					
					// get the whole nested property without the last part
					aggregatePropTypeAlias = criteria.getPropertyAlias(function.getProperty().substring(0, function.getProperty().lastIndexOf('.')));
				}
				
				// get the field by which records are sorted
				Field field = criteria.getType().getField(function.getProperty(), env);
				
				if (field == null)
				{
					throw new DALSyntaxException("No field " + function.getProperty() + " found on type " + criteria.getType().getQualifiedName());
				}
				
				String sqlName = function.getFunction().getSQL() + "(" + quote + aggregatePropTypeAlias + quote + "." + quote + field.getDbColumn() + quote + ")";
				
				// the alias of an aggregate function is exactly the same as the one specified in the DAL e.g. "COUNT(id)"
				columns.add(sqlName + " AS \"" + function.getStringName() + "\"");
			}
		}
		
		// add simple (non-nested) properties to the select list
		for (String property : criteria.getProperties())
		{
			Field field = criteria.getType().getField(property);
			
			if (field == null)
			{
				throw new DALSyntaxException("No field " + property + " found on type " + criteria.getType().getQualifiedName());
			}
			
			String sqlName = field.getSQL(Criteria.MAIN_TABLE_ALIAS, quote);
			
			// add alias for property - convert to lower case
			columns.add(sqlName + " AS \"" + property.toLowerCase() + "\"");
			
			// grouping is applied to this property in two cases: if it is applied to all non-collection
			// properties, or if user grouping has been defined for this property
			if (grouping == Grouping.ALL_NON_COLLECTION_FIELDS || (grouping == Grouping.USER_DEFINED_GROUP_BY_FIELDS && userGroupByFieldSet.contains(property)))
			{
				groupByFields.add(sqlName);
			}
		}
		
		if (nestedProperties != null)
		{
			// Set of nested collection names for which a count property has been added.
			// E.g. if we are queried child collection father.children, selecting two fields:
			// father.children.name and father.children.age, then we will create a count property
			// for this collection (count(father.children.id) = 0 as ...).
			// To mark that a count property has been added for the "children" collection, we will put
			// collection name "father.children" into the collectionsWithCountClause set.
			Set<String> collectionsWithCountClause = new HashSet<String>();
			
			Map<String, String> collectionAliases = new HashMap<String, String>();
			
			// add nested properties to the select list
			for (String nestedProperty : nestedProperties)
			{
				String col = MappedObjectQueryBuilder.getPropertySQL(nestedProperty, criteria, env.getTypeMapping(criteria.getType().getKID()));
				
				// Column names can have table and column names quoted, e.g. "table"."column", but aliases
				// are all wrapped in quotes ("table.column"), so we must get rid of those internal quotes
				// otherwise we would end up with ""table"."column""
				String alias = nestedProperty.toLowerCase();
				
				// if it is a collection
				if (criteria.isInverseCollection(nestedProperty) || criteria.isAssociation(nestedProperty))
				{
					if (!criteria.isSubquery())
					{
						// if nested property is e.g. "father.children.age", then collection name will
						// be created by removing the last nested property, e.g. "age", and thus the
						// collection name will be "father.children"
						String collectionName = nestedProperty.substring(0, nestedProperty.lastIndexOf("."));
						String columnAlias = null;
						
						if (!collectionsWithCountClause.contains(collectionName))
						{
							collectionsWithCountClause.add(collectionName);
							
							// for every aggregate array, add a count of the ID fields because they are the
							// only ones which are guaranteed to be not empty.
							//
							// TODO this is suboptimal because getPropertySQL() is a complicated method
							// and it is called twice - once here to create the "count" property, another
							// time when the actual childCollection.id field is queried.
							// We should use the column SQL generated when childCollection.id is added to
							// the SQL.
							columnAlias = MappedObjectQueryBuilder.getPropertySQL(collectionName + "." + Field.ID_FIELD_NAME, criteria, env.getTypeMapping(criteria.getType().getKID()));
							collectionAliases.put(collectionName, columnAlias);
							
							String countExpr = "count(" + columnAlias + ")";
							
							columns.add(countExpr + " = 0 AS " + aliasQuote + collectionName + "_empty" + aliasQuote);
							
							if (criteria.hasMultipleCollections())
							{
								String countDistinctExpr = "count(distinct " + columnAlias + ")";
								
								// number of different items expected in this collection
								// use case when to avoid division by 0
								columns.add("CASE WHEN " + countExpr + " > 0 THEN " + countExpr + " / " + countDistinctExpr + " ELSE 0 END AS " + aliasQuote + collectionName + DISTINCT_ITEMS_ALIAS_SUFFIX + aliasQuote);
							}
						}
						
						// create an order by clause
						StringBuilder orderBy = new StringBuilder(" order by ");
						orderBy.append(implodeOrderings(orderings));
						
						if (criteria.hasMultipleCollections())
						{
							if (columnAlias == null)
							{
								columnAlias = MappedObjectQueryBuilder.getPropertySQL(collectionName + "." + Field.ID_FIELD_NAME, criteria, env.getTypeMapping(criteria.getType().getKID()));
							}
							orderBy.append(", ").append(columnAlias);
						}
						
						// if it's an inverse property, then we need to aggregate the results for its fields
						
						// this is the way it was done earlier - by simply aggregating results
						// however, when the collection is empty, this resulted in an array with one null value {NULL} instead of expected empty array {}
						// so we need to wrap it in CASE clause
						// col = "array_agg(" + col + orderBy.toString() + ")";
						Field nestedField = env.getTypeMapping(criteria.getType().getKID()).getType().getField(nestedProperty, env);
						// this is actually only needed for BigDecimal types
						col = "CASE WHEN count(" + collectionAliases.get(collectionName) + ") = 0 THEN ARRAY[]::" + DataType.getPostgresType(nestedField.getDataType()) + "[] ELSE array_agg(" + col + orderBy.toString() + ") END";
					}
				}
				else if (grouping == Grouping.ALL_NON_COLLECTION_FIELDS)
				{
					groupByFields.add(col);
				}
				
				columns.add(col + " AS " + aliasQuote + alias + aliasQuote);
			}
		}
		
		if (columns.isEmpty())
		{
			throw new CriteriaException("No properties specified to be retrieved by criteria");
		}
		
		// when type tables are joined with the sharing tables, this counter will be suffixed
		// to the sharing table alias to make it unique, since there potentially be multiple joins
		// with the same sharing table
		int sharingTableAliasCounter = 0;
		
		// the name of the DB table representing object UserRecordSharing
		String sharingTable = null;
		
		sharingTable = SHARING_TABLE;
		
		// ID of the user issuing the DAL query
		KID queryingUserId = criteria.getAuthData() != null ? criteria.getAuthData().getUserId() : null;
		
		// tells whether sharings should be applied to the query's base type
		boolean applyMainTypeSharings = criteria.isApplySharings(criteria.getType().getKID());
		
		// check if user-record sharing rules should be applied to this query
		// if not, all records will be returned regardless of sharings 
		if (applyMainTypeSharings && criteria.getAuthData() == null)
		{
			throw new KommetException("Auth data not passed to criteria object");
		}
		
		sql.append(MiscUtils.implode(columns, ", ", null)).append(" FROM ");
		if (applyMainTypeSharings)
		{
			sql.append("(");
		}
		
		sql.append(criteria.getType().getDbTable()).append(" AS ").append(Criteria.MAIN_TABLE_ALIAS).append(" ");
		
		// if user-record sharing should be applied
		if (applyMainTypeSharings)
		{
			// join this table with the user record sharing table
			sql.append(joinWithSharingTable(criteria.getType(), Criteria.MAIN_TABLE_ALIAS, sharingTable, queryingUserId, sharingTableAliasCounter));
			sharingTableAliasCounter++;
		}
		
		// add joins
		for (SqlJoinStructure joinStruct : criteria.getJoins())
		{
			if (joinStruct instanceof SqlJoin)
			{
				SqlJoin join = (SqlJoin)joinStruct;
				// if user-record sharing should be applied, join this table with the user record sharing table
				if (criteria.isApplySharings(join.getJoinedType().getKID()))
				{
					sql.append(join.getSQL(joinWithSharingTable(join.getJoinedType(), join.getRightTableAlias(), sharingTable, queryingUserId, sharingTableAliasCounter)));
					sharingTableAliasCounter++;
				}
				else
				{
					sql.append(join.getSQL((String[])null));
				}
			}
			else if (joinStruct instanceof AssociationJoin)
			{
				AssociationJoin join = (AssociationJoin)joinStruct;
				String linkingTableSharing = null;
				String associatedTableSharing = null;
				
				if (criteria.isApplySharings(join.getLinkingType().getKID()))
				{
					linkingTableSharing = joinWithSharingTable(join.getLinkingType(), join.getLinkingTableAlias(), sharingTable, queryingUserId, sharingTableAliasCounter);
					sharingTableAliasCounter++;
				}
				if (criteria.isApplySharings(join.getAssociatedType().getKID()))
				{
					associatedTableSharing = joinWithSharingTable(join.getAssociatedType(), join.getAssociatedTableAlias(), sharingTable, queryingUserId, sharingTableAliasCounter);
					sharingTableAliasCounter++;
				}
				
				sql.append(join.getSQL(linkingTableSharing, associatedTableSharing));
			}
			else
			{
				throw new KommetException("Unsupported implementation of SqlJoin: " + joinStruct.getClass().getName());
			}
			
			sql.append(" ");
		}
		
		if (criteria.isNotEmpty())
		{
			sql.append(" WHERE ").append(MappedObjectQueryBuilder.buildCriteriaSQL(criteria, env.getTypeMapping(criteria.getType().getKID())));
		}
		
		if (criteria.isGrouped())
		{
			List<String> groupByCols = new ArrayList<String>();
			for (String groupByProp : criteria.getGroupByProperties())
			{
				// TODO consider caching column definitions for properties instead of calling
				// MappedObjectQueryBuilder.getPropertySQL multiple times for the same property
				groupByCols.add(MappedObjectQueryBuilder.getPropertySQL(groupByProp, criteria, env.getTypeMapping(criteria.getType().getKID())));
			}
			// add user-defined group by properties before all system group by properties
			groupByFields.addAll(0, groupByCols);
		}
		
		if (!groupByFields.isEmpty())
		{
			sql.append(" GROUP BY ").append(MiscUtils.implode(groupByFields, ", "));
		}
		
		if (hasCriteriaOrderings)
		{
			// order by clause is added only if the orderings come from the criteria (i.e. have been
			// defined by the user), not when they are artificial orderings used for inverse collections
			sql.append(" ORDER BY ").append(implodeOrderings(orderings));
		}
		
		if (criteria.getLimit() != null)
		{
			sql.append(" LIMIT ");
			sql.append(criteria.getLimit());
		}
		
		if (criteria.getOffset() != null)
		{
			sql.append(" OFFSET ");
			sql.append(criteria.getOffset());
		}
		
		SelectQuery query = new SelectQuery(env.getTypeMapping(criteria.getType().getKID()), criteria);
		query.sqlQuery = sql.toString(); 
		return query;
	}

	/**
	 * Adds an SQL JOIN between some type table represented by tableAlias and the user-record sharing table,
	 * restricting it to the querying user.
	 * @param sql
	 * @param tableAlias
	 * @param sharingTable
	 * @param userId
	 * @param sharingTableAliasCounter
	 */
	private static String joinWithSharingTable(Type recordType, String tableAlias, String sharingTable, KID userId, int sharingTableAliasCounter)
	{
		StringBuilder sql = new StringBuilder();
		sql.append(" INNER JOIN ").append(sharingTable);
		sql.append(" AS ").append(sharingTable).append("_").append(sharingTableAliasCounter);
		
		if (recordType.getSharingControlledByFieldId() == null)
		{
			// user regular sharing settings from URS
			sql.append(" ON \"").append(tableAlias).append("\".\"").append(Field.ID_FIELD_DB_COLUMN).append("\" = \"");
			sql.append(sharingTable).append("_").append(sharingTableAliasCounter).append("\".\"recordid\"");
		}
		else
		{
			sql.append(" ON ");
			
			if (recordType.isCombineRecordAndCascadeSharing())
			{
				// merge with URS records that correspond to either to record's ID or to its sharingControlledByField field
				sql.append("(\"").append(tableAlias).append("\".\"").append(recordType.getSharingControlledByFieldId() != null ? recordType.getSharingControlledByField().getDbColumn() : Field.ID_FIELD_DB_COLUMN).append("\" = \"");
				sql.append(sharingTable).append("_").append(sharingTableAliasCounter).append("\".\"recordid\" OR \"");
				sql.append(tableAlias).append("\".\"").append(Field.ID_FIELD_DB_COLUMN).append("\" = \"");
				sql.append(sharingTable).append("_").append(sharingTableAliasCounter).append("\".\"recordid\")");
			}
			else
			{
				sql.append("\"").append(tableAlias).append("\".\"").append(recordType.getSharingControlledByFieldId() != null ? recordType.getSharingControlledByField().getDbColumn() : Field.ID_FIELD_DB_COLUMN).append("\" = \"");
				sql.append(sharingTable).append("_").append(sharingTableAliasCounter).append("\".\"recordid\"");
			}
		}
		
		sql.append(" AND \"").append(sharingTable).append("_").append(sharingTableAliasCounter).append("\".\"assigneduser\" = '").append(userId).append("') ");
		return sql.toString();
	}

	private static String implodeOrderings(List<OrderBy> orderings)
	{
		List<String> items = new ArrayList<String>();
		for (OrderBy ordering : orderings)
		{
			items.add(ordering.getPropertySQL() + " " + ordering.getOrder().name());
		}
		return MiscUtils.implode(items, ", ");
	}
	
	enum Grouping
	{
		NONE,
		ALL_NON_COLLECTION_FIELDS,
		USER_DEFINED_GROUP_BY_FIELDS
	}
}