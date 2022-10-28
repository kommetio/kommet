/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries;

import kommet.auth.AuthData;
import kommet.basic.UniqueCheckViolationException;
import kommet.dao.MappedObjectQueryBuilder;
import kommet.dao.TypePersistenceMapping;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;

public class UpdateQuery extends NativeDbQuery
{
	//private static final Logger log = LoggerFactory.getLogger(UpdateQuery.class);
	
	// status returned by the database procedure execute_update()
	private static final String UPDATE_QUERY_SUCCESS_STATUS = "RM.STATUS.OK";
	
	public UpdateQuery(TypePersistenceMapping typeMapping, EnvData env)
	{
		super(typeMapping, env);
	}
	
	public Record execute (Record record, Criteria criteria, AuthData authData) throws KommetException
	{
		return execute(record, criteria, authData, false);
	}
	
	public Record execute (Record record, Criteria criteria, AuthData authData, boolean forceAllowEdit) throws KommetException
	{
		if (criteria.isUseMainTableAlias())
		{
			throw new KommetException("Update criteria cannot use main table alias");
		}
		String sql = MappedObjectQueryBuilder.getUpdateQuery(getTypeMapping(), record, criteria, authData, forceAllowEdit);
		
		try
		{
			wrapUpdateDeleteQueryCall(sql, UPDATE_QUERY_SUCCESS_STATUS);
			return record;
		}
		catch (UniqueCheckViolationException e)
		{
			e.setRecord(record);
			throw e;
		}
		catch (org.springframework.dao.DuplicateKeyException e)
		{
			// we can run on different language versions of postgres, so we cannot depend on the literal
			// error messages - this is why we only check if they contain the "unique_check_" literal which
			// is a standard prefix for unique check constraints
			if (e.getMessage().contains("\"unique_check_"))
			{
				UniqueCheckViolationException e1 = new UniqueCheckViolationException("Unique check violated", null);
				e1.setRecord(record);
				throw e1;
			}
			else
			{
				throw e;
			}
		}
	}
}