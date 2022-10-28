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
import kommet.basic.BusinessActionInvocation;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.businessprocess.BusinessActionInvocationFilter;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class BusinessActionInvocationDao extends GenericDaoImpl<BusinessActionInvocation>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public BusinessActionInvocationDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<BusinessActionInvocation> get (BusinessActionInvocationFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new BusinessActionInvocationFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.BUSINESS_ACTION_INVOCATION_API_NAME)).getKID(), authData);
		c.addProperty("id, parentProcess.id, invokedAction.id, invokedAction.isEntryPoint, invokedAction.name, invokedAction.type, invokedAction.file.id, invokedAction.file.name, invokedAction.file.packageName, attributes.id, attributes.name, attributes.value, name");
		c.addProperty("invokedProcess.id, invokedProcess.name");
		c.createAlias("invokedAction", "invokedAction");
		c.createAlias("invokedProcess", "invokedProcess");
		c.createAlias("attributes", "attributes");
		c.createAlias("invokedAction.file", "invokedAction.file");
		c.addStandardSelectProperties();

		
		if (filter.getInvokedActionIds() != null && !filter.getInvokedActionIds().isEmpty())
		{
			c.add(Restriction.in("invokedAction.id", filter.getInvokedActionIds()));
		}
		
		if (filter.getInvokedProcessIds() != null && !filter.getInvokedProcessIds().isEmpty())
		{
			c.add(Restriction.in("invokedProcess.id", filter.getInvokedProcessIds()));
		}
		
		if (filter.getParentProcessIds() != null && !filter.getParentProcessIds().isEmpty())
		{
			c.add(Restriction.in("parentProcess.id", filter.getParentProcessIds()));
		}
	
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<BusinessActionInvocation> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<BusinessActionInvocation> invocations = new ArrayList<BusinessActionInvocation>();
		
		for (Record r : records)
		{
			invocations.add(new BusinessActionInvocation(r, env));
		}
		
		return invocations;
	}
}