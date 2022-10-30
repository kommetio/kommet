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
import kommet.basic.EventGuest;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.EventGuestFilter;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class EventGuestDao extends GenericDaoImpl<EventGuest>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public EventGuestDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<EventGuest> get (EventGuestFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new EventGuestFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.EVENT_GUEST_API_NAME)).getKID(), authData);
		c.addProperty("id, guest.id, guest.userName, event.id, event.name, response, responseComment");
		c.createAlias("guest", "guest");
		c.createAlias("event", "event");
		c.addStandardSelectProperties();
		
		if (filter.getEventIds() != null && !filter.getEventIds().isEmpty())
		{
			c.add(Restriction.in("event.id", filter.getEventIds()));
		}
		
		if (filter.getGuestIds() != null && !filter.getGuestIds().isEmpty())
		{
			c.add(Restriction.in("guest.id", filter.getGuestIds()));
		}

		
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<EventGuest> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<EventGuest> eventGuests = new ArrayList<EventGuest>();
		
		for (Record r : records)
		{
			eventGuests.add(new EventGuest(r, env));
		}
		
		return eventGuests;
	}
}