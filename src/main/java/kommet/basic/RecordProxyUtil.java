/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;

import kommet.dao.KommetPersistenceException;
import kommet.dao.RecordProxyMapping;
import kommet.data.Env;
import kommet.data.Field;
import kommet.data.NullifiedRecord;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.SpecialValue;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.koll.compiler.KommetClassLoader;
import kommet.koll.compiler.KommetCompiler;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.MiscUtils;

public class RecordProxyUtil
{	
	public static <T extends RecordProxy> Map<String, Object> getPropertyValuesByField (T obj) throws RecordProxyException
	{
		Map<String, Object> properties = new HashMap<String, Object>();
		
		if (obj == null)
		{
			return properties;
		}
		
		try
		{
			// Be sure to use getMethods, not getDeclaredMethod, because some methods can be
			// declared in the superclass ObjectProxy
			for (Method method : obj.getClass().getMethods())
			{
				if (method.isAnnotationPresent(Property.class))
				{
					Property propertyAnnotation = method.getAnnotation(Property.class);
					properties.put(propertyAnnotation.field(), method.invoke(obj));
				}
			}
			
			return properties;
		}
		catch (Exception e)
		{
			throw new RecordProxyException("Error reading property values from object: " + e.getMessage());
		}
	}
	
	/**
	 * Generates a record from an object proxy.
	 * @param proxy
	 * @param type
	 * @param maxPropertyDepth the maximal depth of properties for which records will be initialized. E.g. max depth = 1 means that only direct properties will be initialized
	 * @return
	 * @throws KommetException 
	 */
	public static Record generateRecord (RecordProxy proxy, Type type, int maxPropertyDepth, EnvData env) throws KommetException
	{
		return generateRecord(proxy, type, maxPropertyDepth, new HashMap<Integer, Record>(), env);
	}
	
