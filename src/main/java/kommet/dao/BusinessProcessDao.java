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
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.BusinessProcess;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.businessprocess.BusinessProcessFilter;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class BusinessProcessDao extends GenericDaoImpl<BusinessProcess>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public BusinessProcessDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<BusinessProcess> get (BusinessProcessFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new BusinessProcessFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.BUSINESS_PROCESS_API_NAME)).getKID(), authData);
		
		// original longer query
		//c.addProperty("id, name, label, description, invocationOrder, isCallable, isTriggerable, inputs.id, inputs.name, outputs.id, outputs.name, isDraft, isActive, compiledClass.id, paramAssignments.sourceInvocation.id, paramAssignments.targetInvocation.id, paramAssignments.sourceParam.id, paramAssignments.targetParam.id, paramAssignments.processInput.id, paramAssignments.processOutput.id, invocations.id, invocations.name, invocations.businessAction.id, transitions.id, transitions.previousAction.id, transitions.nextAction.id");
		
		c.addProperty("id, name, label, displaySettings, description, invocationOrder, isCallable, isTriggerable, inputs.id, inputs.name, inputs.dataTypeId, inputs.dataTypeName, outputs.id, outputs.name, outputs.dataTypeId, outputs.dataTypeName, isDraft, isActive, compiledClass.id");
		//c.createAlias("transitions", "transitions");
		c.createAlias("compiledClass", "compiledClass");
		c.createAlias("inputs", "inputs");
		c.createAlias("outputs", "outputs");

		c.addStandardSelectProperties();

		
		if (filter.getProcessIds() != null && !filter.getProcessIds().isEmpty())
		{
			c.add(Restriction.in("id", filter.getProcessIds()));
		}
		
		if (filter.getIsCallable() != null)
		{
			c.add(Restriction.eq("isCallable", filter.getIsCallable()));
		}
		
		if (StringUtils.hasText(filter.getName()))
		{
			c.add(Restriction.eq("name", filter.getName()));
		}
	
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<BusinessProcess> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<BusinessProcess> rules = new ArrayList<BusinessProcess>();
		
		for (Record r : records)
		{
			rules.add(new BusinessProcess(r, env));
		}
		
		return rules;
	}
}