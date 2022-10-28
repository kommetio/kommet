/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.Event;
import kommet.basic.EventGuest;
import kommet.basic.Profile;
import kommet.basic.UniqueCheckViolationException;
import kommet.basic.User;
import kommet.data.DataService;
import kommet.data.FieldValidationException;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.EventFilter;
import kommet.filters.EventGuestFilter;
import kommet.services.EventService;

public class EventTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	EnvService envService;
	
	@Inject
	EventService eventService;
	
	@SuppressWarnings("deprecation")
	@Test
	public void testCRUD() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		AuthData authData = dataHelper.getRootAuthData(env);
		
		Event e1 = new Event();
		e1.setStartDate(new Date(1900, 3, 12));
		e1.setEndDate(new Date(1900, 3, 13));
		e1.setName("My Event");
		
		try
		{
			e1 = eventService.save(e1, authData, env);
			fail("Saving event without an owner should fail");
		}
		catch (FieldValidationException e)
		{
			assertTrue(e.getMessage().contains("Required field Event.owner"));
		}
		
		Profile testProfile = dataHelper.getTestProfileObject("TestProfile", env);
		User testUser = dataHelper.getTestUser("test@kommet.io", "test@kommet.io", testProfile, env);
		User testUser2 = dataHelper.getTestUser("test2@kommet.io", "test2@kommet.io", testProfile, env);
		
		e1.setOwner(testUser);
		e1 = eventService.save(e1, authData, env);
		
		assertNotNull(e1.getId());
		
		Event e2 = new Event();
		e2.setStartDate(new Date(1900, 3, 11));
		e2.setEndDate(new Date(1900, 3, 13));
		e2.setName("My Event");
		e2.setOwner(testUser);
		
		e2 = eventService.save(e2, authData, env);
		assertNotNull(e2.getId());
		
		Event e3 = new Event();
		e3.setStartDate(new Date(1899, 3, 12));
		e3.setEndDate(new Date(1899, 3, 12));
		e3.setName("?_ my Event 2");
		e3.setDescription("anything");
		e3.setOwner(testUser);
		
		e3 = eventService.save(e3, authData, env);
		assertNotNull(e3.getId());
		
		EventFilter eventFilter = new EventFilter();
		eventFilter.setNameLike("2");
		assertEquals(1, eventService.get(eventFilter, authData, env).size());
		
		// find events starting after 1900, 3, 12
		eventFilter = new EventFilter();
		eventFilter.setStartDateFrom(new Date(1900, 3, 12));
		List<Event> events = eventService.get(eventFilter, authData, env);
		assertEquals(1, events.size());
		assertEquals(e1.getId(), events.get(0).getId());
		
		// try to create an event whose end date < start date
		Event e4 = new Event();
		e4.setStartDate(new Date(1900, 3, 12));
		e4.setEndDate(new Date(1900, 3, 11));
		e4.setName("Your Event");
		e4.setDescription("anything");
		e4.setOwner(testUser2);
		
		try
		{
			e4 = eventService.save(e4, authData, env);
			fail("Saving event with start date > end date should fail");
		}
		catch (FieldValidationException e)
		{
			assertTrue(e.getMessage().startsWith("Event start date must not be greater than end date"));
		}
		
		e4.setEndDate(new Date(1902, 3, 11));
		e4 = eventService.save(e4, authData, env);
		
		// find events for one of the users
		eventFilter = new EventFilter();
		eventFilter.addOwnerId(testUser2.getId());
		List<Event> userEvents = eventService.get(eventFilter, authData, env);
		assertEquals(1, userEvents.size());
		assertEquals(e4.getId(), userEvents.get(0).getId());
		assertNull(userEvents.get(0).getGuests());
		
		// add users to event
		EventGuest eg1 = new EventGuest();
		eg1.setEvent(e2);
		eg1.setGuest(testUser2);
		eventService.save(eg1, authData, env);

		// add guest to event in a different way
		eventService.addGuestToEvent(testUser2.getId(), e4.getId(), "Yes", null, authData, env);
		
		EventGuest eg3 = new EventGuest();
		eg3.setEvent(e4);
		eg3.setGuest(testUser);
		eventService.save(eg3, authData, env);
		
		// select event with guests
		Event retrievedEvent = eventService.get(e4.getId(), authData, env);
		assertNotNull(retrievedEvent.getGuests());
		assertEquals(2, retrievedEvent.getGuests().size());
		
		// find event guest records
		EventGuestFilter egFilter = new EventGuestFilter();
		egFilter.addGuestId(testUser2.getId());
		List<EventGuest> userEventGuests = eventService.get(egFilter, authData, env);
		assertEquals(2, userEventGuests.size());
		
		for (EventGuest eg : userEventGuests)
		{
			assertNotNull(eg.getGuest());
			assertNotNull(eg.getGuest().getUserName());
		}
		
		// find events for guests
		eventFilter = new EventFilter();
		eventFilter.addGuestId(testUser2.getId());
		List<Event> eventsForGuest = eventService.get(eventFilter, authData, env);
		assertEquals(2, eventsForGuest.size());
		
		// add event ID to criteria
		eventFilter.addEventId(e4.getId());
		assertEquals(1, eventService.get(eventFilter, authData, env).size());
		
		// find all events for testUser2
		List<Event> eventsForTestUser2 = eventService.getEventsForUser(testUser2.getId(), null, null, authData, env);
		assertEquals(2, eventsForTestUser2.size());
		
		// add another event created by testUser2
		Event e5 = new Event();
		e5.setStartDate(new Date(1900, 3, 12));
		e5.setEndDate(new Date(1900, 4, 13));
		e5.setName("YourEvent2");
		e5.setDescription("anything");
		e5.setOwner(testUser2);
		eventService.save(e5, authData, env);
		eventsForTestUser2 = eventService.getEventsForUser(testUser2.getId(), null, null, authData, env);
		assertEquals(3, eventsForTestUser2.size());
		
		// search in a restricted time frame
		eventsForTestUser2 = eventService.getEventsForUser(testUser2.getId(), new Date(1900, 3, 30), null, authData, env);
		assertEquals(2, eventsForTestUser2.size());
		
		eventsForTestUser2 = eventService.getEventsForUser(testUser2.getId(), new Date(1900, 3, 30), new Date(1901, 1, 1), authData, env);
		assertEquals(2, eventsForTestUser2.size());
		
		eventsForTestUser2 = eventService.getEventsForUser(testUser2.getId(), new Date(1900, 3, 30), new Date(1903, 1, 1), authData, env);
		assertEquals(2, eventsForTestUser2.size());
		
		eventsForTestUser2 = eventService.getEventsForUser(testUser2.getId(), new Date(1901, 3, 30), new Date(1903, 1, 1), authData, env);
		assertEquals(1, eventsForTestUser2.size());
		
		eventsForTestUser2 = eventService.getEventsForUser(testUser2.getId(), new Date(1901, 3, 30), new Date(1901, 1, 1), authData, env);
		assertEquals(1, eventsForTestUser2.size());
		
		// make sure you cannot create two EventGuest records for the same event/user combination
		EventGuest duplicateEventGuest = new EventGuest();
		duplicateEventGuest.setEvent(e4);
		duplicateEventGuest.setGuest(testUser);
		
		try
		{
			eventService.save(duplicateEventGuest, authData, env);
			fail("Creating duplicate event/guest combination should fail");
		}
		catch (UniqueCheckViolationException e)
		{
			assertTrue(e.getUniqueCheck().getFieldIds().contains(env.getType(KeyPrefix.get(KID.EVENT_GUEST_PREFIX)).getField("event").getKID().getId()));
			assertTrue(e.getUniqueCheck().getFieldIds().contains(env.getType(KeyPrefix.get(KID.EVENT_GUEST_PREFIX)).getField("guest").getKID().getId()));
		}
	}

}
