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
import kommet.basic.Reminder;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.ReminderFilter;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class ReminderDao extends GenericDaoImpl<Reminder>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public ReminderDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<Reminder> find (ReminderFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new ReminderFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.REMINDER_API_NAME)).getKID(), authData);
		c.addProperty("title, content, recordId, referencedField, media, intervalUnit, intervalValue, assignedUser.id, assignedGroup.id, status");
		c.addStandardSelectProperties();
		
		c.createAlias("assignedUser", "assignedUser");
		c.createAlias("assignedGroup", "assignedGroup");
		
		if (filter.getReminderIds() != null && !filter.getReminderIds().isEmpty())
		{
			c.add(Restriction.in(Field.ID_FIELD_NAME, filter.getReminderIds()));
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
	
	private static List<Reminder> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<Reminder> reminders = new ArrayList<Reminder>();
		
		for (Record r : records)
		{
			reminders.add(new Reminder(r, env));
		}
		
		return reminders;
	}
}