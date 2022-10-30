/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries;

import java.util.Collection;
import java.util.List;

import kommet.auth.AuthData;
import kommet.basic.RecordProxy;
import kommet.dao.MappedObjectQueryBuilder;
import kommet.dao.TypePersistenceMapping;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.utils.MiscUtils;

public class DeleteQuery extends NativeDbQuery
{
	private static final String DELETE_QUERY_SUCCESS_STATUS = "RM.STATUS.OK";
	
	public DeleteQuery (TypePersistenceMapping objMapping, EnvData envData)
	{
		super(objMapping, envData);
	}
	
	public Record execute (Record record, AuthData authData) throws KommetException
	{
		String sql = MappedObjectQueryBuilder.getDeleteQuery(getTypeMapping(), record.getKID(), authData, getEnv(), false);
		
		wrapUpdateDeleteQueryCall(sql, DELETE_QUERY_SUCCESS_STATUS);
		return null;
	}
	
	public Record execute (KID recordId, AuthData authData) throws KommetException
	{
		String sql = MappedObjectQueryBuilder.getDeleteQuery(getTypeMapping(), recordId, authData, getEnv(), false);
		
		wrapUpdateDeleteQueryCall(sql, DELETE_QUERY_SUCCESS_STATUS);
		return null;
	}
	
	public Record execute (Collection<Record> records, AuthData authData, EnvData env) throws KommetException
	{
		String sql = MappedObjectQueryBuilder.getBulkDeleteQuery(getTypeMapping(), records, authData, env, false);
		wrapUpdateDeleteQueryCall(sql, DELETE_QUERY_SUCCESS_STATUS);
		return null;
	}

	public <T extends RecordProxy> void execute(List<T> objects, AuthData authData, EnvData env) throws KommetException
	{
		List<KID> ids = MiscUtils.getKIDListForProxies(objects);
		String sql = MappedObjectQueryBuilder.getBulkDeleteQueryByIds(getTypeMapping(), ids, authData, env, false);
		
		wrapUpdateDeleteQueryCall(sql, DELETE_QUERY_SUCCESS_STATUS);
	}
}