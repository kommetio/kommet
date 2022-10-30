/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.basic.Event;
import kommet.basic.EventGuest;
import kommet.basic.User;
import kommet.dao.EventDao;
import kommet.dao.EventGuestDao;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.filters.EventFilter;
import kommet.filters.EventGuestFilter;

@Service
public class EventService
{
	@Inject
	EventDao dao;
	
	@Inject
	EventGuestDao eventGuestDao;
	
	@Transactional
	public Event save (Event e, AuthData authData, EnvData env) throws KommetException
	{
		return dao.save(e, authData, env);
	}
	
	@Transactional
	public EventGuest save (EventGuest eg, AuthData authData, EnvData env) throws KommetException
	{
		return eventGuestDao.save(eg, authData, env);
	}
	
	/**
	 * Find all events for a given user. Returns events of which this user is an owner, or to which they are invited.
	 * @param userId
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional(readOnly = true)
	public List<Event> getEventsForUser (KID userId, Date startDate, Date endDate, AuthData authData, EnvData env) throws KommetException
	{
		// first find events for which this user is an owner
		EventFilter filter = new EventFilter();
		filter.addOwnerId(userId);
		filter.setStartDate(startDate);
		filter.setEndDate(endDate);
		List<Event> ownerEvents = get(filter, authData, env);
		
		// now find events to which this user is invited (regardless of whether they accepted or not)
		filter = new EventFilter();
		filter.addGuestId(userId);
		filter.setStartDate(startDate);
		filter.setEndDate(endDate);
		List<Event> guestEvents = get(filter, authData, env);
		
		Set<KID> allEventIds = new HashSet<KID>();
		List<Event> allEvents = new ArrayList<Event>();
		for (Event e : ownerEvents)
		{
			allEvents.add(e);
			allEventIds.add(e.getId());
		}
		
		for (Event e : guestEvents)
		{
			if (!allEventIds.contains(e.getId()))
			{
				allEvents.add(e);
				allEventIds.add(e.getId());
			}
		}
		
		return allEvents;
	}
	
	@Transactional(readOnly = true)
	public List<Event> get (EventFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new EventFilter();
		}
		
		// remember original event IDs so that the method does not modify it
		Set<KID> originalEventIds = null;
		if (filter.getEventIds() != null && !filter.getEventIds().isEmpty())
		{
			originalEventIds = new HashSet<KID>();
			for (KID eventId : filter.getEventIds())
			{
				originalEventIds.add(eventId);
			}
		}
		
		if (filter.getGuestIds() != null && !filter.getGuestIds().isEmpty())
		{
			// find all event assignments for these guests
			EventGuestFilter egFilter = new EventGuestFilter();
			egFilter.setGuestIds(filter.getGuestIds());
			egFilter.setStartDate(filter.getStartDate());
			egFilter.setEndDate(filter.getEndDate());
			
			List<EventGuest> invitedGuests = eventGuestDao.get(egFilter, authData, env);
			if (invitedGuests.isEmpty())
			{
				// no event guests found, so no events will fulfill the criteria
				return new ArrayList<Event>();
			}
			else
			{
				Set<KID> eventIds = new HashSet<KID>();
				for (EventGuest eg : invitedGuests)
				{
					eventIds.add(eg.getEvent().getId());
				}
				
				if (filter.getEventIds() == null || filter.getEventIds().isEmpty())
				{
					filter.setEventIds(eventIds);
				}
				else
				{
					Set<KID> commonEventIds = new HashSet<KID>();
					
					// some event IDs have already been defined for the search, so we keep only those that are among the invited guest event IDs
					for (KID eventId : filter.getEventIds())
					{
						// if event ID is in both collections
						if (eventIds.contains(eventId))
						{
							commonEventIds.add(eventId);
						}
					}
					
					// search only by common event IDs
					filter.setEventIds(commonEventIds);
				}
			}
		}
		
		List<Event> events = dao.get(filter, authData, env);
		filter.setEventIds(originalEventIds);
		return events;
	}
	
	@Transactional(readOnly = true)
	public List<EventGuest> get (EventGuestFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		return eventGuestDao.get(filter, authData, env);
	}
	
	@Transactional(readOnly = true)
	public Event get (KID id, AuthData authData, EnvData env) throws KommetException
	{
		EventFilter filter = new EventFilter();
		filter.addEventId(id);
		filter.setInitGuests(true);
		List<Event> events = dao.get(filter, authData, env);
		return events.isEmpty() ? null : events.get(0);
	}
	
	@Transactional
	public void delete (KID eventId, AuthData authData, EnvData env) throws KommetException
	{
		dao.delete(eventId, authData, env);
	}

	@Transactional
	public void addGuestToEvent(KID guestId, KID eventId, String response, String responseComment, AuthData authData, EnvData env) throws KommetException
	{
		EventGuest eg = new EventGuest();
		
		User guest = new User();
		guest.setId(guestId);
		eg.setGuest(guest);
		
		Event event = new Event();
		event.setId(eventId);
		eg.setEvent(event);
		
		eg.setResponse(response);
		eg.setResponseComment(responseComment);
		
		save(eg, authData, env);
	}

	@Transactional
	public void deleteEventGuests(List<EventGuest> egs, AuthData authData, EnvData env) throws KommetException
	{
		eventGuestDao.delete(egs, authData, env);
	}
}