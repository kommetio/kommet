/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries;

import kommet.basic.UniqueCheckViolationException;
import kommet.dao.MappedObjectQueryBuilder;
import kommet.dao.TypePersistenceMapping;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;

public class InsertQuery extends NativeDbQuery
{	
	private static final String INSERT_QUERY_SUCCESS_STATUS = "RM.STATUS.OK";
	
	public InsertQuery (TypePersistenceMapping objMapping, EnvData envData)
	{
		super(objMapping, envData);
	}
	
	public Record execute (Record record) throws KommetException
	{
		String sql = MappedObjectQueryBuilder.getInsertQuery(getTypeMapping(), record);
		
		try
		{
			String newKID = wrapInsertQueryCall(sql, INSERT_QUERY_SUCCESS_STATUS);
			record.setKID(KID.get(newKID));
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