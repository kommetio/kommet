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
import kommet.basic.Task;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.TaskFilter;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class TaskDao extends GenericDaoImpl<Task>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public TaskDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<Task> find (TaskFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new TaskFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.TASK_API_NAME)).getKID(), authData);
		c.addProperty("title, content, dueDate, status, priority, progress, assignedUser.id, assignedGroup.id, recordId");
		c.addStandardSelectProperties();
		
		if (filter.isFetchAssigneeData())
		{
			c.createAlias("assignedUser", "assignedUser");
			c.createAlias("assignedGroup", "assignedGroup");
			
			c.addProperty("assignedUser.userName, assignedGroup.name");
		}
		
		if (filter.getTaskIds() != null && !filter.getTaskIds().isEmpty())
		{
			c.add(Restriction.in(Field.ID_FIELD_NAME, filter.getTaskIds()));
		}
		
		if (filter.getRecordIds() != null && !filter.getRecordIds().isEmpty())
		{
			c.add(Restriction.in("recordId", filter.getRecordIds()));
		}
		
		if (StringUtils.hasText(filter.getTitleLike()))
		{
			c.add(Restriction.ilike("title", "%" + filter.getTitleLike() + "%"));
		}
		
		if (StringUtils.hasText(filter.getContentLike()))
		{
			c.add(Restriction.ilike("content", "%" + filter.getContentLike() + "%"));
		}
		
		if (filter.getDueDateFrom() != null)
		{
			c.add(Restriction.ge("dueDate", filter.getDueDateFrom()));
		}
		
		if (filter.getDueDateTo() != null)
		{
			c.add(Restriction.le("dueDate", filter.getDueDateTo()));
		}
		
		if (filter.getStatuses() != null && !filter.getStatuses().isEmpty())
		{
			c.add(Restriction.in("status", filter.getStatuses()));
		}
		
		if (filter.getPriorities() != null && !filter.getPriorities().isEmpty())
		{
			c.add(Restriction.in("priorities", filter.getPriorities()));
		}
		
		if (filter.getAssignedUserIds() != null && !filter.getAssignedUserIds().isEmpty())
		{
			c.add(Restriction.in("assignedUser.id", filter.getAssignedUserIds()));
		}
		
		if (filter.getAssignedGroupIds() != null && !filter.getAssignedGroupIds().isEmpty())
		{
			c.add(Restriction.in("assignedGroup.id", filter.getAssignedGroupIds()));
		}
		
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<Task> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<Task> tasks = new ArrayList<Task>();
		
		for (Record r : records)
		{
			tasks.add(new Task(r, env));
		}
		
		return tasks;
	}
}