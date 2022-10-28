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
import kommet.basic.BusinessProcessOutput;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.businessprocess.BusinessProcessParamFilter;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class BusinessProcessOutputDao extends GenericDaoImpl<BusinessProcessOutput>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public BusinessProcessOutputDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<BusinessProcessOutput> get (BusinessProcessParamFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new BusinessProcessParamFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.BUSINESS_PROCESS_OUTPUT_API_NAME)).getKID(), authData);
		c.addProperty("id, name, dataTypeId, dataTypeName, businessAction.id, businessProcess.id");
		
		c.addStandardSelectProperties();

		
		if (filter.getActionIds() != null && !filter.getActionIds().isEmpty())
		{
			c.add(Restriction.in("businessAction.id", filter.getActionIds()));
		}
		
		if (filter.getProcessIds() != null && !filter.getProcessIds().isEmpty())
		{
			c.add(Restriction.in("businessProcess.id", filter.getProcessIds()));
		}
	
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<BusinessProcessOutput> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<BusinessProcessOutput> rules = new ArrayList<BusinessProcessOutput>();
		
		for (Record r : records)
		{
			rules.add(new BusinessProcessOutput(r, env));
		}
		
		return rules;
	}
}