	/**
	 * Generates a record from an record proxy.
	 * @param proxy
	 * @param type
	 * @param maxPropertyDepth the maximal depth of properties for which records will be initialized. E.g. max depth = 1 means that only direct properties will be initialized
	 * @return
	 * @throws KommetException 
	 */
	@SuppressWarnings("unchecked")
	private static Record generateRecord (RecordProxy proxy, Type type, int maxPropertyDepth, Map<Integer, Record> recordCache, EnvData env) throws KommetException
	{	
		// if max depth of properties has been reached, return null
		if (maxPropertyDepth == 0)
		{
			return null;
		}
		
		Record r = new Record(type);
		r.setErrors(proxy.getErrors());
		
		// read all fields by reflection and assume object fields have the same names
		// as this stub's properties
		try
		{
			Map<String, Object> properties = RecordProxyUtil.getPropertyValuesByField(proxy);
			
			for (String propertyName : properties.keySet())
			{
				if (!proxy.isSet(propertyName))
				{
					// skip uninitialized properties
					continue;
				}
				
				// skip the record property of ObjectProxy class
				// TODO handle this in a nicer way - iterate over only properties from the subclass
				if (propertyName.equals("record") || propertyName.equals("class"))
				{
					continue;
				}
				
				Field field = type.getField(propertyName);
				
				if (field == null)
				{
					throw new RecordProxyException("Type " + type.getQualifiedName() + " has no field called " + propertyName);
				}
				
				try
				{
					Object propertyValue = properties.get(propertyName);
					
					if (propertyValue == null)
					{
						// We already know that the property is set, so we just check if it has been nullified.
						// If it has, it is assigned either the value NullifiedRecord (if it's an type reference field)
						// or the regular SpecialValue.NULL value.
						
						// we want to nullify the property, so we either assign it the value NullifiedRecord (if it's an type reference field)
						// or the regular SpecialValue.NULL value.
						// note that it does not make sense to nullify collection fields because they are not stored in database, so we just leave
						// their property value as null
						if (!field.getDataType().isCollection())
						{
							propertyValue = field.getDataTypeId().equals(DataType.TYPE_REFERENCE) ? new NullifiedRecord(type) : SpecialValue.NULL;
						}
					}
					else
					{
						// if the value is an object stub itself and the field is a type reference, we will transform it to
						// a record
						if (propertyValue instanceof RecordProxy)
						{
							DataType dt = type.getField(propertyName).getDataType();
							
							if (dt.getId().equals(DataType.TYPE_REFERENCE))
							{
								// assign a new record instance to nested type reference, unless its value is null
								if (propertyValue != null)
								{
									if (recordCache.containsKey(System.identityHashCode(propertyValue)))
									{
										// if a proxy for this record has already been generated and cached
										// get it from the cache
										propertyValue = recordCache.get(System.identityHashCode(propertyValue));
									}
									else
									{
										propertyValue = generateRecord((RecordProxy)propertyValue, env.getType(((TypeReference)dt).getType().getKeyPrefix()), maxPropertyDepth - 1, recordCache, env);
									}
								}
							}
							else
							{
								throw new RecordProxyException("Field value is an object proxy, but an attempt is made to assign it to a field that is not an type reference");
							}
						}
						else if (propertyValue instanceof List<?>)
						{
							DataType dt = type.getField(propertyName).getDataType();
							
							if (dt.getId().equals(DataType.INVERSE_COLLECTION) || dt.getId().equals(DataType.ASSOCIATION))
							{
								Type proxyType = null;
								if (dt.getId().equals(DataType.INVERSE_COLLECTION))
								{
									proxyType = ((InverseCollectionDataType)dt).getInverseType();
								}
								else if (dt.getId().equals(DataType.ASSOCIATION))
								{
									proxyType = ((AssociationDataType)dt).getAssociatedType();
								}
								
								if (maxPropertyDepth > 1)
								{
									// assign a new record instance to nested type reference, unless its value is null
									if (propertyValue != null)
									{
										List<Record> records = new ArrayList<Record>();
										for (RecordProxy collectionProxy : (List<RecordProxy>)propertyValue)
										{
											if (recordCache.containsKey(System.identityHashCode(propertyValue)))
											{
												// if a proxy for this record has already been generated and cached
												// get it from the cache
												records.add((Record)recordCache.get(System.identityHashCode(collectionProxy)));
											}
											else
											{
												records.add(generateRecord(collectionProxy, proxyType, maxPropertyDepth - 1, recordCache, env));
											}
										}
										
										propertyValue = records;
									}
								}
								else
								{
									// although the nested property has values, they are deeper than the max level, so we skip the whole collection
									propertyValue = null;
								}
							}
							else
							{
								throw new RecordProxyException("Field value is a collection of object proxies, but an attempt is made to assign it to a field that is not of collection data type");
							}
						}
					}

					r.setField(propertyName, propertyValue);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					Object propVal = properties.get(propertyName);
					throw new RecordProxyException("Error setting value '" + properties.get(propertyName) + "' (of type " +  propVal != null ? propVal.getClass().getName() : "<unknown>" + ") for property " + propertyName + ": " + e.getMessage());
				}
			}
			
			return r;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RecordProxyException("Error generating record from proxy: " + e.getMessage());
		}
	}

	public static Type getProxyType (RecordProxy obj, EnvData env) throws KommetException
	{
		Entity entityAnnotation = obj.getClass().getAnnotation(Entity.class);
		if (entityAnnotation == null)
		{
			throw new KommetPersistenceException("Cannot get object type for class " + obj.getClass().getName() + " because the class is not annotated with @" + Entity.class.getName());
		}
		
		// get the API name of the type
		Type type = env.getType(entityAnnotation.type());
		
		if (type == null)
		{
			throw new KommetPersistenceException("Class " + obj.getClass().getName() + " is annotated with @Entity, but type " + entityAnnotation.type() + " does not exist");
		}
		
		return type;
	}
	
	/**
	 * Generates a custom proxy from a record.
	 * @param record The record from which proxy is generated
	 * @param env Current environment
	 * @param compiler 
	 * @return
	 * @throws KommetException
	 */
	public static RecordProxy generateCustomTypeProxy (Record record, EnvData env, KommetCompiler compiler) throws KommetException
	{
		return generateProxy(record, false, RecordProxyType.CUSTOM, new HashMap<Integer, RecordProxy>(), env, compiler);
	}
	
