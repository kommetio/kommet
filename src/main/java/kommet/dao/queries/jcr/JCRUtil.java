/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries.jcr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import kommet.dao.dal.AggregateFunction;
import kommet.dao.dal.AggregateFunctionCall;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.OrderBy;
import kommet.dao.queries.RestrictionOperator;
import kommet.data.Field;
import kommet.data.PIR;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;

/**
 * Javascript Criteria Representation utils.
 * @author Radek Krawiec
 * @created 2-09-2014
 */
public class JCRUtil
{
	private static ObjectMapper mapper;
	
	/**
	 * Get JSON mapper object that has all the knowledge necessary for properly translating a JCR object
	 * into JSON, and the other way around.
	 * @return
	 */
	private static ObjectMapper getJCRMapper()
	{
		if (mapper == null)
		{
			mapper = new ObjectMapper();
			
			// ignore unknown properties - this is useful because we might want to add
			// some properties to the JSON which will not be translated to JCR
			// e.g. "table_search_restriction" added by the km.js.tablesearch component.
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			
			// add custom PIR deserializer
			SimpleModule pirDeserializerModule = new SimpleModule();
			pirDeserializerModule.addDeserializer(PIR.class, new PIRDeserializer());
			mapper.registerModule(pirDeserializerModule);
			
			// add custom PIR deserializer
			SimpleModule ridDeserializerModule = new SimpleModule();
			ridDeserializerModule.addDeserializer(KID.class, new KidDeserializer());
			mapper.registerModule(ridDeserializerModule);
			
			// add aggregate function deserializer
			SimpleModule aggrDeserializerModule = new SimpleModule();
			aggrDeserializerModule.addDeserializer(AggregateFunction.class, new AggregateFunctionDeserializer());
			mapper.registerModule(aggrDeserializerModule);
			
			// Add restriction operator deserializer.
			// Note that since restriction args (and hence subrestrictions) are deserialized using
			// a custom deserializer (RestrictionArgDeserializer), the custom RestrictionOperatorDeserializer is only used to
			// deserialize operators of the top-most restrictions, not subrestrictions.
			SimpleModule roDeserializerModule = new SimpleModule();
			roDeserializerModule.addDeserializer(RestrictionOperator.class, new RestrictionOperatorDeserializer());
			mapper.registerModule(roDeserializerModule);
			
			// add custom PIR serializer
			SimpleModule pirSerializerModule = new SimpleModule();
			pirSerializerModule.addSerializer(PIR.class, new PIRSerializer());
			mapper.registerModule(pirSerializerModule);
			
			// add custom PIR serializer
			SimpleModule ridSerializerModule = new SimpleModule();
			ridSerializerModule.addSerializer(KID.class, new KIDSerializer());
			mapper.registerModule(ridSerializerModule);
			
			// add aggregate function serializer
			SimpleModule aggrSerializerModule = new SimpleModule();
			aggrSerializerModule.addSerializer(AggregateFunction.class, new AggregateFunctionSerializer());
			mapper.registerModule(aggrSerializerModule);
			
			// add restriction operator serializer
			SimpleModule roSerializerModule = new SimpleModule();
			roSerializerModule.addSerializer(RestrictionOperator.class, new RestrictionOperatorSerializer());
			mapper.registerModule(roSerializerModule);
		}
		
		return mapper;
	}
	
