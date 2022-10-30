/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.triggers;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;

import kommet.basic.RecordProxyType;
import kommet.basic.TypeTrigger;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.dao.queries.SortDirection;
import kommet.data.DataService;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.QueryResultOrder;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class TypeTriggerDao extends GenericDaoImpl<TypeTrigger>
{
	@Inject
	DataService dataService;
	
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public TypeTriggerDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<TypeTrigger> find (TypeTriggerFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new TypeTriggerFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.TYPE_TRIGGER_API_NAME)).getKID());
		// do not retrieve the code field in the query because it's too large
		c.addProperty("typeId, triggerFile.id, triggerFile.name, triggerFile.packageName, isActive, isSystem, isBeforeInsert, isBeforeUpdate, isBeforeDelete, isAfterInsert, isAfterUpdate, isAfterDelete");
		
		if (filter.isInitClassCode())
		{
			c.addProperty("triggerFile.kollCode");
		}
		
		c.createAlias("triggerFile", "triggerFile");
		c.addStandardSelectProperties();
		
		if (filter.getTriggerFileIds() != null && !filter.getTriggerFileIds().isEmpty())
		{
			c.add(Restriction.in("triggerFile.id", filter.getTriggerFileIds()));
		}
		
		if (filter.getTypeTriggerIds() != null && !filter.getTypeTriggerIds().isEmpty())
		{
			c.add(Restriction.in("id", filter.getTypeTriggerIds()));
		}
		
		if (filter.getTypeIds() != null && !filter.getTypeIds().isEmpty())
		{
			c.add(Restriction.in("typeId", filter.getTypeIds()));
		}
		
		if (filter.getOrder() != null && filter.getOrderBy() != null)
		{
			c.addOrderBy(filter.getOrder().equals(QueryResultOrder.ASC) ? SortDirection.ASC : SortDirection.DESC, filter.getOrderBy());
		}
		
		return getObjectStubList(c.list(), env);
	}
	
	private static List<TypeTrigger> getObjectStubList(List<Record> records, EnvData env) throws KommetException
	{
		List<TypeTrigger> typeTriggers = new ArrayList<TypeTrigger>();
		
		for (Record r : records)
		{
			typeTriggers.add(new TypeTrigger(r, env));
		}
		
		return typeTriggers;
	}
}