	/**
	 * Generates a standard proxy from a record.
	 * @param record The record from which proxy is generated
	 * @param env Current environment
	 * @param compiler 
	 * @return
	 * @throws KommetException
	 */
	public static RecordProxy generateStandardTypeProxy (Record record, EnvData env, KommetCompiler compiler) throws KommetException
	{
		return generateProxy(record, false, RecordProxyType.STANDARD, new HashMap<Integer, RecordProxy>(), env, compiler);
	}
	
	public static RecordProxy generateCustomTypeProxy (java.lang.Class<? extends RecordProxy> proxyClass, Record record, boolean ignoreUninitializedFields, EnvData env) throws KommetException
	{
		return generateProxy(proxyClass, record, ignoreUninitializedFields, RecordProxyType.CUSTOM, new HashMap<Integer, RecordProxy>(), env);
	}
	
	public static RecordProxy generateStandardTypeProxy (java.lang.Class<? extends RecordProxy> proxyClass, Record record, boolean ignoreUninitializedFields, EnvData env) throws KommetException
	{
		return generateProxy(proxyClass, record, ignoreUninitializedFields, RecordProxyType.STANDARD, new HashMap<Integer, RecordProxy>(), env);
	}
	
	private static RecordProxy generateProxy (java.lang.Class<? extends RecordProxy> proxyClass, Record record, boolean ignoreUninitializedFields, RecordProxyType proxyType, Map<Integer, RecordProxy> proxyCache, EnvData env) throws RecordProxyException
	{
		RecordProxy proxy = null;
		/*ObjectProxyType proxyType = null;
		
		if (StandardObjectProxy.class.isAssignableFrom(proxyClass))
		{
			proxyType = ObjectProxyType.STANDARD;
		}
		else if (CustomObjectProxy.class.isAssignableFrom(proxyClass))
		{
			proxyType = ObjectProxyType.CUSTOM;
		}
		else
		{
			throw new ObjectProxyException("Proxy class " + proxyClass.getName() + " must extends either " + StandardObjectProxy.class.getSimpleName() + " or " + CustomObjectProxy.class.getSimpleName());
		}*/
		
		try
		{
			proxy = proxyClass.newInstance();
		}
		catch (InstantiationException e)
		{
			throw new RecordProxyException("Error instantiating proxy class " + proxyClass.getName() + ":" + e.getMessage(), e);
		}
		catch (IllegalAccessException e)
		{
			throw new RecordProxyException("Error instantiating proxy class " + proxyClass.getName() + ":" + e.getMessage(), e);
		}
		
		return initProxy(proxy, record, ignoreUninitializedFields, proxyType, proxyCache, env);
	}
	