	/**
	 * Constructs a DAL criteria object into a JCR.
	 * @param c
	 * @return
	 * @throws KommetException 
	 */
	public static JCR getJCRFromDALCriteria (Criteria c, EnvData env) throws KommetException
	{
		JCR jcr = new JCR();
		jcr.setBaseTypeId(c.getType().getKID());
		
		/*Map<String, AggregateFunctionCall> aggregateFunctions = new HashMap<String, AggregateFunctionCall>();
		if (c.getAggregateFunctions() != null)
		{
			for (AggregateFunctionCall aggr : c.getAggregateFunctions())
			{
				aggregateFunctions.put(aggr.getProperty(), aggr);
			}
		}*/
		
		Set<String> allProps = new HashSet<String>();
		allProps.addAll(c.getProperties());
		allProps.addAll(c.getNestedProperties());
		
		// if it is a criteria for a subquery, we don't want it to have ID field queried automatically, because there can be only one column queried in a subquery
		if (c.isSubquery() && !c.isIdExplicitlyAddedToSelectClause())
		{
			allProps.remove(Field.ID_FIELD_NAME);
		}
		
		for (String prop : allProps)
		{
			Property jcrProperty = new Property();
			jcrProperty.setName(prop);
			jcrProperty.setId(PIR.get(prop, c.getType(), env));
			jcrProperty.setAlias(c.getType().getField(prop, env).getInterpretedLabel(env.currentAuthData()));
			jcr.addProperty(jcrProperty);
		}
		
		if (c.getAggregateFunctions() != null)
		{
			for (AggregateFunctionCall aggr : c.getAggregateFunctions())
			{
				Property jcrProperty = new Property();
				jcrProperty.setName(aggr.getProperty());
				jcrProperty.setId(PIR.get(aggr.getProperty(), c.getType(), env));
				jcrProperty.setAggregateFunction(aggr.getFunction());
				jcr.addProperty(jcrProperty);
			}
		}
		
		// translate the WHERE clause
		if (c.getBaseRestriction() != null)
		{
			jcr.addRestriction(Restriction.fromDALRestriction(c.getBaseRestriction(), c.getType(), env));
		}
		
		// translate groupings
		if (c.getGroupByProperties() != null && !c.getGroupByProperties().isEmpty())
		{
			for (String property : c.getGroupByProperties())
			{
				Grouping grouping = new Grouping();
				grouping.setPropertyId(PIR.get(property, c.getType(), env));
				grouping.setPropertyName(property);
				jcr.addGrouping(grouping);
			}
		}
		
		// handle limit and offset
		if (c.getLimit() != null)
		{
			jcr.setLimit(c.getLimit());
		}
		
		if (c.getOffset() != null)
		{
			jcr.setOffset(c.getOffset());
		}
		
		// translate orderings
		if (c.getOrderings() != null && !c.getOrderings().isEmpty())
		{
			for (OrderBy orderBy : c.getOrderings())
			{
				Ordering ordering = new Ordering();
				ordering.setPropertyId(PIR.get(orderBy.getProperty(), c.getType(), env));
				ordering.setPropertyName(orderBy.getProperty());
				ordering.setSortDirection(orderBy.getOrder().name());
				jcr.addOrdering(ordering);
			}
		}
		
		// return translated JCR
		return jcr;
	}
	
	/**
	 * Serialize a JCR object into a JSON string.
	 * @param jcr
	 * @return
	 * @throws JcrSerializationException
	 */
	public static String serialize (JCR jcr) throws JcrSerializationException
	{
		try
		{
			return (getJCRMapper()).writeValueAsString(jcr);
		}
		catch (Exception e)
		{
			throw new JcrSerializationException("Error serializing criteria: " + e.getMessage());
		}
	}
	
