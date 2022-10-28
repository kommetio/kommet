/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.json;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kommet.auth.AuthData;
import kommet.basic.RecordProxy;
import kommet.dao.queries.QueryResult;
import kommet.data.Field;
import kommet.data.NoSuchFieldException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.persistence.Property;
import kommet.utils.MiscUtils;

/**
 * Utility class for different JSON operations, including serialization and
 * deserialization.
 * 
 * @author Radek Krawiec
 * @since 2014
 */
public class JSON
{
	public static String getPropertyJSON(String name, String value)
	{
		return "\"" + name + "\": " + (value != null ? "\"" + value + "\"" : "null");
	}
	
	public static String getBooleanPropertyJSON(String name, Boolean value)
	{
		return "\"" + name + "\": " + (value != null ? value : "null");
	}
	
	public static String serializeObjectProxy(RecordProxy proxy, AuthData authData) throws JsonSerializationException
	{
		return serializeObjectProxy(proxy, null, null, authData, null, new HashMap<Object, String>(), null);
	}
	
	public static String serializeObjectProxy(RecordProxy proxy, Set<String> propertiesToSerialize, AuthData authData) throws JsonSerializationException
	{
		return serializeObjectProxy(proxy, propertiesToSerialize, null, authData, null, new HashMap<Object, String>(), null);
	}
	
	public static String serializeRecord (Record record, AuthData authData) throws JsonSerializationException
	{
		Set<KID> visitedRecordIds = new HashSet<KID>();
		return serializeRecord(record, visitedRecordIds, authData);
	}
	
	/**
	 * Serializes a record to JSON string.
	 * @param record The record to be serialized
	 * @param authData Authentication data of the user
	 * @return
	 * @throws KommetException 
	 */
	private static String serializeRecord (Record record, Set<KID> visitedRecordIds, AuthData authData) throws JsonSerializationException
	{
		StringBuilder sb = new StringBuilder("{ ");
		List<String> values = new ArrayList<String>();
		
		KID recordId = null;
		
		try
		{
			recordId = record.attemptGetKID();
		}
		catch (KommetException e)
		{
			throw new JsonSerializationException(e.getMessage());
		}
		
		if (recordId != null)
		{
			// if this record has already been visited (because of going back and forth between parent and children in collection-type reference properties),
			// skip it - otherwise we will get an infinite loop
			if (visitedRecordIds.contains(recordId))
			{
				return "{ \"" + Field.ID_FIELD_NAME + "\": \"" + recordId + "\" }";
			}
			
			visitedRecordIds.add(recordId);
		}
		
		for (String fieldName : record.getFieldValues().keySet())
		{
			Object fieldVal;
			try
			{
				fieldVal = record.getField(fieldName);
			}
			catch (KommetException e)
			{
				throw new JsonSerializationException("Error reading value of field " + fieldName + ". Nested: " + e.getMessage());
			}
			
			Field field = null;
			
			try
			{
				field = record.getType().getField(fieldName);
			}
			catch (KommetException e)
			{
				throw new JsonSerializationException("Error getting field " + fieldName + ". Nested: " + e.getMessage());
			}
			
			if (fieldVal == null)
			{
				values.add("\"" + fieldName + "\": null");
			}
			else if (field.getDataTypeId().equals(DataType.DATETIME))
			{
				try
				{
					fieldVal = MiscUtils.formatDateTimeByUserLocale((Date)fieldVal, authData);
					values.add("\"" + fieldName + "\": \"" + fieldVal + "\"");
				}
				catch (KommetException e)
				{
					throw new JsonSerializationException("Error reading date/time value of field " + fieldName + ". Nested: " + e.getMessage());
				}
			}
			else if (field.getDataTypeId().equals(DataType.DATE))
			{
				try
				{
					fieldVal = MiscUtils.formatDateByUserLocale((Date)fieldVal, authData);
					values.add("\"" + fieldName + "\": \"" + fieldVal + "\"");
				}
				catch (KommetException e)
				{
					throw new JsonSerializationException("Error reading date value of field " + fieldName + ". Nested: " + e.getMessage());
				}
			}
			else if (field.getDataType().isCollection())
			{
				@SuppressWarnings("unchecked")
				List<Record> records = (ArrayList<Record>)fieldVal;
				
				List<String> recordJSON = new ArrayList<String>();
				for (Record r : records)
				{
					recordJSON.add(serializeRecord(r, visitedRecordIds, authData));
				}
				
				values.add("\"" + fieldName + "\": [" + MiscUtils.implode(recordJSON, ", ") + "]");
			}
			else if (fieldVal instanceof Record)
			{
				values.add("\"" + fieldName + "\": " + serializeRecord((Record)fieldVal, visitedRecordIds, authData));
			}
			else
			{
				// we know that fieldVal is not null because it has been checked earlier
				// so we just serialize its value to string as is
				values.add("\"" + fieldName + "\": \"" + fieldVal + "\"");
			}
		}
		
		// if its a result of a group by query
		if (record instanceof QueryResult)
		{
			QueryResult qr = ((QueryResult)record);
			for (String aggrField : qr.getAggregateValues().keySet())
			{
				Object fieldVal;
				try
				{
					fieldVal = qr.getAggregateValue(aggrField);
				}
				catch (KommetException e)
				{
					throw new JsonSerializationException("Error reading value of aggregate field " + aggrField + ". Nested: " + e.getMessage());
				}
				
				if (fieldVal == null)
				{
					values.add("\"" + aggrField + "\": null");
				}
				else
				{
					values.add("\"" + aggrField + "\": \"" + fieldVal + "\"");
				}
			}
			
			for (String groupByField : qr.getGroupByValues().keySet())
			{
				Object fieldVal;
				try
				{
					fieldVal = qr.getGroupByValue(groupByField);
				}
				catch (KommetException e)
				{
					throw new JsonSerializationException("Error reading value of group by field " + groupByField + ". Nested: " + e.getMessage());
				}
				
				if (fieldVal == null)
				{
					values.add("\"" + groupByField + "\": null");
				}
				else
				{
					values.add("\"" + groupByField + "\": \"" + fieldVal + "\"");
				}
			}
		}
		
		sb.append(MiscUtils.implode(values, ", "));
		
		return sb.append(" }").toString();
	}
	
