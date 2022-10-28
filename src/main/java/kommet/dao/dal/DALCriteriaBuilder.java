/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.dal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

import kommet.auth.AuthData;
import kommet.config.Constants;
import kommet.dao.TypePersistenceMapping;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.CriteriaException;
import kommet.dao.queries.OrderBy;
import kommet.dao.queries.Restriction;
import kommet.dao.queries.RestrictionOperator;
import kommet.dao.queries.SortDirection;
import kommet.data.Field;
import kommet.data.NoSuchFieldException;
import kommet.data.KommetException;
import kommet.data.datatypes.DataType;
import kommet.env.EnvData;
import kommet.utils.AppConfig;
import kommet.utils.DataUtil;
import kommet.utils.MiscUtils;

/**
 * Utility that builds criteria objects from DAL queries.
 * 
 * @author Radek Krawiec
 *
 */
public class DALCriteriaBuilder
{	
	private static Set<String> aggregateFunctions = null;	
	private static Set<Character> operators = null;
	
	static
	{
		operators = new HashSet<Character>();
		operators.add('+');
		operators.add('-');
		operators.add('>');
		operators.add('<');
		operators.add('=');
	}
	
	/**
	 * Create a criteria from a DAL query.
	 * 
	 * @param dalQuery - string containing the DAL query
	 * @param env - the environment in which the DAL query is executed
	 * @return
	 * @throws KommetException
	 */
	public static Criteria getSelectCriteriaFromDAL (String dalQuery, AuthData authData, EnvData env) throws KommetException
	{
		// copy instance of the query
		String dal = new String(dalQuery).trim();
		
		// normalize commas and brackets
		dal = DALCriteriaBuilder.format(dal);
		
		// split into tokens: words, quoted strings and brackets are treated as separate tokens
		List<String> tokens = DALCriteriaBuilder.tokenize(dal);
		
		// Build criteria analyzing the DAL from the beginning until the start of the WHERE clause
		// The returned criteria has the object and select fields set.
		Criteria c = DALCriteriaBuilder.getCriteriaForSelectClause(tokens, authData, env, dalQuery);
		
		// find the position of the WHERE keyword
		Integer whereKeywordPos = null;
		
		for (int i = 0; i < tokens.size(); i++)
		{
			if (tokens.get(i).toLowerCase().equals("where"))
			{
				whereKeywordPos = i;
				break;
			}
		}
		
		Integer limitKeywordPos = null;
		Integer offsetKeywordPos = null;
		Integer orderByKeywordPos = null;
		Integer whereClauseEnd = null;
		Integer groupByKeywordPos = null;
		
		// find limit, offset, group by and order by keywords
		for (int i = (whereKeywordPos != null ? whereKeywordPos + 1 : 0); i < tokens.size(); i++)
		{
			String token = tokens.get(i).toLowerCase();
			if (token.equals("limit"))
			{
				limitKeywordPos = i;
				whereClauseEnd = whereClauseEnd == null ? i - 1 : whereClauseEnd;
			}
			else if (token.equals("offset"))
			{
				offsetKeywordPos = i;
				
				// set the whereClauseEnd position only if not already set
				whereClauseEnd = whereClauseEnd == null ? i - 1 : whereClauseEnd; 
			}
			else if (token.equals("order"))
			{
				if ((i + 1) >= tokens.size())
				{
					throw new DALSyntaxException("Last token in query is ORDER");
				}
				else if (tokens.get(i + 1).toLowerCase().equals("by"))
				{
					orderByKeywordPos = i;
					
					// set the whereClauseEnd position only if not already set
					whereClauseEnd = whereClauseEnd == null ? i - 1 : whereClauseEnd;
				}
			}
			else if (token.equals("group"))
			{
				if ((i + 1) >= tokens.size())
				{
					throw new DALSyntaxException("Last token in query is GROUP");
				}
				else if (tokens.get(i + 1).toLowerCase().equals("by"))
				{
					groupByKeywordPos = i;
					
					// set the whereClauseEnd position only if not already set
					whereClauseEnd = whereClauseEnd == null ? i - 1 : whereClauseEnd;
				}
			}
		}
		
		if (whereKeywordPos == null)
		{
			if (limitKeywordPos == null && orderByKeywordPos == null && offsetKeywordPos == null && groupByKeywordPos == null)
			{	
				checkSelectFieldsAgainstAggregationAndGrouping(c);
				
				// if no WHERE keyword has been found, the where clause simply does not exist
				// so we return the criteria as it is
				return c;
			}
		}
		else
		{	
			// analyze the where clause and add conditions to criteria
			c = DALCriteriaBuilder.addCriteriaConditionsFromDAL (c, tokens, whereKeywordPos, whereClauseEnd != null ? whereClauseEnd : tokens.size() - 1, dalQuery, authData, env);
		}
		
		checkKeywordOrder(tokens, whereKeywordPos, orderByKeywordPos, limitKeywordPos, offsetKeywordPos, groupByKeywordPos);
		
		// handle GROUP BY keyword
		if (groupByKeywordPos != null)
		{		
			parseGroupByClause(c, tokens, groupByKeywordPos, orderByKeywordPos != null ? orderByKeywordPos : (limitKeywordPos != null ? limitKeywordPos : offsetKeywordPos));
		}
		
		// after group by clauses have been processed, we can check whether all properties
		// in the select clause are either aggregate functions or have been used in the group by clause
		checkSelectFieldsAgainstAggregationAndGrouping(c);
		
		// handle ORDER BY keyword
		if (orderByKeywordPos != null)
		{		
			parseOrderByClause(c, tokens, orderByKeywordPos, limitKeywordPos != null ? limitKeywordPos : offsetKeywordPos);
		}
		
		// handle LIMIT keyword
		if (limitKeywordPos != null)
		{
			if (tokens.size() >= limitKeywordPos)
			{
				try
				{
					Integer limit = Integer.parseInt(tokens.get(limitKeywordPos + 1));
					c.setLimit(limit);
				}
				catch (NumberFormatException e)
				{
					throw new DALSyntaxException("Expected an integer after the LIMIT keyword, encountered '" + tokens.get(limitKeywordPos + 1));
				}
			}
			else
			{
				throw new DALSyntaxException("Row limit expected after the LIMIT keyword");
			}
		}
		
		// handle OFFSET keyword
		if (offsetKeywordPos != null)
		{
			if (tokens.size() >= offsetKeywordPos)
			{
				try
				{
					Integer offset = Integer.parseInt(tokens.get(offsetKeywordPos + 1));
					c.setOffset(offset);
				}
				catch (NumberFormatException e)
				{
					throw new DALSyntaxException("Expected an integer after the OFFSET keyword, encountered '" + tokens.get(limitKeywordPos + 1));
				}
			}
			else
			{
				throw new DALSyntaxException("Row offset expected after the OFFSET keyword");
			}
		}
		
		return c;
	}
	