	/**
	 * Deserialize a JCR object from a JSON string.
	 * @param json
	 * @return
	 * @throws KommetException
	 */
	public static JCR deserialize (String json) throws KommetException
	{	
		ObjectMapper jcrMapper = getJCRMapper();
		
		try
		{
			return (jcrMapper).readValue(json, JCR.class);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new KommetException("Error deserializing criteria: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Gets base type from JCR basing on the base type ID or name specified in the JCR.
	 * @param jcr
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public static Type getBaseType (JCR jcr, EnvData env) throws KommetException
	{
		Type type = null;
		
		if (jcr.getBaseTypeId() != null)
		{
			type = env.getType(jcr.getBaseTypeId());
			if (type == null)
			{
				throw new KommetException("Type with ID " + jcr.getBaseTypeId() + " does not exist");
			}
		}
		else if (StringUtils.hasText(jcr.getBaseTypeName()))
		{
			type = env.getType(jcr.getBaseTypeName());
			if (type == null)
			{
				throw new KommetException("Type with name " + jcr.getBaseTypeName() + " does not exist");
			}
		}
		else
		{
			throw new KommetException("Neither base type name nor ID specified in JCR");
		}
		
		return type;
	}
	
	/**
	 * Checks if the criteria is valid.
	 * 
	 * This method checks whether all properties in the select clause are either simple, or, if
	 * groupings or/and aggregate functions are used, if they are applied to all properties.
	 * @param jcr
	 * @return If there are no errors, returns an empty array. If there are errors, returns an array of
	 * i18n keys representing errors that occurred during validation.
	 * @throws KommetException 
	 */
	public static List<String> validate (JCR jcr, EnvData env) throws KommetException
	{
		List<String> errors = new ArrayList<String>();
		
		// list of properties that are neither grouped nor used in an aggregate function
		Set<PIR> plainProperties = new HashSet<PIR>();
		boolean hasAggrFunctions = false;
		boolean hasGroupings = false;
		
		Type type = getBaseType(jcr, env);
		
		if (jcr.getProperties() != null)
		{
			for (Property prop : jcr.getProperties())
			{
				PIR propId = null;
				
				if (prop.getId() != null)
				{
					propId = prop.getId();
				}
				else if (StringUtils.hasText(prop.getName()))
				{
					propId = PIR.get(prop.getName(), type, env);
				}
				else
				{
					throw new KommetException("Neither property name nor PIR are defined");
				}
				
				if (prop.getAggregateFunction() != null)
				{
					hasAggrFunctions = true;
					
					// make sure aggregate function can be used for this type
					if (!AggregateFunction.validate(prop.getAggregateFunction(), type.getField(getPropertyName(prop, type, env), env).getDataType()))
					{
						errors.add("dal.aggr.function.not.applicable.to.datatype");
					}
				}
				else
				{
					plainProperties.add(propId);
				}
			}
		}
		
		// if there are groupings, properties appearing in groupings
		// are allowed to appear in the select clause
		if (jcr.getGroupings() != null && !jcr.getGroupings().isEmpty())
		{
			hasGroupings = true;
			for (Grouping group : jcr.getGroupings())
			{
				PIR pir = PIR.get(JCRUtil.getPropertyName(group, type, env), type, env);
				plainProperties.remove(pir);
			}
		}
		
		if (!plainProperties.isEmpty() && (hasAggrFunctions || hasGroupings))
		{
			errors.add("reports.property.not.grouped.or.aggr");
		}
		
		return errors;
	}
	
	public static String pirToNestedProperty(PIR pir, Type type, EnvData env) throws KommetException
	{
		PirParseResult npd = pirToNestedPropertyData(pir, type, env);
		return npd.getQualifiedName();
	}
	
	public static String getPropertyName (Ordering ordering, Type type, EnvData env) throws KommetException
	{
		return getPropertyName(ordering.getPropertyName(), ordering.getPropertyId(), type, env);
	}
	
	public static String getPropertyName (Grouping grouping, Type type, EnvData env) throws KommetException
	{
		return getPropertyName(grouping.getPropertyName(), grouping.getPropertyId(), type, env);
	}
	
	public static String getPropertyName (Property prop, Type type, EnvData env) throws KommetException
	{
		return getPropertyName(prop.getName(), prop.getId(), type, env);
	}
	
	private static String getPropertyName (String name, PIR pir, Type type, EnvData env) throws KommetException
	{
		String propName = null;
		
		if (pir != null)
		{
			propName = JCRUtil.pirToNestedProperty(pir, type, env);
			
			if (StringUtils.hasText(name))
			{
				// if name is also defined, make sure it is the same as the name translated from PIR
				if (!name.equals(propName))
				{
					throw new KommetException("Property name '" + name + "' does not match the PIR " + pir + " (it is a PIR for property " + propName + ")");
				}
			}
		}
		else
		{
			propName = name;
		}
		
		return propName;
	}

	/**
	 * Parses a PIR and retrieves data such as the field behind this PIR and its qualified property representation.
	 * @param type
	 * @param pir
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public static PirParseResult pirToNestedPropertyData (PIR pir, Type type, EnvData env) throws KommetException
	{
		if (pir.getValue().contains("."))
		{
			// split nested property
			String firstPropertyId = pir.getValue().substring(0, pir.getValue().indexOf('.'));
			String furtherPropertiesIds = pir.getValue().substring(pir.getValue().indexOf('.') + 1);
			
			Field field = type.getField(KID.get(firstPropertyId));
			Type nestedType = null;
			
			if (field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
			{
				nestedType = ((TypeReference)field.getDataType()).getType();
			}
			else if (field.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
			{
				nestedType = ((InverseCollectionDataType)field.getDataType()).getInverseType();
			}
			else if (field.getDataTypeId().equals(DataType.ASSOCIATION))
			{
				nestedType = ((AssociationDataType)field.getDataType()).getAssociatedType();
			}
			else
			{
				throw new KommetException("Property " + field.getApiName() + " on type " + type.getQualifiedName() + " is neither an type reference nor a collection, so its nested properties cannot be accessed");
			}
			
			PirParseResult npd = pirToNestedPropertyData(new PIR(furtherPropertiesIds), nestedType, env);
			npd.setQualifiedName(field.getApiName() + "." + npd.getQualifiedName());
			return npd;
		}
		else
		{
			Field field = type.getField(KID.get(pir.getValue()));
			PirParseResult npd = new PirParseResult();
			npd.setQualifiedName(field.getApiName());
			npd.setMostNestedField(field);
			return npd;
		}
	}
	
	/*public static List<String> getColumnHeadersFromJCR (JCR jcr, EnvData env) throws KommetException
	{
		List<String> columnHeaders = new ArrayList<String>();
		
		LinkedHashMap<PIR, String> fieldsByPir = new LinkedHashMap<PIR, String>();
		
		Type type = getBaseType(jcr, env);
		if (type == null)
		{
			throw new KommetException("Could not find type for JCR" + (jcr.getBaseTypeId() == null ? ". Base type ID is null" : ""));
		}
		
		// write column names
		for (Property prop : jcr.getProperties())
		{
			String propName = JCRUtil.getPropertyName(prop, type, env);
			
			fieldsByPir.put(prop.getId() != null ? prop.getId() : PIR.get(propName, type, env), propName);
			// TODO escape header names 
			
			if (StringUtils.hasText(prop.getAlias()))
			{
				columnHeaders.add(prop.getAlias());
			}
			else
			{
				// use property label
				columnHeaders.add(type.getField(fieldsByPir.get(prop.getId())).getLabel());
			}
		}
		
		return columnHeaders;
	}*/
}