	/**
	 * Escape string so that it can be used in JSON.
	 * @param s String to escape
	 * @return
	 * @throws KommetException 
	 * @throws JsonProcessingException 
	 */
	public static String escape (String s) throws KommetException
	{
		try
		{
			return s != null ? MiscUtils.trim((new ObjectMapper()).writeValueAsString(s), '"') : null;
		}
		catch (JsonProcessingException e)
		{
			throw new KommetException("Error escaping string for JSON. Nested: " + e.getMessage());
		}
	}
	
	public static String escapeHTML (String s) throws KommetException
	{
		if (s == null)
		{
			return s;
		}
		
		s = escape(s);
		return s.replaceAll("\\/", "\\/");
	}
	
	public static String serialize (Object obj, AuthData authData) throws JsonSerializationException
	{
		return serialize(obj, authData, null, new HashMap<Object, String>(), null);
	}
	
	/**
	 * Serializes any kind of object into JSON. Works both for single objects and for collections.
	 * <p>
	 * Different serialization rules are applied to three major classes of objects:
	 * <ul>
	 * <li>object proxies</li>
	 * <li>records</li>
	 * <li>all other POJOs</li>
	 * </ul>
	 * </p>
	 * @param obj
	 * @param authData
	 * @return
	 * @throws JsonSerializationException
	 */
	public static String serialize (Object obj, AuthData authData, AdditionalPropertySerializer additionalSerializer) throws JsonSerializationException
	{
		return serialize(obj, authData, null, new HashMap<Object, String>(), additionalSerializer);
	}
	
