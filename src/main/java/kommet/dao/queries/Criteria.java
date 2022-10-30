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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.dao.dal.AggregateFunction;
import kommet.dao.dal.AggregateFunctionCall;
import kommet.dao.dal.DALException;
import kommet.dao.dal.DALSyntaxException;
import kommet.dao.dal.InsufficientPrivilegesException;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.utils.MiscUtils;

public class Criteria
{
	private Restriction baseRestriction;
	private Type type;
	private EnvData env;
	private AuthData authData;
	private List<SqlJoinStructure> joins;
	
	/**
	 * Tells if table names should be quotes.
	 * Usually we want this to handle special keywords in table and column names
	 */
	private boolean quoteTableAndColumnNames = true;
	
	/**
	 * Map SQL aliases to type properties that they represent.
	 */
	private Map<String, String> aliasesToProperties;
	
	/**
	 * SQL aliases of nested properties. Only aliases of nested properties are stored,
	 * because simple properties have aliases identical to property names.
	 */
	private Map<String, String> propertyToAlias;
	
	/**
	 * Collection of nested properties that should be retrieved in this query,
	 * in addition to standard, direct properties on the main type.
	 */
	private Set<String> nestedProperties;
	
	/**
	 * List of simple, non-nested properties to be retrieved
	 */
	private Set<String> properties;
	
	/**
	 * List of aggregate functions in the SELECT clause
	 */
	private Set<AggregateFunctionCall> aggregateFunctions;
	
	/**
	 * Properties representing inverse collections
	 */
	private Set<String> inverseCollectionProperties;
	
	/**
	 * Properties representing associations
	 */
	private Set<String> associationProperties;
	
	/**
	 * Properties by which results are grouped
	 */
	private LinkedHashSet<String> groupByProperties;

	public static final String MAIN_TABLE_ALIAS = "this";
	
	/**
	 * Tells whether the DB table representing the main criteria object will be referenced
	 * with an alias in SQL queries.
	 * This flag should be true for SELECT queries and false for UPDATE queries.
	 */
	private boolean useMainTableAlias = false;
	
	private List<OrderBy> orderings;
	
	private Integer limit;
	private Integer offset;
	
	// counter used to give aliases consecutive numbers
	private Integer aliasCounter = 0;
	
	// tells if the ID field has been explicitly added to the select clause
	private boolean idExplicitlyAddedToSelectClause = false;

	private List<Type> joinedTypes;
	
	private boolean isSubquery;
	
	public Criteria (Type type, EnvData env, boolean useMainTableAlias) throws KommetException
	{
		this(type, null, env, useMainTableAlias);
	}
	
	/**
	 * Creates a criteria object for the given type type.
	 * @param type - KObject for which criteria is created
	 * @param env - environment on which criteria is created
	 * @param useMainTableAlias - whether to use an alias (recommended for SELECT queries) on the main table or not (use with UPDATE queries)
	 * @throws KommetException
	 */
	public Criteria (Type type, AuthData authData, EnvData env, boolean useMainTableAlias) throws KommetException
	{
		if (authData != null && !authData.canReadType(type.getKID(), false, env))
		{
			throw new InsufficientPrivilegesException("Insufficient privileges to query type " + type.getQualifiedName());
		}
		
		// the whole criteria is in fact a conjunction of multiple criteria
		this.type = type;
		this.joins = new ArrayList<SqlJoinStructure>();
		this.aliasesToProperties = new HashMap<String, String>();
		this.useMainTableAlias = useMainTableAlias;
		this.propertyToAlias = new HashMap<String, String>();
		this.nestedProperties = new HashSet<String>();
		this.properties = new HashSet<String>();
		this.aggregateFunctions = new HashSet<AggregateFunctionCall>();
		this.inverseCollectionProperties = new HashSet<String>();
		this.associationProperties = new HashSet<String>();
		this.authData = authData;
		
		// ID field is always retrieved in the criteria, unless the criteria contains aggregate functions.
		// At this point we don't know whether there will be aggregate functions in the select clause
		// so we add the ID field by default, and if there are aggregate functions, it will be removed.
		this.properties.add(Field.ID_FIELD_NAME);
		
		if (env == null)
		{
			throw new KommetException("Env not set while creating criteria");
		}
		this.env = env;
	}
	
