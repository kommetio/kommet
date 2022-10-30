/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.basic.TypeInfo;
import kommet.env.EnvData;

@Service
public class TypeInfoService
{
	@Inject
	TypeInfoDao typeInfoDao;
	
	/**
	 * Type info is an object that, once created, never changes, because it contains
	 * information about type's standard pages which are immutable.
	 * 
	 * Furthermore, it is very frequent referenced in the request filter when action/page
	 * to be displayed is determined.
	 * 
	 * For these reasons we want to have it cached.
	 */
	private Map<KID, TypeInfo> cachedTypeInfo = new HashMap<KID, TypeInfo>();
	
	@Transactional(readOnly = true)
	public List<TypeInfo> find (TypeInfoFilter filter, EnvData env) throws KommetException
	{
		return typeInfoDao.find(filter, env);
	}
	
	// Since type info is immutable, we don't need a method to save it. It will only be saved once
	// when it is created, using the DAO method directly
	// In case it is changed to mutable, we need to remember to update its state in the cache
	// each time it is updated.
	/*
	@Transactional
	public TypeInfo save (TypeInfo info, AuthData authData, EnvData env) throws KommetException
	{
		// TODO update it in the cache map cachedTypeInfo
		return typeInfoDao.save(info, authData, env);
	}*/
	
	public TypeInfo getForType(KID typeId, EnvData env) throws KommetException
	{
		return getForType(typeId, true, env);
	}

	public TypeInfo getForType(KID typeId, boolean isUseCache, EnvData env) throws KommetException
	{
		if (isUseCache && cachedTypeInfo.containsKey(typeId))
		{
			return cachedTypeInfo.get(typeId);
		}
		
		TypeInfoFilter filter = new TypeInfoFilter();
		filter.addTypeId(typeId);
		List<TypeInfo> infos = typeInfoDao.find(filter, env);
		if (infos.isEmpty())
		{
			return null;
		}
		else if (infos.size() == 1)
		{
			// cache the result
			this.cachedTypeInfo.put(typeId, infos.get(0));
			return infos.get(0);
		}
		else
		{
			throw new KommetException("More than one type info object found for type with ID " + typeId);
		}
	}
}