/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.persistence;

import java.util.Collection;

import kommet.auth.AuthData;
import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyType;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.PropertySelection;

/**
 * Abstract implementation of the basic operations on persistent types.
 * @author Radek Krawiec
 * @created 26-07-2013
 *
 * @param <T> persistent type
 */
public abstract class GenericDaoImpl<T extends RecordProxy> implements GenericDao<T>
{	
	private RecordProxyType proxyType;
	
	public GenericDaoImpl (RecordProxyType proxyType)
	{
		this.proxyType = proxyType;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public T save(T obj, boolean skipTriggers, boolean skipSharing, boolean skipCreatePermissionCheck, boolean isSilentUpdate, AuthData authData, EnvData env) throws KommetException
	{
		return (T)getEnvCommunication().save(obj, skipTriggers, skipSharing, skipCreatePermissionCheck, isSilentUpdate, authData, env);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public T save(T obj, AuthData authData, EnvData env) throws KommetException
	{
		return (T)getEnvCommunication().save(obj, authData, env);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void delete (Collection<T> objects, AuthData authData, EnvData env) throws KommetException
	{
		getEnvCommunication().delete((Collection<RecordProxy>)objects, true, authData, env);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void delete (Collection<T> objects, boolean skipTriggers, AuthData authData, EnvData env) throws KommetException
	{
		getEnvCommunication().delete((Collection<RecordProxy>)objects, skipTriggers, authData, env);
	}
	
	@Override
	public void delete (KID id, AuthData authData, EnvData env) throws KommetException
	{
		getEnvCommunication().delete(id, authData, env);
	}
	
	@Override
	public void delete (KID id, boolean skipTriggers, AuthData authData, EnvData env) throws KommetException
	{
		getEnvCommunication().delete(id, skipTriggers, authData, env);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public T get(KID id, AuthData authData, EnvData env) throws KommetException
	{
		return (T)getEnvCommunication().getRecordById(id, PropertySelection.ALL_SIMPLE_PROPERTIES, proxyType, authData, env);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get(KID id, EnvData env) throws KommetException
	{
		return (T)getEnvCommunication().getRecordById(id, PropertySelection.ALL_SIMPLE_PROPERTIES, proxyType, null, env);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public T get(KID id, PropertySelection propertySelection, String properties, AuthData authData, EnvData env) throws KommetException
	{
		return (T)getEnvCommunication().getRecordById(id, propertySelection, properties, proxyType, authData, env);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public T get(KID id, PropertySelection propertySelection, String properties, EnvData env) throws KommetException
	{
		return (T)getEnvCommunication().getRecordById(id, propertySelection, properties, proxyType, null, env);
	}

	/**
	 * Interface for communicating with Kommet environments
	 * @return
	 */
	protected abstract EnvPersistenceInterface getEnvCommunication();
}