	/**
	 * Initializes the properties of the proxy object from a record.
	 * @param proxy
	 * @param record
	 * @param ignoreUninitializedFields
	 * @param useBasicTypes 
	 * @param proxyCache Cache of object proxies already generated in this transation.<p>When proxies are generated for a record A with type reference to object B, that in turn
	 * contains an inverse collection to object A, we don't want to end up in an infinite loop generating proxies recursively for A and B. This is why we
	 * have a buffer that is a map of Record objects (their references used as key) to already generated proxies.
	 * @return the initialized proxy
	 * @throws KommetException
	 */
	@SuppressWarnings("unchecked")
	public static RecordProxy initProxy (RecordProxy proxy, Record record, boolean ignoreUninitializedFields, RecordProxyType proxyType, Map<Integer, RecordProxy> proxyCache, EnvData env) throws RecordProxyException
	{
		if (proxy.isInitialized())
		{
			throw new RecordProxyException("Cannot initialized proxy for class " + proxy.getClass().getName() + ". Some fields of the proxy are already initialized. Method initProxy can only be used to initialized new proxies.");
		}
		
		// Check if the proxy has not already been generated in this transaction.
		// This will prevent us from generating the same proxy multiple times and possibly ending up in an infinite loop.
		// Note: the record's address in memory is used as the key in the buffer map. We need to uniquely identify records
		// for which proxies are created, and we cannot do this using their IDs because they may not be saved yet (and thus
		// they have not IDs).
		if (proxyCache.containsKey(System.identityHashCode(record)))
		{
			return proxyCache.get(System.identityHashCode(record));
		}
		else
		{
			// add this proxy to buffer
			proxyCache.put(System.identityHashCode(record), proxy);
		}
		
		// make sure information about the record's type is available
		Type type = record.getType();
		
		if (type == null)
		{
			throw new RecordProxyException("Cannot generate proxy for record whose type property is null");
		}
		
		java.lang.Class<? extends RecordProxy> proxyClass = proxy.getClass();
		
		// make sure the proxy class is annotated with @Entity
		if (!proxyClass.isAnnotationPresent(Entity.class))
		{
			throw new RecordProxyException("Proxy class " + proxyClass.getName() + " is not annotated with @" + Entity.class.getSimpleName());
		}
		
		// iterate through proxy's methods
		for (Method method : proxyClass.getMethods())
		{
			// The method has to be a getter declared either in the proxy class or the supertype RecordProxy.
			// This way we will avoid checking methods from classes higher in the hierarchy, e.g. Object.
			if (!MiscUtils.isGetter(method) || !(method.getDeclaringClass().equals(proxyClass) || method.getDeclaringClass().equals(RecordProxy.class) || method.getDeclaringClass().equals(StandardTypeRecordProxy.class)))
			{
				// if not a getter, skip it
				continue;
			}
			else
			{
				// skip getters annotated with @Transient
				if (method.isAnnotationPresent(Transient.class))
				{
					continue;
				}
				else if (!method.isAnnotationPresent(Property.class))
				{
					throw new RecordProxyException("Method " + proxyClass.getName() + "." + method.getName() + " is a getter but is not annotated with either @" + Entity.class.getSimpleName() + " or @" + Transient.class.getSimpleName());
				}
			}
			
			Method setter = null;
			try
			{
				setter = MiscUtils.getSetter(method);
			}
			catch (KommetException e)
			{
				throw new RecordProxyException("Error getting setter for getter method: " + method.getName() + ": " + e.getMessage());
			}
			
			String fieldName = method.getAnnotation(Property.class).field();
			Object propertyValue = null;
			DataType dataType = null;
			try
			{
				// read field value
				propertyValue = record.getField(fieldName, !ignoreUninitializedFields);
				Field field = type.getField(fieldName);
				if (field == null)
				{
					throw new RecordProxyException("Invalid field " + fieldName + " in @" + Property.class.getSimpleName() + " annotation. No such field on type " + type.getQualifiedName());
				}
				dataType = field.getDataType();
			}
			catch (KommetException e)
			{
				throw new RecordProxyException("Error generating proxy: " + e.getMessage(), e);
			}
			
			if (!dataType.isPrimitive())
			{
				if (dataType instanceof TypeReference)
				{
					if (!RecordProxy.class.isAssignableFrom(method.getReturnType()))
					{
						throw new RecordProxyException("Method " + method.getName() + " represents type reference property, but its return type is " + method.getReturnType().getName() + " which does not extend " + RecordProxy.class.getSimpleName());
					}
					
					java.lang.Class<? extends RecordProxy> propertyProxyClass = (java.lang.Class<? extends RecordProxy>)method.getReturnType();
					
					// Properties createdBy and lastModifiedBy are treated in a special way, because they are declared as RecordProxy, not as User, in the
					// StandardTypeRecordProxy class. This is why their type has to be determined manually for standard type proxies.
					// But for custom type proxies these fields are generated automatically, so their class should be correct.
					if (propertyProxyClass.isAssignableFrom(propertyProxyClass) && RecordProxyType.STANDARD.equals(proxyType) && (fieldName.equals(Field.CREATEDBY_FIELD_NAME) || fieldName.equals(Field.LAST_MODIFIED_BY_FIELD_NAME)))
					{
						propertyProxyClass = User.class;
					}
					
					propertyValue = propertyValue != null ? generateProxy(propertyProxyClass, (Record)propertyValue, true, proxyType, proxyCache, env) : null;
				}
				else if (dataType instanceof InverseCollectionDataType)
				{
					// TODO test this case
					if (!ArrayList.class.isAssignableFrom(method.getReturnType()))
					{
						throw new RecordProxyException("Method " + method.getName() + " represents collection property, but its return type is " + method.getReturnType().getName() + " which does not extend " + ArrayList.class.getSimpleName());
					}
					
					if (propertyValue != null)
					{
						// iterate through the collection
						// also handle empty collections (i.e. when propertyValue is null)
						ArrayList<RecordProxy> proxyCollection = new ArrayList<RecordProxy>();
						
						// TODO - think (perhaps) of a more reliable solution for the problem described below
						// We need to instantiate the item of the inverse collection proxy, but cannot do it because
						// we don't have access to the lists parametrized parameter (or do we have it?), nor to the
						// env's class loader. This is why we rely on the class loader read from the proxy
						// (proxy.getClass().getClassLoader()). But can we be sure that this class loader will
						// always be set for object proxy classes? Perhaps we should instantiate the collection item proxy
						// class in another way?
						ClassLoader classLoader = proxy.getClass().getClassLoader();
						if (classLoader == null)
						{
							throw new RecordProxyException("Instance of record proxy was not created with a Kommet class loader, since it is null");
						}
						
						String className = MiscUtils.userToEnvPackage(((InverseCollectionDataType)dataType).getInverseType().getQualifiedName(), env);
						className = classNameByProxyType(className, proxyType, classLoader);
						
						java.lang.Class<? extends RecordProxy> collectionItemType = null;
						try
						{
							collectionItemType = (java.lang.Class<? extends RecordProxy>)(proxy.getClass().getClassLoader()).loadClass(className);
						}
						catch (ClassNotFoundException e)
						{
							throw new RecordProxyException("Inverse collection item proxy class " + className + " not found by class loader");
						}
						
						for (Record collectionItem : (List<Record>)propertyValue)
						{
							// generate a proxy for each item in the inverse collection
							proxyCollection.add(generateProxy(collectionItemType, (Record)collectionItem, true, proxyType, proxyCache, env));
						}
						
						propertyValue = proxyCollection;
					}
				}
				else if (dataType instanceof AssociationDataType)
				{
					// TODO test this case
					if (!ArrayList.class.isAssignableFrom(method.getReturnType()))
					{
						throw new RecordProxyException("Method " + method.getName() + " represents collection property, but its return type is " + method.getReturnType().getName() + " which does not extend " + ArrayList.class.getSimpleName());
					}
					
					if (propertyValue != null)
					{
						// iterate through the collection
						// also handle empty collections (i.e. when propertyValue is null)
						ArrayList<RecordProxy> proxyCollection = new ArrayList<RecordProxy>();
						
						// TODO - think (perhaps) of a more reliable solution for the problem described below
						// We need to instantiate the item of the inverse collection proxy, but cannot do it because
						// we don't have access to the lists parametrized parameter (or do we have it?), nor to the
						// env's class loader. This is why we rely on the class loader read from the proxy
						// (proxy.getClass().getClassLoader()). But can we be sure that this class loader will
						// always be set for object proxy classes? Perhaps we should instantiate the collection item proxy
						// class in another way?
						ClassLoader classLoader = proxy.getClass().getClassLoader();
						if (classLoader == null)
						{
							throw new RecordProxyException("Instance of object proxy was not created with a Kommet class loader, since it is null");
						}
						
						String className = MiscUtils.userToEnvPackage(((AssociationDataType)dataType).getAssociatedType().getQualifiedName(), env);
						className = classNameByProxyType(className, proxyType, classLoader);
						
						java.lang.Class<? extends RecordProxy> collectionItemType = null;
						try
						{
							collectionItemType = (java.lang.Class<? extends RecordProxy>)(proxy.getClass().getClassLoader()).loadClass(className);
						}
						catch (ClassNotFoundException e)
						{
							throw new RecordProxyException("Association collection item proxy class " + className + " not found by class loader");
						}
						
						for (Record collectionItem : (List<Record>)propertyValue)
						{
							// generate a proxy for each item in the inverse collection
							proxyCollection.add(generateProxy(collectionItemType, (Record)collectionItem, true, proxyType, proxyCache, env));
						}
						
						propertyValue = proxyCollection;
					}
				}
			}
			
			// Set property value only if it is not null. If we were setting null values here, it would
			// make the property setters to set the isSet flag for this property to true.
			// This flag is only needed when property is nullified, and this is never done through
			// this method as it is only called for uninitialized proxies.
			if (propertyValue != null)
			{
				try
				{
					setter.invoke(proxy, propertyValue);
				}
				catch (Exception e)
				{
					throw new RecordProxyException("Error calling setter " + setter.getName() + " with value " + propertyValue + ". Nested: " + e.getMessage());
				}
			}
		}
		
		// return the generated proxy
		return proxy;
	}
	
