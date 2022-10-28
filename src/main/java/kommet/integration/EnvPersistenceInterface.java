/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.integration;

import java.util.Collection;

import kommet.auth.AuthData;
import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyType;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;

/**
 * An interface that abstract object persistence operations for a Kommet environment.
 * 
 * All implementations of the interface must implement methods that allow for creating and manipulating types
 * as well as creating, saving and updating specific records of types.
 * 
 * @author Radek Krawiec
 * @created 26-07-2013
 */
public interface EnvPersistenceInterface
{
	public Type create (Type obj, AuthData authData, EnvData env) throws KommetException;
	public RecordProxy save (RecordProxy obj, boolean skipTriggers, boolean skipSharing, boolean skipCreatePermissionCheck, boolean isSilentUpdate, AuthData authData, EnvData env) throws KommetException;
	public RecordProxy save (RecordProxy record, AuthData authData, EnvData env) throws KommetException;
	public RecordProxy save (RecordProxy record, boolean skipTriggers, boolean skipSharing, AuthData authData, EnvData env) throws KommetException;
	public RecordProxy getRecordById(KID id, PropertySelection propertySelection, RecordProxyType proxyType, AuthData authData, EnvData env) throws KommetException;
	public RecordProxy getRecordById(KID id, PropertySelection propertySelection, String properties, RecordProxyType proxyType, AuthData authData, EnvData env) throws KommetException;
	public void delete (Collection<RecordProxy> objects, AuthData authData, EnvData env) throws KommetException;
	public void delete (KID id, AuthData authData, EnvData env) throws KommetException;
	public void delete (Collection<RecordProxy> objects, boolean skipTriggers, AuthData authData, EnvData env) throws KommetException;
	public void delete (KID id, boolean skipTriggers, AuthData authData, EnvData env) throws KommetException;
	public long count (Type type, EnvData env) throws KommetException;
}