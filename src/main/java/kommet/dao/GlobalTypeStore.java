/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;

/**
 * A container of all types on the environment.
 * @author Radek Krawiec
 */
public class GlobalTypeStore
{
	private Map<KID, Type> typesByKID = new HashMap<KID, Type>();
	
	// map that translates type qualified names to their IDs
	private Map<String, KID> typeIdByQualifiedName = new HashMap<String, KID>();
	
	public void registerType (Type type) throws KommetException
	{
		if (type.getKID() == null)
		{
			throw new KommetException("Cannot register type " + type.getQualifiedName() + ". The KID is null.");
		}
		
		if (type.getId() == null)
		{
			throw new KommetException("Cannot register type " + type.getQualifiedName() + ". The ID is null.");
		}
		
		this.typesByKID.put(type.getKID(), type);
		this.typeIdByQualifiedName.put(type.getQualifiedName(), type.getKID());
	}
	
	public void unregisterType (Type type) throws KommetException
	{
		if (type.getKID() == null)
		{
			throw new KommetException("Cannot unregister type " + type.getQualifiedName() + ". The ID is null.");
		}
		
		this.typesByKID.remove(type.getKID());
		this.typeIdByQualifiedName.remove(type.getQualifiedName());
	}
	
	public Type getType (KID kid)
	{
		return this.typesByKID.get(kid);
	}
	
	public KID getTypeIdByQualifiedName (String qualifiedName)
	{
		return this.typeIdByQualifiedName.get(qualifiedName);
	}
	
	public Type getType (String qualifiedName)
	{
		return this.typesByKID.get(getTypeIdByQualifiedName(qualifiedName));
	}
	
	public Collection<Type> getAllNonStandardTypes()
	{
		List<Type> types = new ArrayList<Type>();
		
		for (Type type : this.typesByKID.values())
		{
			if (!type.isBasic())
			{
				types.add(type);
			}
		}
		
		return types;
	}

	public Collection<Type> getAllTypes()
	{
		return this.typesByKID.values();
	}

	public Type getType(KeyPrefix keyPrefix)
	{
		for (Type type : this.typesByKID.values())
		{
			if (type.getKeyPrefix().equals(keyPrefix))
			{
				return type;
			}
		}
		
		return null;
	}

	public void renameType(String oldQualifiedName, Type type) throws KommetException
	{
		this.typeIdByQualifiedName.remove(oldQualifiedName);
		registerType(type);
	}

	public void updateType(Type type) throws KommetException
	{
		unregisterType(type);
		registerType(type);
	}
}