	/**
	 * Makes sure keywords are in correct order
	 * @param whereKeywordPos
	 * @param orderByKeywordPos
	 * @param limitKeywordPos
	 * @param offsetKeywordPos
	 * @param offsetKeywordPos2 
	 * @throws DALSyntaxException 
	 */
	private static void checkKeywordOrder(List<String> tokens, Integer whereKeywordPos, Integer orderByKeywordPos, Integer limitKeywordPos, Integer offsetKeywordPos, Integer groupByKeywordPos) throws DALSyntaxException
	{
		List<Integer> positions = new ArrayList<Integer>();
		positions.add(whereKeywordPos);
		positions.add(groupByKeywordPos);
		positions.add(orderByKeywordPos);
		positions.add(limitKeywordPos);
		positions.add(offsetKeywordPos);
		
		int max = -1;
		for (Integer pos : positions)
		{
			if (pos == null)
			{
				continue;
			}
			
			if (max == -1)
			{
				max = pos;
			}
			else
			{
				if (max >= pos)
				{
					throw new DALSyntaxException("Misplaced token " + tokens.get(pos) + ". Clauses in query should appear in the following order: WHERE, GROUP BY, ORDER BY, LIMIT, OFFSET");
				}
				
				max = pos;
			}
				
		}
	}

