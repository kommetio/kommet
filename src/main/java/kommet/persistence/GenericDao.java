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
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.integration.PropertySelection;

/**
 * Abstract definition of basic database operations on a persistent entity.
 * 
 * @author Radek Krawiec
 * @created 26-07-2013
 * @param <T> persistent type
 */
public interface GenericDao<T extends RecordProxy>
{
	public T save (T obj, AuthData authData, EnvData env) throws KommetException;
	public T save (T obj, boolean skipTriggers, boolean skipSharing, boolean skipCreatePermissionsCheck, boolean isSilentUpdate, AuthData authData,	EnvData env) throws KommetException;
	public T get (KID id, EnvData env) throws KommetException;
	public T get (KID id, AuthData authData, EnvData env) throws KommetException;
	//public long count(EnvData env) throws KommetException;
	public T get (KID id, PropertySelection propertySelection, String properties, EnvData env) throws KommetException;
	public T get (KID id, PropertySelection propertySelection, String properties, AuthData authData, EnvData env) throws KommetException;
	public void delete(Collection<T> objects, AuthData authData, EnvData env) throws KommetException;
	public void delete(Collection<T> objects, boolean skipTriggers, AuthData authData, EnvData env) throws KommetException;
	public void delete(KID id, AuthData authData, EnvData env) throws KommetException;
	public void delete(KID id, boolean skipTriggers, AuthData authData, EnvData env) throws KommetException;
}