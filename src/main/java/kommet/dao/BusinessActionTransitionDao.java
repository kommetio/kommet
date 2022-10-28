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
import kommet.basic.BusinessActionTransition;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.businessprocess.BusinessActionTransitionFilter;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class BusinessActionTransitionDao extends GenericDaoImpl<BusinessActionTransition>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public BusinessActionTransitionDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<BusinessActionTransition> get (BusinessActionTransitionFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new BusinessActionTransitionFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.BUSINESS_ACTION_TRANSITION_API_NAME)).getKID(), authData);
		c.addProperty("id, businessProcess.id, previousAction.id, previousAction.name, nextAction.id, nextAction.name, previousAction.name, nextAction.name");
		c.addStandardSelectProperties();
		c.createAlias("previousAction", "previousAction");
		c.createAlias("nextAction", "nextAction");
		
		if (filter.getProcessIds() != null && !filter.getProcessIds().isEmpty())
		{
			c.add(Restriction.in("businessProcess.id", filter.getProcessIds()));
		}
	
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<BusinessActionTransition> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<BusinessActionTransition> rules = new ArrayList<BusinessActionTransition>();
		
		for (Record r : records)
		{
			rules.add(new BusinessActionTransition(r, env));
		}
		
		return rules;
	}
}