	/**
	 * If aggregate functions are used, this method checks if all fields in the SELECT clause are either
	 * put in the group by clause 
	 * @param c
	 * @throws DALSyntaxException
	 */
	private static void checkSelectFieldsAgainstAggregationAndGrouping (Criteria c) throws DALSyntaxException
	{
		// after group by clauses have been processed, we can check whether all properties
		// in the select clause are either aggregate functions or have been used in the group by clause
		if ((!c.getAggregateFunctions().isEmpty() || c.isGrouped()) && !c.getProperties().isEmpty())
		{
			List<String> offendingProperties = new ArrayList<String>();
			for (String selectField : c.getProperties())
			{
				// select fields that are not aggregate functions must be used in group by clauses
				if (!c.isGroupedProperty(selectField))
				{
					offendingProperties.add(selectField);
				}
			}
			
			if (!offendingProperties.isEmpty())
			{
				throw new DALSyntaxException("The following properties should be used either in an aggregate function or a group by clause: " + MiscUtils.implode(c.getProperties(), ", "));
			}
		}
	}

	/**
	 * Parses the ORDER BY clause of the query and adds proper orderings to the criteria.
	 * 
	 * @param c criteria representing the query
	 * @param tokens list of tokens of the query
	 * @param orderByKeywordPos position of the ORDER keyword in the tokens list
	 * @param nextKeywordPos positions of the next keyword (or null if there is no next keyword) in the tokens list
	 * @throws DALSyntaxException thrown when a syntax error is encountered in the ORDER BY clause
	 */
	private static void parseOrderByClause (Criteria c, List<String> tokens, int orderByKeywordPos, Integer nextKeywordPos) throws DALSyntaxException
	{
		// the orderByKeywordPos is the position of the word "order", after which there has to be
		// the word BY and the a field name
		if (tokens.size() >= (orderByKeywordPos + 1))
		{
			List<OrderBy> orderings = new ArrayList<OrderBy>();
			OrderBy currentOrdering = new OrderBy(SortDirection.ASC);
			
			// the order of keywords is strictly defined and should be WHERE, ORDER BY, LIMIT, OFFSET
			// so we can parse order by properties until the end of the query or the LIMIT keyword is encountered
			for (int i = orderByKeywordPos + 2; i < (nextKeywordPos != null ? nextKeywordPos : tokens.size()); i++)
			{
				String token = tokens.get(i);
				if (token.equals(","))
				{
					if (currentOrdering.getProperty() != null)
					{
						orderings.add(currentOrdering);
						currentOrdering = new OrderBy(SortDirection.ASC);
					}
					else
					{
						throw new DALSyntaxException("Error in ORDER BY clause: order by property not defined, comma encountered");
					}
				}
				else if (token.toLowerCase().equals("asc"))
				{
					currentOrdering.setOrder(SortDirection.ASC);
				}
				else if (token.toLowerCase().equals("desc"))
				{
					currentOrdering.setOrder(SortDirection.DESC);
				}
				else
				{
					// set sort property name - do not convert to lower case since properties are case sensitive
					currentOrdering.setProperty(token);
				}
			}
			
			// add the last ordering because the loop exits before it is added
			if (currentOrdering.getProperty() != null)
			{
				orderings.add(currentOrdering);
			}
			else
			{
				throw new DALSyntaxException("Error in ORDER BY clause: order by property not defined, end of ORDER BY clause reached");
			}
			
			c.setOrderings(orderings);
		}
		else
		{
			throw new DALSyntaxException("Field name not encountered after the ORDER BY keyword");
		}
	}
	