	public void add (Restriction restriction)
	{
		if (this.baseRestriction == null)
		{
			this.baseRestriction = new Restriction(RestrictionOperator.AND);
		}
		this.baseRestriction.addSubrestriction(restriction);
	}
	
	public Restriction getBaseRestriction()
	{
		return this.baseRestriction;
	}

	public void setType(Type type)
	{
		this.type = type;
	}

	public Type getType()
	{
		return type;
	}
	
	public boolean isPropertyAliased (String property)
	{
		return this.aliasesToProperties.values().contains(property);
	}

	/**
	 * Creates an alias to the property.
	 * @param property
	 * @param originalAlias
	 * @param joinType
	 * @throws KommetException
	 */
	public void createAlias (String property, String originalAlias, JoinType joinType) throws KommetException
	{
		// append a random suffix to the alias - this is necessary because when we create a subquery, the subquery does not know
		// about aliases used in the parent query criteria, and it might use the same alias twice
		String alias = originalAlias + "_" + (new Random()).nextInt(1000000);
		
		// if the alias already exists, throw an exception
		if (this.aliasesToProperties.containsKey(alias))
		{
			throw new CriteriaException("Alias '" + alias + "' already exists in the criteria");
		}
		
		// make sure only one alias is added for each property
		if (isPropertyAliased(property))
		{
			throw new CriteriaException("An alias for property '" + property + "' has already been added");
		}
		
		this.aliasesToProperties.put(alias, property);
		addPropertyAlias(property, alias);
		
		// get the property from the criteria type
		Field field = this.type.getField(property, env);
		
		if (field == null)
		{
			throw new KommetException("No field found with name '" + property + "' on type " + this.type.getQualifiedName());
		}
		
		if (field.getDataType() instanceof TypeReference)
		{
			Type refType = ((TypeReference)field.getDataType()).getType();
			addJoinedType(refType);
			
			if (refType == null)
			{
				throw new KommetException("type reference incomplete. Make sure you pass the full type definition obtained from KObjectGlobalStore while creating criteria");
			}
			
			// if this property is already a nested property, e.g. "action.view" on type "standardAction",
			// then the left join table must be the alias for "standardAction.action"
			
			String leftTable = null;
			String leftColumn = null;
			String leftAlias = null;
			
			if (property.contains("."))
			{
				// remove the last subproperty
				String partialProperty = property.substring(0, property.lastIndexOf('.'));
			
				// we assume that this property already has to be aliased, so we find the alias for it
				leftAlias = this.propertyToAlias.get(partialProperty).toLowerCase();
				
				Field partialPropertyField = this.type.getField(partialProperty, env);
				
				if (partialPropertyField == null)
				{
					throw new CriteriaException("No property '" + partialProperty + "' found on type " + this.type.getApiName());
				}
				
				// left table is not needed because it is already joined
				leftTable = null;
			}
			else
			{
				// the property is not nested, so it is directly joined with the main criteria object
				leftTable = this.type.getDbTable();
				leftAlias = MAIN_TABLE_ALIAS;
			}
			
			leftColumn = field.getDbColumn();
			
			this.joins.add(new SqlJoin(joinType, leftTable, leftColumn, leftAlias, refType.getDbTable(), Field.ID_FIELD_DB_COLUMN, alias, refType));
		}
		else if (field.getDataType() instanceof InverseCollectionDataType)
		{
			this.inverseCollectionProperties.add(property);
			
			// TODO think about this restriction - uncomment to turn it on
			/*if (MiscUtils.getPartialProperties(property, false).size() > 2)
			{
				throw new KommetException("Inverse property " + property + " goes more than one level down");
			}*/
			
			Type inverseType = env.getType(((InverseCollectionDataType)field.getDataType()).getInverseTypeId());
			addJoinedType(inverseType);
			String inverseProperty = ((InverseCollectionDataType)field.getDataType()).getInverseProperty();
			
			if (inverseType == null)
			{
				throw new KommetException("Inverse Collection definition incomplete, missing inverse type. Make sure you pass the full type definition obtained from KObjectGlobalStore while creating criteria");
			}
			if (inverseProperty == null)
			{
				throw new KommetException("Inverse Collection definition incomplete, missing inverse property. Make sure you pass the full type definition obtained from KObjectGlobalStore while creating criteria");
			}
			
			Field inverseField = inverseType.getField(inverseProperty);
			if (inverseField == null)
			{
				throw new KommetException("Unknown property " + inverseProperty + " on inverse type " + inverseType.getQualifiedName());
			}
			
			this.joins.add(new SqlJoin(JoinType.LEFT_JOIN, this.type.getDbTable(), Field.ID_FIELD_DB_COLUMN, MAIN_TABLE_ALIAS, inverseType.getDbTable(), inverseField.getDbColumn(), alias, inverseType));
		}
		else if (field.getDataType() instanceof AssociationDataType)
		{
			this.associationProperties.add(property);
			AssociationDataType fieldDT = ((AssociationDataType)field.getDataType());
			
			Type linkingType = fieldDT.getLinkingType();
			String selfLinkingField = fieldDT.getSelfLinkingField();
			Type associatedType = fieldDT.getAssociatedType();
			String foreignLinkingField = fieldDT.getForeignLinkingField();
			
			addJoinedType(linkingType);
			addJoinedType(associatedType);
			
			if (linkingType == null)
			{
				throw new KommetException("Association definition incomplete, missing linking type. Make sure you pass the full type definition obtained from KObjectGlobalStore while creating criteria");
			}
			if (!StringUtils.hasText(selfLinkingField))
			{
				throw new KommetException("Association definition incomplete, missing self linking field. Make sure you pass the full type definition obtained from KObjectGlobalStore while creating criteria");
			}
			if (associatedType == null)
			{
				throw new KommetException("Association definition incomplete, missing associated type. Make sure you pass the full type definition obtained from KObjectGlobalStore while creating criteria");
			}
			if (!StringUtils.hasText(foreignLinkingField))
			{
				throw new KommetException("Association definition incomplete, missing foreign linking field. Make sure you pass the full type definition obtained from KObjectGlobalStore while creating criteria");
			}
			
			// generate alias for the linking table because it's not explicitly set by the user
			String linkingTableAlias = "alias_" + aliasCounter++;
			
			this.joins.add(new AssociationJoin(linkingType.getDbTable(), this.type.getDbTable(), associatedType.getDbTable(), linkingTableAlias, MAIN_TABLE_ALIAS, alias, linkingType.getField(selfLinkingField).getDbColumn(), linkingType.getField(foreignLinkingField).getDbColumn(), linkingType, associatedType, joinType));
			
			// add join between main type table and linking table
			//this.joins.add(new SqlJoin(JoinType.LEFT_JOIN, this.type.getDbTable(), Field.ID_FIELD_DB_COLUMN, MAIN_TABLE_ALIAS, linkingType.getDbTable(), linkingType.getField(selfLinkingField).getDbColumn(), linkingTableAlias, linkingType));
			
			// add join between linking type table and associated type table
			//this.joins.add(new SqlJoin(JoinType.INNER_JOIN, linkingType.getDbTable(), linkingType.getField(foreignLinkingField).getDbColumn(), linkingTableAlias, associatedType.getDbTable(), Field.ID_FIELD_DB_COLUMN, alias, associatedType));
		}
		else
		{
			throw new CriteriaException("Tried to create alias for property " + property + " of type " + field.getDataType() + ", but aliases can only be created for type references.");
		}
	}
	
