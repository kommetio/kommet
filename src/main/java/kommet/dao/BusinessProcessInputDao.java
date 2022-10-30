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
import kommet.basic.BusinessProcessInput;
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
public class BusinessProcessInputDao extends GenericDaoImpl<BusinessProcessInput>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public BusinessProcessInputDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<BusinessProcessInput> get (BusinessProcessParamFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new BusinessProcessParamFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.BUSINESS_PROCESS_INPUT_API_NAME)).getKID(), authData);
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
	
	private static List<BusinessProcessInput> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<BusinessProcessInput> rules = new ArrayList<BusinessProcessInput>();
		
		for (Record r : records)
		{
			rules.add(new BusinessProcessInput(r, env));
		}
		
		return rules;
	}
}