	@SuppressWarnings("unchecked")
	private static String serialize (Object obj, AuthData authData, Map<Class<?>, List<Method>> gettersPerClass, Map<Object, String> cachedRecordProxies, AdditionalPropertySerializer additionalSerializer) throws JsonSerializationException
	{
		if (obj instanceof RecordProxy)
		{
			return serializeObjectProxy((RecordProxy)obj, null, null, authData, gettersPerClass, cachedRecordProxies, additionalSerializer);
		}
		else if (obj instanceof Collection<?>)
		{
			return serialize((Collection<?>)obj, authData, cachedRecordProxies, additionalSerializer);
		}
		else if (obj instanceof Record)
		{
			return serializeRecord((Record)obj, authData);
		}
		else if (obj instanceof Type)
		{
			return serialize((Type)obj, authData, additionalSerializer);
		}
		else if (obj instanceof LinkedHashMap<?, ?>)
		{
			try
			{
				return serialize((LinkedHashMap<Object, Object>)obj, authData);
			}
			catch (KommetException e)
			{
				throw new JsonSerializationException("Error serializing map: " + e.getMessage());
			}
		}
		else if (obj instanceof Date)
		{
			try
			{
				return "\"" + MiscUtils.formatDateTimeByUserLocale((Date)obj, authData) + "\"";
			}
			catch (KommetException e)
			{
				throw new JsonSerializationException("Error serializing date: " + e.getMessage());
			}
		}
		else if (obj instanceof BigDecimal)
		{
			return ((BigDecimal)obj).toPlainString();
		}
		else if (obj instanceof Boolean)
		{
			return obj.toString();
		}
		else if (obj instanceof KID)
		{
			return "\"" + ((KID)obj).getId() + "\"";
		}
		else if (obj instanceof String)
		{
			return "\"" + StringEscapeUtils.escapeJson((String)obj) + "\"";
		}
		else if (obj instanceof Integer || obj instanceof Long)
		{
			return obj.toString();
		}
		else if (obj == null)
		{
			return "null";
		}
		else
		{
			try
			{
				return (new ObjectMapper()).writeValueAsString(obj);
			}
			catch (JsonProcessingException e)
			{
				throw new JsonSerializationException("Error serializing object of type " + obj.getClass().getName() + ". Nested: " + e.getMessage());
			}
		}
		/*else
		{
			throw new JsonSerializationException("Serialization of type " + obj.getClass().getName() + " not implemented");
		}*/
	}

	public static String serialize (RecordProxy proxy, AuthData authData) throws JsonSerializationException
	{
		return serializeObjectProxy(proxy, authData);
	}
	
	@SuppressWarnings("unchecked")
	public static String serialize (LinkedHashMap<Object, Object> map, AuthData authData) throws KommetException
	{
		StringBuilder sb = new StringBuilder("{");
		
		List<String> serializedItems = new ArrayList<String>();
		
		for (Object key : map.keySet())
		{
			if (key == null)
			{
				throw new KommetException("Cannot deserialize map with null key");
			}
			
			Object val = map.get(key);
			
			String stringKey = key.toString();
			if (key instanceof KID)
			{
				stringKey = ((KID) key).getId();
			}
			
			if (val instanceof LinkedHashMap<?, ?>)
			{
				serializedItems.add("\"" + stringKey + "\": " + serialize((LinkedHashMap<String,Object>)val, authData));
			}
			else
			{
				serializedItems.add("\"" + stringKey + "\": " + serialize(val, authData));
			}
		}
		
		sb.append(MiscUtils.implode(serializedItems, ", ")).append("}");
		return sb.toString();
	}
	
