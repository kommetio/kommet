/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.env;

import java.util.HashMap;
import java.util.Map;

import kommet.basic.RecordProxy;
import kommet.dao.GlobalTypeStore;
import kommet.dao.KommetPersistenceException;
import kommet.dao.RecordProxyMapping;
import kommet.dao.TypeForProxyNotFoundException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.persistence.Entity;
import kommet.persistence.PersistenceConfig;

/**
 * Persistence configuration implementation that is environment-aware.
 * 
 * Different than the plain PersistenceConfig, it has access to type definitions on the given environment.
 * 
 * @author Radek Krawiec
 * @created 26/07/2013
 *
 */
public class EnvPersistenceConfig extends PersistenceConfig
{
	private GlobalTypeStore typeStore;
	private Map<Class<? extends RecordProxy>, Type> proxyClassToType;
	private Map<KID, Class<? extends RecordProxy>> typeIdToProxy;
	private Map<KID, String> typeIdToName;
	
	public EnvPersistenceConfig (GlobalTypeStore typeStore)
	{
		this.typeStore = typeStore;
		this.proxyClassToType = new HashMap<Class<? extends RecordProxy>, Type>();
		this.typeIdToProxy = new HashMap<KID, Class<? extends RecordProxy>>();
		this.typeIdToName = new HashMap<KID, String>();
	}
	
	@Override
	public RecordProxyMapping addMapping (Class<? extends RecordProxy> proxyClass, EnvData env) throws KommetPersistenceException
	{
		RecordProxyMapping mapping = super.addMapping(proxyClass, env);
		
		// map the proxy class to the type it proxies
		this.proxyClassToType.put(proxyClass, this.typeStore.getType(mapping.getTypeQualifiedName()));
		
		Type type = this.typeStore.getType(mapping.getTypeQualifiedName());
		if (type == null)
		{
			throw new TypeForProxyNotFoundException("Type with API name " + mapping.getTypeQualifiedName() + " not found");
		}
		
		this.typeIdToProxy.put(type.getKID(), proxyClass);
		this.typeIdToName.put(type.getKID(), proxyClass.getAnnotation(Entity.class).type());
		
		return mapping;
	}
	
	/**
	 * Deletes a mapping for the given type.
	 * @param typeId
	 * @param env
	 * @throws KommetException
	 */
	public void deleteMapping (KID typeId, EnvData env) throws KommetException
	{	
		RecordProxyMapping mapping = getMapping(typeId);
		Class<? extends RecordProxy> proxyClass = this.typeIdToProxy.get(typeId);
		this.proxyClassToType.remove(proxyClass);
		
		Type type = this.typeStore.getType(mapping.getTypeQualifiedName());
		if (type == null)
		{
			throw new KommetPersistenceException("Type with API name " + mapping.getTypeQualifiedName() + " not found");
		}
		this.typeIdToProxy.remove(type.getKID());
		super.deleteMapping(proxyClass, type.isDeclaredInCode(), env);
	}
	
	/**
	 * Returns a mapping for the given type.
	 * @param typeId
	 * @return
	 */
	public RecordProxyMapping getMapping (KID typeId)
	{
		return (RecordProxyMapping) getMapping(this.typeIdToName.get(typeId));
	}
}