	/**
	 * Parses the GROUP BY clause of the query and adds proper groupings to the criteria.
	 * 
	 * @param c criteria representing the query
	 * @param tokens list of tokens of the query
	 * @param groupByKeywordPos position of the GROUP keyword in the tokens list
	 * @param nextKeywordPos positions of the next keyword (or null if there is no next keyword) in the tokens list
	 * @throws DALSyntaxException thrown when a syntax error is encountered in the ORDER BY clause
	 */
	private static void parseGroupByClause (Criteria c, List<String> tokens, int groupByKeywordPos, Integer nextKeywordPos) throws DALSyntaxException
	{
		// the orderByKeywordPos is the position of the word "order", after which there has to be
		// the word BY and the a field name
		if (tokens.size() >= (groupByKeywordPos + 1))
		{	
			// the order of keywords is strictly defined and should be WHERE, ORDER BY, LIMIT, OFFSET
			// so we can parse order by properties until the end of the query or the LIMIT keyword is encountered
			for (int i = groupByKeywordPos + 2; i < (nextKeywordPos != null ? nextKeywordPos : tokens.size()); i++)
			{
				String token = tokens.get(i);
				if (!token.equals(","))
				{
					c.addGroupByProperty(token);
				}
			}
		}
		else
		{
			throw new DALSyntaxException("Field name not encountered after the GROUP BY keyword");
		}
	}
	
	public static String format (String dal)
	{	
		String formattedDal = new String(dal).replaceAll("\\s+,\\s+", ", ");
		return formattedDal;
	}
	
	public static List<String> tokenize (String str) throws KommetException
	{
		return MiscUtils.tokenize(str, '\'', null, operators);
	}
	
	public static List<String> treatBracketsAsWords (List<String> items)
	{
		List<String> results = new ArrayList<String>();
		
		for (String item : items)
		{
			if (!item.startsWith("'") || !item.endsWith("'"))
			{
				CollectionUtils.addAll(results, (item.replaceAll("[\\(\\)]", " $0 ").split("\\s+")));
			}
			else
			{
				// if its a quoted string, just rewrite it
				results.add(item);
			}
		}
		
		return results;
	}
	