	public List<SqlJoinStructure> getJoins()
	{
		return this.joins;
	}
	
	public Map<String, String> getAliasesToProperties()
	{
		return aliasesToProperties;
	}

	public void setUseMainTableAlias(boolean useMainTableAlias)
	{
		this.useMainTableAlias = useMainTableAlias;
	}

	public boolean isUseMainTableAlias()
	{
		return useMainTableAlias;
	}

	/**
	 * Adds an alias for a property
	 * @param property
	 * @param alias
	 */
	public void addPropertyAlias (String property, String alias)
	{
		this.propertyToAlias.put(property, alias);
	}
	
	public String getPropertyAlias (String property)
	{
		return this.propertyToAlias.get(property);
	}
	
	public List<Record> list() throws KommetException
	{
		SelectQuery query = SelectQuery.buildFromCriteria(this, this.nestedProperties, env);
		return query.execute();
	}
	
	public Record singleRecord() throws KommetException
	{
		SelectQuery query = SelectQuery.buildFromCriteria(this, this.nestedProperties, env);
		List<Record> records = query.execute();
		
		if (records.size() > 0)
		{
			if (records.size() == 1)
			{
				return records.get(0);
			}
			else
			{
				throw new KommetException("Query returned " + records.size() + " instead of expected single record");
			}
		}
		else
		{
			return null;
		}
	}
	
