/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyType;
import kommet.basic.RecordProxyUtil;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.QueryResult;
import kommet.dao.queries.Restriction;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.koll.compiler.KommetCompiler;

/**
 * Interface that allows for manipulating DB data on a locally accessible environment.
 * 
 * @author Radek Krawiec
 * @created 26-07-2013
 *
 */
@Service
public class LocalEnvPersistenceInterface implements EnvPersistenceInterface
{
	@Inject
	DataService dataService;
	
	@Inject
	KommetCompiler compiler;
	
	//private static final Logger log = LoggerFactory.getLogger(LocalEnvPersistenceInterface.class);

	@Override
	public Type create(Type obj, AuthData authData, EnvData env) throws KommetException
	{
		return dataService.createType(obj, authData, env);
	}
	
	@Override
	public RecordProxy save (RecordProxy obj, boolean skipTriggers, boolean skipSharing, boolean skipCreatePermissionCheck, boolean isSilentUpdate, AuthData authData, EnvData env) throws KommetException
	{
		// TODO think if the object record has to generated from the stub each time, or can it be somehow
		// stored within the stub
		
		// generate record from object stub
		Record record = RecordProxyUtil.generateRecord(obj, RecordProxyUtil.getProxyType(obj, env), 2, env);
		
		// save the record
		record = dataService.save(record, skipTriggers, skipSharing, skipCreatePermissionCheck, isSilentUpdate, authData, env);
		obj.setId(record.getKID());
		return obj;
	}

	@Override
	public RecordProxy save (RecordProxy obj, AuthData authData, EnvData env) throws KommetException
	{
		return save(obj, false, false, false, false, authData, env);
	}
	
	@Override
	public RecordProxy save (RecordProxy obj, boolean skipTriggers, boolean skipSharing, AuthData authData, EnvData env) throws KommetException
	{	
		// generate record from object stub
		Record record = RecordProxyUtil.generateRecord(obj, RecordProxyUtil.getProxyType(obj, env), 2, env);
		
		// save the record
		record = dataService.save(record, skipTriggers, skipSharing, authData, env);
		obj.setId(record.getKID());
		return obj;
	}

	
	@Override
	public RecordProxy getRecordById (KID id, PropertySelection propertySelection, RecordProxyType proxyType, AuthData authData, EnvData env) throws KommetException
	{
		return getRecordById(id, propertySelection, null, proxyType, authData, env);
	}
	
	@Override
	public long count (Type type, EnvData env) throws KommetException
	{
		Criteria c = env.getSelectCriteria(type.getKID());
		c.addProperty("count(" + Field.ID_FIELD_NAME + ")");
		
		List<Record> records = c.list();
		return (Long)((QueryResult)records.get(0)).getAggregateValue("count(" + Field.ID_FIELD_NAME + ")");
	}

	@Override
	public RecordProxy getRecordById (KID id, PropertySelection propertySelection, String properties, RecordProxyType proxyType, AuthData authData, EnvData env) throws KommetException
	{
		Criteria c = env.getSelectCriteria(env.getTypeByRecordId(id).getKID(), authData);
		c.add(Restriction.eq("id", id));
		
		if (propertySelection.equals(PropertySelection.ALL_SIMPLE_PROPERTIES))
		{
			// get all simple properties of this type
			Type obj = env.getTypeByRecordId(id);
			for (Field field : obj.getFields())
			{
				if (field.getDataType().isPrimitive())
				{
					c.addProperty(field.getApiName());
				}
				else if (field.getDataType() instanceof TypeReference)
				{
					// we can safely append the ID to the type reference field without worrying that it will cause an additional join
					// because ID fields of child relationships are queried without joining with the child relationship table
					c.addProperty(field.getApiName() + "." + Field.ID_FIELD_NAME);
				}
			}
		}
		else if (propertySelection.equals(PropertySelection.SPECIFIED) || propertySelection.equals(PropertySelection.SPECIFIED_AND_BASIC))
		{
			if (!StringUtils.hasText(properties))
			{
				throw new KommetException("When specific properties are to be retrieved, their names need to be specified in the parameter");
			}
			
			c.addProperty(properties);
			
			if (propertySelection.equals(PropertySelection.SPECIFIED_AND_BASIC))
			{
				c.addStandardSelectProperties();
			}
			
			String[] splitProperties = properties.split(",");
			for (String prop : splitProperties)
			{
				c.addAliasesForProperty(prop.trim());
			}
		}
		
		List<Record> records = c.list();
		
		if (!records.isEmpty())
		{
			// decide whether standard or custom proxies will be generated
			if (RecordProxyType.STANDARD.equals(proxyType))
			{
				return RecordProxyUtil.generateStandardTypeProxy(records.get(0), true, env, compiler);
			}
			else if (RecordProxyType.CUSTOM.equals(proxyType))
			{
				return RecordProxyUtil.generateCustomTypeProxy(records.get(0), true, env, compiler);
			}
			else
			{
				throw new KommetException("Unsupported proxy type " + proxyType);
			}
		}
		else
		{
			return null;
		}
	}
	
	@Override
	public void delete(Collection<RecordProxy> objects, AuthData authData, EnvData env) throws KommetException
	{
		delete(objects, false, authData, env);
	}

	@Override
	public void delete(Collection<RecordProxy> objects, boolean skipTriggers, AuthData authData, EnvData env) throws KommetException
	{
		// TODO - passing only IDs for delete should be enough, why parse whole objects?
		List<Record> records = new ArrayList<Record>();
		for (RecordProxy obj : objects)
		{
			records.add(RecordProxyUtil.generateRecord(obj, RecordProxyUtil.getProxyType(obj, env), 1, env));
		}
		
		dataService.deleteRecords(records, skipTriggers, authData, env);
	}
	
	@Override
	public void delete(KID id, boolean skipTriggers, AuthData authData, EnvData env) throws KommetException
	{	
		dataService.deleteRecord(id, authData, env);
	}
	
	@Override
	public void delete(KID id, AuthData authData, EnvData env) throws KommetException
	{	
		dataService.deleteRecord(id, authData, env);
	}
}