	private static Criteria getCriteriaForSelectClause(List<String> tokens, AuthData authData, EnvData env, String dalQuery) throws KommetException
	{
		// make sure the query starts with a SELECT keyword
		if (!tokens.get(0).toLowerCase().equals("select"))
		{
			throw new DALSyntaxException("Select query does not start with the SELECT keyword: " + dalQuery);
		}
		
		List<String> selectFields = new ArrayList<String>();
		List<AggregateFunctionCall> aggregateFunctions = new ArrayList<AggregateFunctionCall>();
		Integer fromKeywordPos = null;
		
		AggregateFunctionCall aggregateFunction = null;
		int bracketsInAggFunction = 0;
		boolean isQueryDefaultField = false;
		
		// parse all fields until a WHERE keyword is encountered
		for (int i = 1; i < tokens.size(); i++)
		{
			String word = tokens.get(i);
			if (word.equals(","))
			{
				continue;
			}
			
			if (word.toLowerCase().equals("from"))
			{
				fromKeywordPos = i;
				break;
			}
			
			if (aggregateFunction != null)
			{
				if ("(".equals(word))
				{
					if (bracketsInAggFunction > 0)
					{
						throw new DALSyntaxException("Double opening bracket in aggregate function " + aggregateFunction.getFunction());
					}
					else
					{
						bracketsInAggFunction++;
					}
				}
				else if (")".equals(word))
				{
					if (bracketsInAggFunction != 1)
					{
						throw new DALSyntaxException("Incorrect syntax near closing bracket of aggregate function " + aggregateFunction.getFunction());
					}
					else
					{
						aggregateFunctions.add(aggregateFunction);
						aggregateFunction = null;
						bracketsInAggFunction = 0;
					}
				}
				else
				{
					if (aggregateFunction.getProperty() == null)
					{
						if (bracketsInAggFunction == 1)
						{
							aggregateFunction.setProperty(word);
						}
						else
						{
							throw new DALSyntaxException("Bracket expected in aggregate function " + aggregateFunction.getFunction() + ", found token '" + word + "'");
						}
					}
					else
					{
						throw new DALSyntaxException("Incorrect property '" + word + "' specified for double function " + aggregateFunction.getFunction());
					}
				}
				
				continue;
			}
			else if ("(".equals(word) || ")".equals(word))
			{
				throw new DALSyntaxException("Bracket '" + word + "' found in SELECT clause but it is not preceded by an aggregate function name");
			}
			
			if (isAggregateFunction(word))
			{
				aggregateFunction = new AggregateFunctionCall();
				aggregateFunction.setFunction(AggregateFunction.getByName(word));
			}
			else if (word.equals(Constants.DEFAULT_FIELD_DAL_TOKEN))
			{
				isQueryDefaultField = true;
			}
			else
			{
				selectFields.add(word);
			}
		}
		
		if (selectFields.isEmpty() && aggregateFunctions.isEmpty() && !isQueryDefaultField)
		{
			throw new DALSyntaxException("The select query does not contain any fields or aggregate functions to retrieve: " + dalQuery);
		}
		
		if (fromKeywordPos == null)
		{
			throw new DALSyntaxException("The select query does not contain the FROM keyword: " + dalQuery);
		}
		
		if (fromKeywordPos + 1 >= tokens.size())
		{
			throw new DALSyntaxException("No type specified after the FROM keyword: " + dalQuery);
		}
		
		// the next word after the WHERE keyword should be the main entities API name
		String mainTypeApiName = tokens.get(fromKeywordPos + 1).trim();
		
		// in DAL queries, types must be referenced by their full qualified user specific names
		// unless they are placed in the base package of the env
		TypePersistenceMapping mainQueryTypeMapping = env.getTypeMappingByApiName(mainTypeApiName);
		
		// check all aggregate function properties if they are not collections
		for (AggregateFunctionCall aggr : aggregateFunctions)
		{
			if (aggr.getProperty().contains(".") && DataUtil.isCollection(aggr.getProperty(), mainQueryTypeMapping.getType()))
			{
				//throw new DALSyntaxException("Property " + aggr.getProperty() + " cannot be used aggregate function because it is a collection");
			}
		}
		
		// if not found, try with base package
		if (mainQueryTypeMapping == null && !mainTypeApiName.contains("."))
		{
			String basePackageName = env.getEnv().getBasePackage() + "." + mainTypeApiName;
			mainQueryTypeMapping = env.getTypeMappingByApiName(basePackageName);
			
			if (mainQueryTypeMapping == null)
			{
				// try with base package for system types
				basePackageName = AppConfig.BASE_TYPE_PACKAGE + "." + mainTypeApiName;
				mainQueryTypeMapping = env.getTypeMappingByApiName(basePackageName);
			}
		}
		
		if (mainQueryTypeMapping == null)
		{
			throw new DALSyntaxException("No type found with API name " + mainTypeApiName + " in query: " + dalQuery + ". Remember API names are case-sensitive");
		}
		
		if (isQueryDefaultField)
		{
			selectFields.add(mainQueryTypeMapping.getType().getDefaultFieldApiName());
		}
		
		Criteria criteria = env.getSelectCriteria(mainQueryTypeMapping.getType().getKID(), authData);
		
		boolean hasGroupedCollection = false;
		boolean hasGroupedNonCollection = false;
		
		if (!aggregateFunctions.isEmpty())
		{
			criteria.addAggregateFunctions(aggregateFunctions);
			for (AggregateFunctionCall aggrFunction : aggregateFunctions)
			{
				// check if the group by property is a collection or a type reference
				String firstProperty = MiscUtils.getPartialProperties(aggrFunction.getProperty(), false).get(0);
				
				Field aggrField = criteria.getType().getField(firstProperty);
				if (aggrField == null)
				{
					throw new DALSyntaxException("Property " + aggregateFunction.getProperty() + " not found on type " + criteria.getType().getQualifiedName());
				}
				
				if (aggrField.getDataTypeId().equals(DataType.INVERSE_COLLECTION) || aggrField.getDataTypeId().equals(DataType.ASSOCIATION))
				{
					hasGroupedCollection = true;
				}
				else
				{
					hasGroupedNonCollection = true;
				}
				
				if (hasGroupedCollection && hasGroupedNonCollection)
				{
					throw new DALSyntaxException("Cannot call aggregate function on both collections and non-collections");
				}
				
				criteria.addAliasesForProperty(aggrFunction.getProperty());
			}
		}
		
		// add select fields to the SQL
		for (String propertyName : selectFields)
		{
			criteria.addProperty(propertyName);
			Field field = criteria.getType().getField(propertyName, criteria.getEnv());
			
			if (field == null)
			{
				throw new NoSuchFieldException("Field " + propertyName + " not found on type " + criteria.getType().getQualifiedName());
			}
			
			// Make sure relationships are not included in DAL select clause.
			// E.g. if we have a field people.mother.age, we cannot write "select mother from people",
			// we need to make it "select mother.id, mother.age from people"
			if (!field.getDataType().isPrimitive())
			{
				throw new DALSyntaxException("Cannot reference whole relationship '" + propertyName + "' in a DAL query. Specific fields of the relationship must be listed, e.g. '" + propertyName + ".id'");
			}
			
			// create an alias for every subproperty of the nested property
			if (propertyName.contains("."))
			{	
				criteria.addAliasesForProperty(propertyName);
			}
		}
		
		return criteria;
	}