	private void addNestedProperty (String property)
	{
		this.nestedProperties.add(property);
		
		// If a nested property is added (e.g. father.father.name on object pigeon), then we automatically add
		// the ID of each nested property object on the path, i.e. father.id and father.father.id.
		// This feature has been turned off because it caused errors in GROUP BY queried.
		// Consider a query "select father.name from person group by father.name". This feature
		// caused adding the father.id field to the select clause, but not to the group by clause
		// which is illegal. Adding it to the GROUP BY clause would render incorrect results.
		
		/*String[] subproperties = property.split("\\.");
		String partialSubproperty = "";
		for (int i = 0; i < (subproperties.length - 1); i++)
		{
			partialSubproperty += subproperties[i] + "."; 
			this.nestedProperties.add(partialSubproperty + Field.ID_FIELD_NAME);
		}*/
	}
	
	public void addProperty (String property)
	{
		// we can set multiple properties separated by a comma
		if (property.contains(","))
		{
			String[] properties = property.split(",");
			for (int i = 0; i < properties.length; i++)
			{
				addProperty(properties[i].trim());
			}
		}
		else if (property.contains("."))
		{
			addNestedProperty(property);
		}
		else
		{
			this.properties.add(property);
			
			if (Field.ID_FIELD_NAME.equals(property))
			{
				idExplicitlyAddedToSelectClause = true;
			}
		}
	}
	
	public void addProperties(Collection<String> properties)
	{
		for (String p : properties)
		{
			addProperty(p);
		}
	}
	
	public void addAggregateFunction(AggregateFunctionCall function)
	{
		this.aggregateFunctions.add(function);
		
		if (!idExplicitlyAddedToSelectClause)
		{
			this.properties.remove(Field.ID_FIELD_NAME);
		}
	}
	
	/**
	 * Add an alias for each partial property of a nested property.
	 * Aliases are created by replacing dots in the property path by underscores.
	 * 
	 * E.g. for a partial property "father.mother.child.id", the following aliases will be added:
	 * "father_mother" for relationship "father.mother", "father_mother_child" for "father.mother.child"
	 * 
	 * 
	 * @param property nested property (can contain multiple nested properties separated by a dot)
	 * @throws KommetException
	 */
	public void addAliasesForProperty (String property) throws KommetException
	{
		List<String> partialProperties = MiscUtils.getPartialProperties(property, true);
		
		for (String partialProperty : partialProperties)
		{
			if (!isPropertyAliased(partialProperty))
			{
				// create an alias by replacing dots with an underscore
				createAlias(partialProperty, partialProperty.replaceAll("\\.", "_"));
			}
		}
	}
	