	public static String serialize (Type type, AuthData authData, AdditionalPropertySerializer additionalSerializer) throws JsonSerializationException
	{
		List<String> serializedProperties = new ArrayList<String>();
		
		serializedProperties.add("\"id\": \"" + type.getKID() + "\"");
		serializedProperties.add("\"qualifiedName\": \"" + type.getQualifiedName() + "\"");
		serializedProperties.add("\"label\": \"" + type.getLabel() + "\"");
		serializedProperties.add("\"pluralLabel\": \"" + type.getPluralLabel() + "\"");
		serializedProperties.add("\"package\": \"" + type.getPackage() + "\"");
		serializedProperties.add("\"apiName\": \"" + type.getApiName() + "\"");
		serializedProperties.add("\"isBasic\": " + type.isBasic());
		serializedProperties.add("\"keyPrefix\": \"" + type.getKeyPrefix().getPrefix() + "\"");
		
		List<String> serializedFields = new ArrayList<String>();
		
		for (Field field : type.getFields())
		{
			List<String> fieldProps = new ArrayList<String>();
			fieldProps.add("\"id\": \"" + field.getKID() + "\"");
			fieldProps.add("\"apiName\": \"" + field.getApiName() + "\"");
			fieldProps.add("\"dataType\": \"" + field.getDataType().getName() + "\"");
			fieldProps.add("\"dataTypeId\": " + field.getDataType().getId());
			fieldProps.add("\"isPrimitive\": " + field.getDataType().isPrimitive());
			fieldProps.add("\"label\": \"" + field.getLabel() + "\"");
			
			if (additionalSerializer != null)
			{
				fieldProps.addAll(additionalSerializer.getProperties(field));
			}
			
			serializedFields.add("{" + MiscUtils.implode(fieldProps, ", ") + "}");
		}
		
		if (additionalSerializer != null)
		{
			List<String> additionalProps = additionalSerializer.getProperties(type);
			if (additionalProps != null)
			{
				serializedProperties.addAll(additionalProps);
			}
		}
		
		serializedProperties.add("\"fields\": [" + MiscUtils.implode(serializedFields, ", ") + "]");
		
		return "{" + MiscUtils.implode(serializedProperties, ", ") + "}";
	}
	
	public static String serialize (Collection<?> objects, AuthData authData, Map<Object, String> cachedRecordProxies, AdditionalPropertySerializer additionalSerializer) throws JsonSerializationException
	{
		StringBuilder sb = new StringBuilder("[");
		List<String> serializedObjs = new ArrayList<String>();
		
		// this map will store getters from each class annotated with @Property
		// so that they don't have to be found every time an object of this class is serialized
		Map<Class<?>, List<Method>> gettersPerClass = new HashMap<Class<?>, List<Method>>();

		for (Object obj : objects)
		{
			if (obj == null)
			{
				serializedObjs.add("null");
				continue;
			}
			
			// find property getters for this class, as long as it is an object proxy or a POJO
			if (!(obj instanceof Record) && !gettersPerClass.containsKey(obj.getClass()))
			{
				gettersPerClass.put(obj.getClass(), getPropertyGetters(obj.getClass()));
			}
			
			// serialize each object independently - they can even be of different types
			serializedObjs.add(JSON.serialize(obj, authData, gettersPerClass, cachedRecordProxies, additionalSerializer));
		}

		sb.append(MiscUtils.implode(serializedObjs, ", "));
		sb.append("]");
		return sb.toString();
	}
	
	private static List<Method> getPropertyGetters(Class<? extends Object> cls)
	{
		List<Method> getters = new ArrayList<Method>();
		
		for (Method m : cls.getMethods())
		{
			if (m.isAnnotationPresent(Property.class))
			{
				getters.add(m);
			}
		}
		
		return getters;
	}