	public static Criteria addCriteriaConditionsFromDAL(Criteria c,	List<String> tokens, Integer whereKeywordPos, int lastTokenInWhereClausePos, String dalQuery, AuthData authData, EnvData env) throws KommetException
	{	
		// rewrite where clause token, wrapping the where clause in brackets
		List<String> whereClauseTokens = new ArrayList<String>();
		whereClauseTokens.add("(");
		whereClauseTokens.addAll(tokens.subList(whereKeywordPos + 1, lastTokenInWhereClausePos + 1));
		whereClauseTokens.add(")");
		
		// start analyzing with the next token after the WHERE keyword
		RestrictionParseResult parsedRestriction = getRestrictionFromDAL(whereClauseTokens, 0, dalQuery, authData, env); 
		c.add(parsedRestriction.getRestriction());
		
		// create an alias for all properties used in the restrictions
		for (String property : parsedRestriction.getProperties())
		{
			// make sure the property exists
			if (c.getType().getField(property, env) == null)
			{
				throw new DALSyntaxException("Property " + property + " not found on type " + c.getType().getQualifiedName());
			}
			
			// create aliases for all partial properties, e.g. for nested property 'father.mother.age'
			// create an alias for 'father' and 'father.mother'
			List<String> partialProperties = MiscUtils.getPartialProperties(property, true);
			
			for (String partialProperty : partialProperties)
			{
				if (!c.isPropertyAliased(partialProperty))
				{
					// create an alias by replacing dots with an underscore
					c.createAlias(partialProperty, partialProperty.replaceAll("\\.", "_"));
				}
			}
		}
		
		return c;
	}

