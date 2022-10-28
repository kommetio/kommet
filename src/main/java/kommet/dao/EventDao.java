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
import kommet.basic.Event;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.EventFilter;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class EventDao extends GenericDaoImpl<Event>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public EventDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<Event> get (EventFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new EventFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.EVENT_API_NAME)).getKID(), authData);
		c.addProperty("id, name, description, startDate, endDate, owner.id, owner.userName");
		c.createAlias("owner", "owner");
		
		if (filter.isInitGuests())
		{
			c.createAlias("guests", "guests");
			c.createAlias("guests.guest", "guestsUser");
			c.addProperty("guests.guest.userName, guests.response, guests.responseComment");
		}
		
		c.addStandardSelectProperties();
		
		if (filter.getEventIds() != null && !filter.getEventIds().isEmpty())
		{
			c.add(Restriction.in(Field.ID_FIELD_NAME, filter.getEventIds()));
		}
		
		if (filter.getOwnerIds() != null && !filter.getOwnerIds().isEmpty())
		{
			c.add(Restriction.in("owner.id", filter.getOwnerIds()));
		}
		
		if (StringUtils.hasText(filter.getName()))
		{
			c.add(Restriction.eq("name", filter.getName()));
		}
		
		if (StringUtils.hasText(filter.getNameLike()))
		{
			c.add(Restriction.like("name", "%" + filter.getNameLike() + "%"));
		}
		
		if (StringUtils.hasText(filter.getDescriptionLike()))
		{
			c.add(Restriction.like("description", "%" + filter.getDescriptionLike() + "%"));
		}
		
		if (filter.getStartDate() != null)
		{
			c.add(Restriction.not(Restriction.lt("endDate", filter.getStartDate())));
		}
		
		if (filter.getEndDate() != null)
		{
			c.add(Restriction.not(Restriction.gt("startDate", filter.getEndDate())));
		}
		
		if (filter.getStartDateFrom() != null)
		{
			c.add(Restriction.ge("startDate", filter.getStartDateFrom()));
		}
		
		if (filter.getStartDateTo() != null)
		{
			c.add(Restriction.le("startDate", filter.getStartDateTo()));
		}
		
		if (filter.getEndDateFrom() != null)
		{
			c.add(Restriction.ge("endDate", filter.getEndDateFrom()));
		}
		
		if (filter.getEndDateTo() != null)
		{
			c.add(Restriction.le("endDate", filter.getEndDateTo()));
		}
		
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<Event> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<Event> events = new ArrayList<Event>();
		
		for (Record r : records)
		{
			events.add(new Event(r, env));
		}
		
		return events;
	}
}