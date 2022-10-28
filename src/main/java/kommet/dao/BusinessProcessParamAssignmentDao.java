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
import kommet.basic.BusinessProcessParamAssignment;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.businessprocess.BusinessProcessParamAssignmentFilter;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class BusinessProcessParamAssignmentDao extends GenericDaoImpl<BusinessProcessParamAssignment>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public BusinessProcessParamAssignmentDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<BusinessProcessParamAssignment> get (BusinessProcessParamAssignmentFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new BusinessProcessParamAssignmentFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.BUSINESS_PROCESS_PARAM_ASSIGNMENT_API_NAME)).getKID(), authData);
		c.addProperty("id, businessProcess.id, sourceInvocation.id, sourceInvocation.name, sourceInvocation.invokedAction.name, targetInvocation.id, targetInvocation.name, targetInvocation.invokedAction.name, sourceParam.id, sourceParam.name, sourceParam.dataTypeName, sourceParam.dataTypeId, targetParam.id, targetParam.name, processInput.id, processInput.name, processInput.dataTypeId, processInput.dataTypeName, targetParam.dataTypeName, targetParam.dataTypeId, processOutput.id, processOutput.name, processOutput.dataTypeId, processOutput.dataTypeName");
		c.addStandardSelectProperties();
		c.createAlias("targetInvocation", "targetInvocation");
		c.createAlias("sourceInvocation", "sourceInvocation");
		c.createAlias("targetInvocation.invokedAction", "targetInvocation.invokedAction");
		c.createAlias("sourceInvocation.invokedAction", "sourceInvocation.invokedAction");
		c.createAlias("targetParam", "targetParam");
		c.createAlias("sourceParam", "sourceParam");
		c.createAlias("processInput", "processInput");
		c.createAlias("processOutput", "processOutput");
		
		if (filter.getProcessIds() != null && !filter.getProcessIds().isEmpty())
		{
			c.add(Restriction.in("businessProcess.id", filter.getProcessIds()));
		}
	
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<BusinessProcessParamAssignment> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<BusinessProcessParamAssignment> rules = new ArrayList<BusinessProcessParamAssignment>();
		
		for (Record r : records)
		{
			rules.add(new BusinessProcessParamAssignment(r, env));
		}
		
		return rules;
	}
}