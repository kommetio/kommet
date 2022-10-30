/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import kommet.dao.queries.InsertQuery;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;

@Service
public class TypePersistenceConfig
{
	private Map<KID, TypePersistenceMapping> mappingsByKID;
	private Map<String, TypePersistenceMapping> mappingsByApiName;
	
	public TypePersistenceConfig() throws KommetException
	{
		this.mappingsByKID = new HashMap<KID, TypePersistenceMapping>();
		this.mappingsByApiName = new HashMap<String, TypePersistenceMapping>();
		
		// TODO - make it automatic
		//initMappings();
	}

	public void addMapping(KID rid, String apiName, TypePersistenceMapping mapping)
	{
		this.mappingsByKID.put(rid, mapping);
		this.mappingsByApiName.put(apiName, mapping);
	}
	
	public void removeMapping(KID rid, String apiName)
	{
		this.mappingsByKID.remove(rid);
		this.mappingsByApiName.remove(apiName);
	}

	public InsertQuery createInsertQuery (KID rid, EnvData envData) throws KommetException
	{
		TypePersistenceMapping mapping = this.mappingsByKID.get(rid);
		
		if (mapping == null)
		{
			throw new KommetPersistenceException("No persistence mapping found for type with ID = " + rid);
		}
		
		return new InsertQuery(mapping, envData);
	}

	public TypePersistenceMapping getMapping (KID rid)
	{
		return this.mappingsByKID.get(rid);
	}
	
	public TypePersistenceMapping getMappingByApiName (String apiName)
	{
		return this.mappingsByApiName.get(apiName);
	}

	public void renameType(String oldQualifiedName, Type type, EnvData env) throws KommetException
	{
		TypePersistenceMapping mapping = this.mappingsByApiName.get(oldQualifiedName);
		if (mapping == null)
		{
			throw new KommetException("Cannot update type mapping: no mapping exists for API name " + oldQualifiedName);
		}
		this.mappingsByApiName.remove(oldQualifiedName);
		addMapping(type.getKID(), type.getQualifiedName(), TypePersistenceMapping.get(type, env));
	}

	public void updateType(Type type, EnvData env) throws KommetException
	{
		TypePersistenceMapping mapping = this.mappingsByApiName.get(type.getQualifiedName());
		if (mapping == null)
		{
			throw new KommetException("Cannot update type mapping: no mapping exists for API name " + type.getQualifiedName());
		}
		this.mappingsByApiName.remove(type.getQualifiedName());
		addMapping(type.getKID(), type.getQualifiedName(), TypePersistenceMapping.get(type, env));
	}
}