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
import kommet.basic.RecordProxyType;
import kommet.basic.SharingRule;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.SharingRuleFilter;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class SharingRuleDao extends GenericDaoImpl<SharingRule>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public SharingRuleDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<SharingRule> get (SharingRuleFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new SharingRuleFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.SHARING_RULE_API_NAME)).getKID(), authData);
		c.addProperty("id, name, description, file.name, file.packageName, file.id, method, type, referencedType, isEdit, isDelete, dependentTypes, sharedWith");
		c.createAlias("file", "file");
		
		c.addStandardSelectProperties();
		
		if (filter.getRuleIds() != null && !filter.getRuleIds().isEmpty())
		{
			c.add(Restriction.in(Field.ID_FIELD_NAME, filter.getRuleIds()));
		}
		
		if (filter.getFileIds() != null && !filter.getFileIds().isEmpty())
		{
			c.add(Restriction.in("file.id", filter.getFileIds()));
		}
		
		if (filter.getReferencedTypes() != null && !filter.getReferencedTypes().isEmpty())
		{
			c.add(Restriction.in("referencedType", filter.getReferencedTypes()));
		}
		
		if (StringUtils.hasText(filter.getName()))
		{
			c.add(Restriction.eq("name", filter.getName()));
		}
		
		if (StringUtils.hasText(filter.getMethod()))
		{
			c.add(Restriction.eq("method", filter.getMethod()));
		}
	
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<SharingRule> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<SharingRule> rules = new ArrayList<SharingRule>();
		
		for (Record r : records)
		{
			rules.add(new SharingRule(r, env));
		}
		
		return rules;
	}
}