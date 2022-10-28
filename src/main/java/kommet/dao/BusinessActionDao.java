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
import kommet.basic.BusinessAction;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.businessprocess.BusinessActionFilter;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class BusinessActionDao extends GenericDaoImpl<BusinessAction>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public BusinessActionDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<BusinessAction> get (BusinessActionFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new BusinessActionFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.BUSINESS_ACTION_API_NAME)).getKID(), authData);
		c.addProperty("id, name, description, file.name, file.packageName, file.id, type, isEntryPoint, inputs.id, inputs.name, inputs.dataTypeId, inputs.dataTypeName, outputs.id, outputs.name, outputs.dataTypeId, outputs.dataTypeName");
		c.createAlias("file", "file");
		c.createAlias("outputs", "outputs");
		c.createAlias("inputs", "inputs");
		
		c.addStandardSelectProperties();

		
		if (filter.getFileIds() != null && !filter.getFileIds().isEmpty())
		{
			c.add(Restriction.in("file.id", filter.getFileIds()));
		}
		
		if (filter.getActionIds() != null && !filter.getActionIds().isEmpty())
		{
			c.add(Restriction.in("id", filter.getActionIds()));
		}
		
		if (StringUtils.hasText(filter.getName()))
		{
			c.add(Restriction.eq("name", filter.getName()));
		}
		
		if (StringUtils.hasText(filter.getType()))
		{
			c.add(Restriction.eq("type", filter.getType()));
		}
	
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<BusinessAction> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<BusinessAction> rules = new ArrayList<BusinessAction>();
		
		for (Record r : records)
		{
			rules.add(new BusinessAction(r, env));
		}
		
		return rules;
	}
}