	/**
	 * Serialize object proxy to JSON.
	 * @param proxy Proxy object to be serialized
	 * @param propertiesToSerialize Optional set of property names to be serialized
	 * @param superProperty
	 * @param authData
	 * @return
	 * @throws JsonSerializationException
	 */
	private static String serializeObjectProxy(RecordProxy proxy, Set<String> propertiesToSerialize, String superProperty, AuthData authData, Map<Class<?>, List<Method>> gettersPerClass, Map<Object, String> cachedRecordProxies, AdditionalPropertySerializer additionalSerializer) throws JsonSerializationException
	{
		if (proxy == null)
		{
			return "null";
		}
		
		List<String> propertyJSON = new ArrayList<String>();
		
		if (gettersPerClass == null)
		{
			gettersPerClass = new HashMap<Class<?>, List<Method>>();
		}
		
		List<Method> getters = gettersPerClass.get(proxy.getClass());
		
		// if getters for this class have not been found yet, do this now
		if (getters == null)
		{
			getters = getPropertyGetters(proxy.getClass());
			gettersPerClass.put(proxy.getClass(), getters);
		}
		
		// the topmost property is not serialized yet, but we need to set it as cached to avoid circular reference
		cachedRecordProxies.put(proxy, "{ \"" + Field.ID_FIELD_NAME + "\": \"" + proxy.getId() + "\" }");
		
		// iterate through all getter methods
		for (Method method : getters)
		{
			if (!method.isAnnotationPresent(Property.class))
			{
				continue;
			}
			
			String prop = null;
			
			try
			{
				prop = MiscUtils.getPropertyFromGetter(method.getName());
			}
			catch (KommetException e)
			{
				throw new JsonSerializationException("Error getting property name from getter method name: " + e.getMessage());
			}
			
			// Skip two kinds of properties:
			// - uninitialized properties
			// - properties not specified in the propertiesToSerialize collection, unless they are type references or collection themselves. This
			// is because property "user" will not be specified in the propertiesToSerialize collection, but if it's an object proxy, its subproperties
			// (such as "user.name") may be specified there.
			if (!proxy.isSet(prop) || (propertiesToSerialize != null && !RecordProxy.class.isAssignableFrom(method.getReturnType()) && !Collection.class.isAssignableFrom(method.getReturnType()) && !propertiesToSerialize.contains((superProperty != null ? superProperty + "." : "") + prop)))
			{
				continue;
			}
			
			Object val;
			try
			{
				val = method.invoke(proxy);
			}
			catch (Exception e)
			{
				throw new JsonSerializationException("Error invoking getter of property " + prop + ". Nested: " + e.getMessage());
			}
			
			if (val == null)
			{
				propertyJSON.add(getPropertyJSON(prop, null));
			}
			else if (val instanceof RecordProxy)
			{
				// check if this record was already cached
				if (cachedRecordProxies != null && cachedRecordProxies.containsKey(val))
				{
					propertyJSON.add("\"" + prop + "\": " + cachedRecordProxies.get(val));
				}
				else
				{
					String json = serializeObjectProxy((RecordProxy)val, propertiesToSerialize, (superProperty != null ? superProperty + "." : "") + prop, authData, gettersPerClass, cachedRecordProxies, additionalSerializer);
					propertyJSON.add("\"" + prop + "\": " + json);
					
					if (cachedRecordProxies != null)
					{
						cachedRecordProxies.put(val, json);
					}
				}
			}
			else if (val instanceof Collection<?>)
			{
				propertyJSON.add("\"" + prop + "\": " + serialize((Collection<?>)val, authData, gettersPerClass, cachedRecordProxies, additionalSerializer));
			}
			else if (val instanceof Date)
			{
				try
				{
					propertyJSON.add(getPropertyJSON(prop, MiscUtils.formatDateTimeByUserLocale((Date)val, authData)));
				}
				catch (KommetException e)
				{
					throw new JsonSerializationException("Error serializing value " + val + " of field " + prop + ". Nested: " + e.getMessage());
				}
			}
			else if (val instanceof String)
			{
				propertyJSON.add(getPropertyJSON(prop, val != null ? StringEscapeUtils.escapeJson((String)val) : "null"));
			}
			else if (val instanceof Boolean)
			{
				propertyJSON.add(getBooleanPropertyJSON(prop, (Boolean)val));
			}
			else
			{
				// we just output the string value
				propertyJSON.add(getPropertyJSON(prop, val != null ? val.toString() : "null"));
			}
		}
		
		if (additionalSerializer != null)
		{
			propertyJSON.addAll(additionalSerializer.getProperties(proxy));
		}
		
		return "{ " + MiscUtils.implode(propertyJSON, ", ") + " }";
	}