	private static String classNameByProxyType(String className, RecordProxyType proxyType, ClassLoader classLoader) throws RecordProxyException
	{
		// depending on proxy type, the new instance of the proxy will be obtained either from the
		// Kommet class loader, or the standard class loader
		if (proxyType == RecordProxyType.CUSTOM)
		{
			if (!(classLoader instanceof KommetClassLoader))
			{
				throw new RecordProxyException("Instance of object proxy " + className + " was not created with a Kommet class loader");
			}
		}
		else
		{
			// class name is env-specific, but when loaded from env definition, it will contain the env prefix
			className = removeEnvPrefix(className);
		}
		
		return className;
	}

	private static String removeEnvPrefix(String className) throws RecordProxyException
	{
		if (!className.startsWith(Env.ENV_PACKAGE_PREFIX))
		{
			throw new RecordProxyException("Class name does not start with a name prefix");
		}
		
		className = className.substring(Env.ENV_PACKAGE_PREFIX.length() + 1);
		return className.substring(className.indexOf('.') + 1);
	}

	public static RecordProxy generateStandardTypeProxy (Record record, boolean ignoreUninitializedFields, EnvData env, KommetCompiler compiler) throws KommetException
	{
		return generateProxy(record, ignoreUninitializedFields, RecordProxyType.STANDARD, new HashMap<Integer, RecordProxy>(), env, compiler);
	}
	
