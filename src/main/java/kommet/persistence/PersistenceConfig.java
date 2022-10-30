/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.persistence;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

import kommet.basic.RecordProxy;
import kommet.dao.PersistenceMapping;
import kommet.dao.KommetPersistenceException;
import kommet.dao.RecordProxyMapping;
import kommet.data.ExceptionErrorType;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.utils.MiscUtils;

/**
 * Represents all information about persistence storage for the given app.
 * 
 * More specifically, it contains object mappings for some set of classes. This set of classes can be all persistent
 * classes in a given app, all classes for a given environment etc.
 * 
 * Note that since this persistence config object can be used not only with Kommet storage, it does not contain any information
 * about the object's type or similar. It does, however, contain information about the object's API name, which is read
 * from the object proxy's @Entity annotation.
 * 
 * @author Radek Krawiec
 * @created 26-07-2013
 *
 */
public class PersistenceConfig
{
	private Map<String, PersistenceMapping> mappingsByApiName;
	
	public PersistenceConfig()
	{
		this.mappingsByApiName = new HashMap<String, PersistenceMapping>();
	}
	
	public RecordProxyMapping addMapping (Class<? extends RecordProxy> proxyClass, EnvData env) throws KommetPersistenceException
	{
		RecordProxyMapping mapping = new RecordProxyMapping(proxyClass);
		if (!StringUtils.hasText(mapping.getTypeQualifiedName()))
		{
			throw new KommetPersistenceException("Property object name not set on object mapping for object " + proxyClass.getName());
		}
		this.mappingsByApiName.put(mapping.getTypeQualifiedName(), mapping);
		
		if (mapping.getProxyClass() == null)
		{
			throw new KommetPersistenceException("Property proxy class not set on object mapping for object " + proxyClass.getName());
		}
		
		return mapping;
	}
	
	public void deleteMapping (Class<? extends RecordProxy> proxyClass, boolean isDeclaredInCode, EnvData env) throws KommetException
	{
		// if class was declared in code, the @Entity annotation was removed from it when the @Type annotation
		// was removed, so we cannot call constructor new RecordProxyMapping(), because it looks for @Entity annotation.
		// Nor do we need to call it, because it's only there to verify the correctness of the @Entity annotation
		if (!isDeclaredInCode)
		{
			RecordProxyMapping mapping = new RecordProxyMapping(proxyClass);
			if (!StringUtils.hasText(mapping.getTypeQualifiedName()))
			{
				throw new KommetPersistenceException("Property object name not set on object mapping for object " + proxyClass.getName());
			}
		}
		
		String typeName = MiscUtils.isEnvSpecific(proxyClass.getName()) ? MiscUtils.envToUserPackage(proxyClass.getName(), env) : proxyClass.getName();
		
		if (this.mappingsByApiName.remove(typeName) == null)
		{
			throw new KommetException("Could not delete proxy mapping by name " + typeName + ". Mapping not found", ExceptionErrorType.PROXY_MAPPING_NOT_FOUND);
		}
	}
	
	public PersistenceMapping getMapping (String apiName)
	{
		return this.mappingsByApiName.get(apiName);
	}
}