	public static String serializeObjectProxies(Collection<? extends RecordProxy> beans, Set<String> properties, AuthData authData) throws JsonSerializationException
	{
		if (beans == null)
		{
			return "null";
		}

		StringBuilder sb = new StringBuilder("[");
		List<String> serializedObjs = new ArrayList<String>();

		for (RecordProxy obj : beans)
		{
			serializedObjs.add(JSON.serializeObjectProxy(obj, properties, authData));
		}

		sb.append(MiscUtils.implode(serializedObjs, ", "));
		sb.append("]");
		return sb.toString();
	}

	/*public static String serialize(Object bean, Collection<String> properties, AuthData authData) throws KommetException
	{
		if (bean instanceof ObjectProxy)
		{
			throw new JsonSerializationException("Method serialize should not be used to serialize object proxies. Use serializeObjectProxy instead.");
		}
		
		List<String> jsonProps = new ArrayList<String>();
		Map<String, Set<String>> nestedProperties = new HashMap<String, Set<String>>();

		for (String prop : properties)
		{
			try
			{
				if (prop.contains("."))
				{
					String[] propParts = prop.split("\\.", 2);
					String firstProperty = propParts[0];

					if (!nestedProperties.containsKey(firstProperty))
					{
						nestedProperties.put(firstProperty, new HashSet<String>());
					}
					nestedProperties.get(firstProperty).add(propParts[1]);

					// do not process nested properties until all are gathered
					continue;
				}

				Object val = MiscUtils.getProperty(bean, prop);

				if (val instanceof Date)
				{
					val = MiscUtils.formatDateTimeByUserLocale((Date) val, authData);
				}

				if (val instanceof ObjectProxy)
				{
					// serialize nested proxy
					jsonProps.add(getPropertyJSON(prop, serialize(val, new ArrayList<String>(), authData)));
				}
				else
				{
					jsonProps.add(getPropertyJSON(prop, val != null ? val.toString() : null));
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				throw new KommetException("Cannot read property " + prop + " on type " + bean.getClass().getName() + ". Probably does not exist or is inaccessible. Nested: " + e.getMessage());
			}
		}

		// process nested properties
		for (String nestedProperty : nestedProperties.keySet())
		{
			Object val = null;

			try
			{
				val = PropertyUtils.getProperty(bean, nestedProperty);
				// System.out.println("Nested prop " + nestedProperty +
				// " (type " + bean.getClass().getName() + ") value = " + val +
				// " (" + val.getClass().getName() + ")");
			}
			catch (Exception e)
			{
				e.printStackTrace();
				throw new KommetException("Could not read property " + nestedProperty + " on object of type " + bean.getClass().getName() + ". Nested: " + e.getMessage());
			}

			if (val instanceof ArrayList<?>)
			{
				jsonProps.add("\"" + nestedProperty + "\": " + serialize((ArrayList<?>) val, nestedProperties.get(nestedProperty), authData));
			}
			else
			{
				jsonProps.add("\"" + nestedProperty + "\": " + serialize(val, nestedProperties.get(nestedProperty), authData));
			}
		}

		return "{ " + MiscUtils.implode(jsonProps, ", ") + " }";
	}*/
	
