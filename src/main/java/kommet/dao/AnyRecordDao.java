/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;

import kommet.auth.AuthData;
import kommet.basic.AnyRecord;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.AnyRecordFilter;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class AnyRecordDao extends GenericDaoImpl<AnyRecord>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public AnyRecordDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<AnyRecord> get (AnyRecordFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new AnyRecordFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.ANY_RECORD_API_NAME)).getKID(), authData);
		c.addProperty("id, recordId, createdDate");
		c.addStandardSelectProperties();
		
		if (filter.getRecordIds() != null && !filter.getRecordIds().isEmpty())
		{
			c.add(Restriction.in("recordId", filter.getRecordIds()));
		}
		
		c = filter.applySortAndLimit(c);
		
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<AnyRecord> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<AnyRecord> anyRecords = new ArrayList<AnyRecord>();
		
		for (Record r : records)
		{
			anyRecords.add(new AnyRecord(r, env));
		}
		
		return anyRecords;
	}
}