	public EnvData getEnv()
	{
		return this.env;
	}

	public Collection<String> getNestedProperties()
	{
		return this.nestedProperties;
	}

	public boolean isNotEmpty()
	{
		return this.baseRestriction != null && this.baseRestriction.getSubrestrictions() != null && !this.baseRestriction.getSubrestrictions().isEmpty();
	}

	public void createAlias(String property, String alias) throws KommetException
	{
		createAlias(property, alias.toLowerCase(), JoinType.LEFT_JOIN);
	}

	public Set<String> getProperties()
	{
		return this.properties;
	}
	
	public boolean isInverseCollection (String property)
	{
		List<String> parts = MiscUtils.getPartialProperties(property, false);
		for (String partialProperty : parts)
		{
			if (this.inverseCollectionProperties.contains(partialProperty))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isAssociation (String property)
	{
		List<String> parts = MiscUtils.getPartialProperties(property, false);
		for (String partialProperty : parts)
		{
			if (this.associationProperties.contains(partialProperty))
			{
				return true;
			}
		}
		return false;
	}
	
	public Set<String> getInverseCollectionProperties()
	{
		return this.inverseCollectionProperties;
	}
	
	public Set<String> getAssociationProperties()
	{
		return this.associationProperties;
	}

	public void setOrderings(List<OrderBy> orderings)
	{
		this.orderings = orderings;
	}

	public List<OrderBy> getOrderings()
	{
		return orderings;
	}
	
	public void addOrderBy (SortDirection direction, String property)
	{
		if (this.orderings == null)
		{
			this.orderings = new ArrayList<OrderBy>();
		}
		this.orderings.add(new OrderBy(direction, property));
	}

	public void setLimit(Integer limit)
	{
		this.limit = limit;
	}

	public Integer getLimit()
	{
		return limit;
	}

	public void setQuoteTableAndColumnNames(boolean quoteTableNames)
	{
		this.quoteTableAndColumnNames = quoteTableNames;
	}

	public boolean isQuoteTableAndColumnNames()
	{
		return quoteTableAndColumnNames;
	}

	/**
	 * Adds fields to the criteria that are always/usually selected.
	 * These include most system fields such as createdDate/By, lastModifiedDate/By
	 * @throws KommetException 
	 */
	public void addStandardSelectProperties() throws KommetException
	{
		addProperty(Field.ID_FIELD_NAME);
		addProperty(Field.CREATEDBY_FIELD_NAME + "." + Field.ID_FIELD_NAME);
		addProperty(Field.CREATEDDATE_FIELD_NAME);
		addProperty(Field.LAST_MODIFIED_BY_FIELD_NAME + "." + Field.ID_FIELD_NAME);
		addProperty(Field.LAST_MODIFIED_DATE_FIELD_NAME);
		addProperty(Field.ACCESS_TYPE_FIELD_NAME);
		
		// create aliases for user reference fields
		createAlias("createdBy", "createdBy");
		createAlias("lastModifiedBy", "lastModifiedBy");
	}

	public void setOffset(Integer offset)
	{
		this.offset = offset;
	}

	public Integer getOffset()
	{
		return offset;
	}

	public void addAggregateFunctions(List<AggregateFunctionCall> functions) throws KommetException
	{
		if (functions == null || functions.isEmpty())
		{
			throw new KommetException("Aggregate function list passed to the criteria are null or empty");
		}
		this.properties.remove(Field.ID_FIELD_NAME);
		this.aggregateFunctions.addAll(functions);
	}
	
	public Set<AggregateFunctionCall> getAggregateFunctions()
	{
		return this.aggregateFunctions;
	}

	public Long count() throws KommetException
	{
		List<Record> results = list();
		if (results.isEmpty())
		{
			throw new DALException("Invalid query for COUNT aggregate function. Should return exactly one row, returned none.");
		}
		else if (results.size() > 1)
		{
			throw new DALException("Invalid query for COUNT aggregate function. Should return exactly one row, returned " + results.size());
		}
		else if (!(results.get(0) instanceof QueryResult))
		{
			throw new DALException("Aggregate query returned a record instead of an aggregate query result");
		}
		else
		{
			if (aggregateFunctions.size() != 1)
			{
				throw new DALException("Aggregate criteria contains more than one aggregate function");
			}
			
			if (!aggregateFunctions.iterator().next().getFunction().equals(AggregateFunction.COUNT))
			{
				throw new DALException("Cannot call method count() on an aggregate function " + aggregateFunctions.iterator().next().getFunction());
			}
			
			Object aggregateValue = ((QueryResult)results.get(0)).getSingleAggregateValue();
			if (aggregateValue instanceof Long)
			{
				return (Long)aggregateValue;
			}
			else
			{
				throw new DALSyntaxException("Aggregate value returned for count query is not a long value");
			}
		}
	}

	public AuthData getAuthData()
	{
		return authData;
	}

	/**
	 * Tells whether user-record sharing rules should be applied when querying the given type.
	 * @param typeId
	 * @return
	 * @throws KommetException
	 */
	public boolean isApplySharings (KID typeId) throws KommetException
	{
		if (this.authData == null)
		{
			// if no auth data is available, do not apply any sharings
			return false;
		}
		else
		{
			if (this.authData.canReadAllType(typeId, false, env))
			{
				// if user can read all records of the given type, we do not apply any sharings
				return false;
			}
			else
			{
				if (this.authData.canReadType(typeId, false, env))
				{
					return true;
				}
				else
				{
					throw new InsufficientPrivilegesException("Insufficient privileges to query type " + env.getType(typeId).getQualifiedName());
				}
			}
		}
	}

	public void addGroupByProperty(String property)
	{
		if (this.groupByProperties == null)
		{
			this.groupByProperties = new LinkedHashSet<String>();
		}
		this.groupByProperties.add(property);
		
		if (!idExplicitlyAddedToSelectClause)
		{
			this.properties.remove(Field.ID_FIELD_NAME);
		}
	}

	public LinkedHashSet<String> getGroupByProperties()
	{
		return groupByProperties;
	}

	public boolean isGrouped()
	{
		return this.groupByProperties != null && !this.groupByProperties.isEmpty();
	}

	public boolean isGroupedProperty(String property)
	{
		return this.groupByProperties != null && this.groupByProperties.contains(property);
	}

	public List<Type> getJoinedTypes()
	{
		return joinedTypes;
	}
	
	private void addJoinedType (Type type)
	{
		if (this.joinedTypes == null)
		{
			this.joinedTypes = new ArrayList<Type>();
		}
		this.joinedTypes.add(type);
	}
	
	public boolean hasMultipleCollections()
	{
		return (inverseCollectionProperties.size() + associationProperties.size()) > 1;
	}

	public boolean isSubquery()
	{
		return isSubquery;
	}

	public void setSubquery(boolean isSubquery)
	{
		this.isSubquery = isSubquery;
	}
	
	public boolean isIdExplicitlyAddedToSelectClause()
	{
		return idExplicitlyAddedToSelectClause;
	}

	/**
	 * Returns a field that is the only selected field in a subquery criteria.
	 * @return
	 * @throws KommetException 
	 */
	public Field getSelectedFieldForSubquery() throws KommetException
	{
		if (!isSubquery)
		{
			return null;
		}
		
		if (!idExplicitlyAddedToSelectClause)
		{
			this.properties.remove(Field.ID_FIELD_NAME);
		}
		
		if ((this.properties.size() + this.nestedProperties.size()) > 1)
		{
			return null;
		}
		
		if (this.properties.size() == 1)
		{
			return this.type.getField(this.properties.iterator().next());
		}
		else if (this.nestedProperties.size() == 1)
		{
			return this.type.getField(this.nestedProperties.iterator().next(), this.env);
		}
		else
		{
			throw new CriteriaException("Could not get single subquery property");
		}
	}
}	