	/**
	 * Parses JSON string to a map.
	 * @param json
	 * @return
	 * @throws JsonDeserializationException
	 */
	@SuppressWarnings("unchecked")
	public static HashMap<String, Object> parseToMap (String json) throws JsonDeserializationException
	{
		try
		{
			return new ObjectMapper().readValue(json, HashMap.class);
		}
		catch (JsonParseException e)
		{
			throw new JsonDeserializationException("Error deserializing JSON. Nested exception is " + e.getMessage());
		}
		catch (JsonMappingException e)
		{
			throw new JsonDeserializationException("Error deserializing JSON. Nested exception is " + e.getMessage());
		}
		catch (IOException e)
		{
			throw new JsonDeserializationException("Error deserializing JSON. Nested exception is " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	public static Record toRecord(String objectJSON, boolean ignoreMissingFields, Type type, EnvData env) throws KommetException
	{
		HashMap<String, Object> parsedObj = null;

		try
		{
			parsedObj = new ObjectMapper().readValue(objectJSON, HashMap.class);
		}
		catch (Exception e)
		{
			throw new KommetException("Error parsing JSON into record: " + e.getMessage());
		}

		return toRecord(parsedObj, ignoreMissingFields, type, env);
	}

	/**
	 * Parses JSON string into a record
	 * 
	 * @param objectJSON
	 * @return
	 * @throws KommetException
	 */
	@SuppressWarnings("unchecked")
	public static Record toRecord(Map<String, Object> parsedObj, boolean ignoreMissingFields, Type type, EnvData env) throws KommetException
	{
		Record rec = new Record(type);

		for (String property : parsedObj.keySet())
		{
			Object val = parsedObj.get(property);
			Field field = type.getField(property);
			if (field == null)
			{
				if (ignoreMissingFields)
				{
					continue;
				}
				else
				{
					throw new NoSuchFieldException("Field " + property + " does not exist on type " + type.getQualifiedName());
				}
			}
			DataType dt = field.getDataType();

			if (DataType.TYPE_REFERENCE == dt.getId())
			{
				val = toRecord((Map<String, Object>) val, ignoreMissingFields, ((TypeReference) dt).getType(), env);
			}
			else
			{
				val = dt.getJavaValue(val);
			}

			rec.setField(property, val, env);
		}

		return rec;
	}

	/**
	 * Get JSON mapper object that has all the knowledge necessary for properly
	 * translating an object proxy into JSON, and the other way around.
	 * 
	 * @return
	 */
	/*private static ObjectWriter getObjectProxyWriter()
	{
		if (objectProxyWriter == null)
		{
			// filter that will make sure only initialized properties of an object proxy are serialized
			PropertyFilter initializedPropertyFilter = new SimpleBeanPropertyFilter()
			{
				@Override
				public void serializeAsField(Object obj, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer) throws Exception
				{
					if (!(obj instanceof ObjectProxy))
					{
						writer.serializeAsField(obj, jgen, provider);
						return;
					}
					
					System.out.print("Prop: " + writer.getFullName().getSimpleName() + " ");
					
					if (((ObjectProxy)obj).getInitializedProperties().contains(writer.getFullName().getSimpleName()))
					{
						System.out.println(" include");
						writer.serializeAsField(obj, jgen, provider);
						return;
					}
					else
					{
						System.out.println(" exclude");
						writer.serializeAsOmittedField(obj, jgen, provider);
					}
				}

				@Override
				protected boolean include(BeanPropertyWriter writer)
				{
					return true;
				}

				@Override
				protected boolean include(PropertyWriter writer)
				{
					return true;
				}
			};
			
			ObjectMapper objectProxyMapper = new ObjectMapper();
			
			// add custom KID deserializer
			SimpleModule ridDeserializerModule = new SimpleModule();
			ridDeserializerModule.addDeserializer(KID.class, new RidDeserializer());
			objectProxyMapper.registerModule(ridDeserializerModule);

			// add custom PIR serializer
			SimpleModule ridSerializerModule = new SimpleModule();
			ridSerializerModule.addSerializer(KID.class, new KIDSerializer());
			objectProxyMapper.registerModule(ridSerializerModule);
			
			FilterProvider filters = new SimpleFilterProvider().addFilter("initializedPropertyFilter", initializedPropertyFilter);
			objectProxyWriter = objectProxyMapper.writer(filters);
		}

		return objectProxyWriter;
	}*/
}