	public static RestrictionParseResult getRestrictionFromDAL(List<String> tokens, Integer currTokenIndex, String dalQuery, AuthData authData, EnvData env) throws KommetException
	{
		Restriction r = new Restriction();
		int openBrackets = 0;
		String firstToken = null;
		Set<String> propertiesUsedInRestriction = new HashSet<String>();
		
		while (currTokenIndex < tokens.size())
		{
			String token = tokens.get(currTokenIndex++);
			if (firstToken == null)
			{
				firstToken = token;
			}
			
			if (token.equals("("))
			{
				// opening a bracket introduces a new criterion, unless it was preceded by an IN operator
				// in which case the brackets will contain values for this operator
				if (!RestrictionOperator.IN.equals(r.getOperator()))
				{
					openBrackets++;
					RestrictionParseResult parsedRestriction = getRestrictionFromDAL(tokens, currTokenIndex, dalQuery, authData, env);
					r.addSubrestriction(parsedRestriction.getRestriction());
					currTokenIndex = parsedRestriction.getCurrentTokenIndex();
					// add properties used by the subrestriction to the list of all properties used by the current restriction
					propertiesUsedInRestriction.addAll(parsedRestriction.getProperties());
					//log.debug("Finished subquery #1 at pos " + parsedRestriction.getCurrentTokenIndex() + ", brackets = " + openBrackets);
				}
				else
				{	
					boolean isFirstTokenInBracket = true;
					boolean isSubqueryInBracket = false;
					StringBuilder subquery = new StringBuilder();
					
					// parse the bracket, its content and the closing bracket as a value for the IN operator
					while (!token.equals(")") && currTokenIndex < tokens.size())
					{
						token = tokens.get(currTokenIndex++);
						
						if (isFirstTokenInBracket && token.toLowerCase().equals("select"))
						{
							// if the first word in the bracket is a SELECT keyword, then we assume that the whole contents of the bracket is a subquery
							isSubqueryInBracket = true;
						}
						
						isFirstTokenInBracket = false;
						
						if (isSubqueryInBracket)
						{
							subquery.append(token).append(" ");
						}
						else
						{
							// values in the IN argument bracket are separated by commas which we don't
							// treat as values, so they need to be skipped
							if (!token.equals(",") && !token.equals(")"))
							{
								r.addValue(MiscUtils.trimLeft(MiscUtils.trimRight(token, '\''), '\''));
							}
						}
					}
					
					if (isSubqueryInBracket)
					{
						Criteria subcriteria = DALCriteriaBuilder.getSelectCriteriaFromDAL(subquery.toString().trim(), authData, env);
						subcriteria.setSubquery(true);
						r.setValue(subcriteria);
					}
					
					if (r.isValid())
					{
						return new RestrictionParseResult(r, currTokenIndex, propertiesUsedInRestriction);
					}
					else
					{
						throw new DALSyntaxException("Error parsing IN restriction - syntax problem");
					}
				}
			}
			else if (token.equals(")"))
			{
				openBrackets--;
				if (openBrackets < 1)
				{
					// if a restriction is started with a bracket, and there was neither an operator, property or value
					// assigned to it, just a single subrestriction, then this restriction was probably contained
					// in a superfluous bracket, e.g. ((age > 2)). In this case we want to extract the subrestriction
					// from the restriction.
					if (firstToken.equals("(") && r.getOperator() == null && r.getProperty() == null && r.getValue() == null && r.getSubrestrictions() != null && r.getSubrestrictions().size() == 1)
					{
						r = r.getSubrestrictions().get(0);
					}
					
					// there are 0 open brackets, so the restriction should be complete
					if (r.isValid())
					{
						return new RestrictionParseResult(r, currTokenIndex, propertiesUsedInRestriction);
					}
					else
					{
						throw new DALSyntaxException("All brackets are closed, but the restriction is not completed");
					}
				}
			}
			// if the encountered token is a restriction operator
			else if (RestrictionOperator.fromDALOperator(token) != null)
			{
				// get operator from string token
				RestrictionOperator operator = RestrictionOperator.fromDALOperator(token);
				
				if (operator.equals(RestrictionOperator.AND) || operator.equals(RestrictionOperator.OR))
				{	
					// A restriction operator is may already defined for this restriction, which is allowed
					// only which multioperand operators AND and OR.
					// E.g. the query might be "cond1 AND cond2 AND cond3" which is OK,
					// but cannot be "cond1 AND cond2 OR cond3".
					if (r.getOperator() == null || r.getOperator().equals(operator))
					{
						r.setOperator(operator);
						RestrictionParseResult parsedRestriction = getRestrictionFromDAL(tokens, currTokenIndex, dalQuery, authData, env);
						r.addSubrestriction(parsedRestriction.getRestriction());
						currTokenIndex = parsedRestriction.getCurrentTokenIndex();
						// add properties used by the subrestriction to the list of all properties used by the current restriction
						propertiesUsedInRestriction.addAll(parsedRestriction.getProperties());
						
						//log.debug("Finished subquery #2 at pos " + parsedRestriction.getCurrentTokenIndex() + ", brackets = " + openBrackets);
					}
					else
					{
						throw new CriteriaException("Operator already set for criteria. Work query version: " + MiscUtils.implode(tokens, " "));
					}
				}
				else
				{
					r.setOperator(operator);
					
					// ISNULL is the only unary operator that appears after the property
					// if it has been encountered, the restriction should already be finished
					if (operator.equals(RestrictionOperator.ISNULL))
					{
						if (r.isValid())
						{
							return new RestrictionParseResult(r, currTokenIndex, propertiesUsedInRestriction);
						}
						else
						{
							throw new DALSyntaxException("Cannot create restriction for operator ISNULL. Operator may be misplaced. Make sure to use it after the property whose value is checked.");
						}
					}
				}
			}
			else
			{
				// This condition is entered when a word token (not an operator) has been encountered.
				// This token may be either a property or a value.
				
				// If no operator is set yet for the restriction, then it means that this restriction
				// is binary, in which case the first argument of the operator is a property
				// This applies to opertors >, <, <=, >=, =, <>, LIKE
				if (r.getOperator() == null)
				{
					r.setProperty(token);
					propertiesUsedInRestriction.add(token);
				}
				// if operator has already been set
				else
				{
					if (r.getOperator().equals(RestrictionOperator.NOT))
					{
						RestrictionParseResult parsedRestriction = getRestrictionFromDAL(tokens, currTokenIndex, dalQuery, authData, env);
						r.addSubrestriction(parsedRestriction.getRestriction());
						currTokenIndex = parsedRestriction.getCurrentTokenIndex();
						// add properties used by the subrestriction to the list of all properties used by the current restriction
						propertiesUsedInRestriction.addAll(parsedRestriction.getProperties());
						return new RestrictionParseResult(r, currTokenIndex, propertiesUsedInRestriction);
					}
					else if (r.getOperator().equals(RestrictionOperator.AND) || r.getOperator().equals(RestrictionOperator.OR))
					{
						// start a new subrestriction
						RestrictionParseResult parsedRestriction = getRestrictionFromDAL(tokens, currTokenIndex, dalQuery, authData, env);
						r.addSubrestriction(parsedRestriction.getRestriction());
						currTokenIndex = parsedRestriction.getCurrentTokenIndex();
						// add properties used by the subrestriction to the list of all properties used by the current restriction
						propertiesUsedInRestriction.addAll(parsedRestriction.getProperties());
					}
					else
					{
						// set the value, but if the token in enclosed in single quotes, remove them
						r.setValue(MiscUtils.trimLeft(MiscUtils.trimRight(token, '\''), '\''));
	
						// the value for the restriction has already been set, so if it is complete, we can return it
						if (openBrackets == 0)
						{
							return new RestrictionParseResult(r, currTokenIndex, propertiesUsedInRestriction);
						}
						else
						{
							throw new DALSyntaxException("Restriction is completed, but not all brackets are closed");
						}
					}
				}
			}
		}
		
		return new RestrictionParseResult(r, currTokenIndex, propertiesUsedInRestriction);
	}
	
	private static boolean isAggregateFunction(String keyword)
	{
		return getAggregateFunctions().contains(keyword.toLowerCase());
	}
	
	private static Set<String> getAggregateFunctions()
	{
		if (aggregateFunctions == null)
		{
			aggregateFunctions = new HashSet<String>();
			aggregateFunctions.add("count");
			aggregateFunctions.add("sum");
			aggregateFunctions.add("avg");
			aggregateFunctions.add("min");
			aggregateFunctions.add("max");
		}
		
		return aggregateFunctions;
	}
}