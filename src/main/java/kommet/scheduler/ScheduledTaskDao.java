/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.scheduler;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import kommet.basic.RecordProxyType;
import kommet.basic.ScheduledTask;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.dao.queries.SortDirection;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.QueryResultOrder;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class ScheduledTaskDao extends GenericDaoImpl<ScheduledTask>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public ScheduledTaskDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<ScheduledTask> get(ScheduledTaskFilter filter, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new ScheduledTaskFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.SCHEDULED_TASK_API_NAME)).getKID());
		c.addProperty("id, file.id, file.name, file.packageName, name, cronExpression, method");
		c.addStandardSelectProperties();
		c.createAlias("file", "file");
		
		if (StringUtils.hasText(filter.getNameLike()))
		{
			c.add(Restriction.ilike("name", "%" + filter.getNameLike() + "%"));
		}
		
		if (StringUtils.hasText(filter.getName()))
		{
			c.add(Restriction.eq("name", filter.getName()));
		}
		
		if (filter.getTasksIds() != null && !filter.getTasksIds().isEmpty())
		{
			c.add(Restriction.in("id", filter.getTasksIds()));
		}
		
		if (filter.getOrder() != null && filter.getOrderBy() != null)
		{
			c.addOrderBy(filter.getOrder().equals(QueryResultOrder.ASC) ? SortDirection.ASC : SortDirection.DESC, filter.getOrderBy());
		}
		
		List<Record> records = c.list();
		List<ScheduledTask> tasks = new ArrayList<ScheduledTask>();
		for (Record r : records)
		{
			tasks.add(new ScheduledTask(r, env));
		}
		
		return tasks;
	}
}