	public static RecordProxy generateCustomTypeProxy (Record record, boolean ignoreUninitializedFields, EnvData env, KommetCompiler compiler) throws KommetException
	{
		return generateProxy(record, ignoreUninitializedFields, RecordProxyType.CUSTOM, new HashMap<Integer, RecordProxy>(), env, compiler);
	}

	public static java.lang.Class<? extends RecordProxy> getProxyClass (Type type, RecordProxyType proxyType, EnvData env) throws KommetPersistenceException
	{
		RecordProxyMapping mapping = proxyType.equals(RecordProxyType.STANDARD) ? env.getBasicTypeProxyMapping((type.getKID())) : env.getCustomTypeProxyMapping(type.getKID());
		if (mapping == null)
		{
			throw new KommetPersistenceException("No object proxy mapping found for type " + type.getQualifiedName());
		}
		
		return mapping.getProxyClass();
	}
	
	@SuppressWarnings("unchecked")
	private static RecordProxy generateProxy (Record record, boolean ignoreUninitializedFields, RecordProxyType proxyType, Map<Integer, RecordProxy> proxyCache, EnvData env, KommetCompiler compiler) throws KommetException
	{	
		Type type = record.getType();
		
		// get proxy mapping for this type
		// depending on the "useBasicTypeProxies" parameter use either basic or custom proxy mappings
		RecordProxyMapping mapping = proxyType.equals(RecordProxyType.STANDARD) ? env.getBasicTypeProxyMapping((type.getKID())) : env.getCustomTypeProxyMapping(type.getKID());
		if (mapping == null)
		{
			throw new KommetPersistenceException("No record proxy mapping found for type " + record.getType().getQualifiedName());
		}
		
		RecordProxy proxy = null;
		try
		{
			// get instance of the proxy class
			proxy = (RecordProxy)compiler.getClass(mapping.getProxyClass().getName(), false, env).newInstance();
		}
		catch (KommetException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new KommetException("Error instantiating proxy class: " + e.getMessage());
		}
		
		proxyCache.put(System.identityHashCode(record), proxy);
		
		// set all mapped properties
		for (String property : mapping.getPropertyMappings().keySet())
		{
			if (!record.isSet(property))
			{
				// skip properties that are not set - do not set them using object proxy
				// setter method, because this would result in non-initialized properties
				// on record having isInitialized = true on object proxy
				continue;
			}
			
			String fieldName = mapping.getPropertyMappings().get(property).getColumn();
			Object value = record.getField(fieldName, !ignoreUninitializedFields);
			
			if (value != null)
			{
				// if the field represents a field reference, it has to be turned into a proxy before it can be assigned
				if (type.getField(fieldName).getDataTypeId().equals(DataType.TYPE_REFERENCE))
				{
					if (!(value instanceof NullifiedRecord))
					{
						if (proxyCache.containsKey(System.identityHashCode(value)))
						{
							value = proxyCache.get(System.identityHashCode(value));
						}
						else
						{
							// ignore uninitialized fields on a nested type reference, because
							// they may not have been included in the query
							RecordProxy generatedProxy = generateProxy((Record)value, true, proxyType, proxyCache, env, compiler);
							proxyCache.put(System.identityHashCode(value), generatedProxy);
							value = generatedProxy;
						}
					}
					else
					{
						// the record representing the type reference is in fact special value null,
						// so we nullify it on the proxy as well
						proxy.nullify(fieldName);
						value = null;
					}
				}
				// if the field is a collection (inverse collection or association)
				// it needs to be converted into a list of object proxies
				else if (type.getField(fieldName).getDataTypeId().equals(DataType.INVERSE_COLLECTION) || type.getField(fieldName).getDataTypeId().equals(DataType.ASSOCIATION))
				{
					List<Record> collectionRecords = (List<Record>)value;
					List<RecordProxy> collectionProxies = new ArrayList<RecordProxy>();
					
					if (collectionRecords != null)
					{
						for (Record rec : collectionRecords)
						{
							if (rec == null)
							{
								throw new RecordProxyException("Null value in collection field " + type.getQualifiedName() + "." + fieldName);
							}
							// this check is a bit superfluous, because records in a collection
							// should never be a NullifiedRecord
							else if (!(rec instanceof NullifiedRecord))
							{
								if (proxyCache.containsKey(System.identityHashCode(rec)))
								{
									collectionProxies.add(proxyCache.get(System.identityHashCode(rec)));
								}
								else
								{
									// convert each record in the collection into a proxy
									RecordProxy generatedProxy = generateProxy(rec, true, proxyType, proxyCache, env, compiler);
									proxyCache.put(System.identityHashCode(rec), generatedProxy);
									collectionProxies.add(generatedProxy);
								}
							}
							else
							{
								throw new RecordProxyException("Item in collection " + fieldName + " is an instance of NullifiedRecord, which is not allowed");
							}
						}
					}
					
					value = collectionProxies;
				}
			}
			
			if (SpecialValue.isNull(value))
			{
				// if the value on the record is SpecialValue.NULL, it will be represented on the object proxy
				// by a combination of two settings - the value of the property will be null, and it will be
				// additionally marked as nullified.
				proxy.nullify(fieldName);
				value = null;
			}
			
			try
			{
				PropertyUtils.setProperty(proxy, property, value);
			}
			catch (IllegalAccessException e)
			{
				throw new KommetPersistenceException("Error setting property " + property + ": " + e.getMessage(), e);
			}
			catch (InvocationTargetException e)
			{
				throw new KommetPersistenceException("Error setting property " + property + ": " + e.getMessage(), e);
			}
			catch (NoSuchMethodException e)
			{
				throw new KommetPersistenceException("Error setting property " + property + ": " + e.getMessage(), e);
			}
		}
		
		return proxy;
	}
}