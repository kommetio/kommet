/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.Event;
import kommet.basic.EventGuest;
import kommet.basic.User;
import kommet.basic.keetle.tags.breadcrumbs.Breadcrumbs;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.UniqueCheckService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.EventGuestFilter;
import kommet.koll.annotations.ResponseBody;
import kommet.labels.ManipulatingReferencedLabelException;
import kommet.rest.RestUtil;
import kommet.services.EventService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

@Controller
public class EventController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	UniqueCheckService ucService;
	
	@Inject
	EventService eventService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/events/list", method = RequestMethod.GET)
	public ModelAndView list(HttpSession session, HttpServletRequest req) throws KommetException
	{
		// add breadcrumbs
		Breadcrumbs.add(req.getRequestURL().toString(), "Events", appConfig.getBreadcrumbMax(), session, getContextPath(session));
		return new ModelAndView("events/list");
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/calendar", method = RequestMethod.GET)
	public ModelAndView calendar(HttpSession session, HttpServletRequest req) throws KommetException
	{
		// add breadcrumbs
		Breadcrumbs.add(req.getRequestURL().toString(), "Calendar", appConfig.getBreadcrumbMax(), session, getContextPath(session));
		return new ModelAndView("events/calendar");
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/events/deleteguest", method = RequestMethod.POST)
	@ResponseBody
	public void deleteGuest(@RequestParam(value = "guestId", required = false) String sGuestId, @RequestParam(value = "eventId", required = false) String sEventId, HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		resp.setContentType("application/json; charset=UTF-8");
		PrintWriter out = resp.getWriter();
		
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		
		// find guest assignment
		EventGuestFilter filter = new EventGuestFilter();
		filter.addEventId(KID.get(sEventId));
		filter.addGuestId(KID.get(sGuestId));
		
		List<EventGuest> egs = eventService.get(filter, authData, env);
		
		if (egs.size() > 1)
		{
			// this should never happen due to unique checks on the EventGuest type
			out.write(RestUtil.getRestErrorResponse("More than one event-guest assignment found"));
			return;
		}
		else if (egs.isEmpty())
		{
			out.write(RestUtil.getRestSuccessResponse("No guest to delete"));
			return;
		}
		
		try
		{
			eventService.deleteEventGuests(egs, authData, env);
			out.write(RestUtil.getRestSuccessResponse("Guest deleted"));
		}
		catch (Exception e)
		{
			out.write(RestUtil.getRestErrorResponse(e.getMessage()));
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/events/addguest", method = RequestMethod.POST)
	@ResponseBody
	public void addGuest(@RequestParam(value = "guestId", required = false) String sGuestId, @RequestParam(value = "eventId", required = false) String sEventId, HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		resp.setContentType("application/json; charset=UTF-8");
		PrintWriter out = resp.getWriter();
		
		try
		{
			eventService.addGuestToEvent(KID.get(sGuestId), KID.get(sEventId), null, null, AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
			out.write(RestUtil.getRestSuccessResponse("Guest added"));
		}
		catch (Exception e)
		{
			out.write(RestUtil.getRestErrorResponse(e.getMessage()));
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/rest/events", method = RequestMethod.GET)
	@ResponseBody
	public void eventsForCalendar(@RequestParam(value = "userId", required = false) String sUserId, @RequestParam(value = "startDate", required = false) Long startDate, @RequestParam(value = "endDate", required = false) Long endDate, HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		// find all events within the given time frame
		List<Event> events = eventService.getEventsForUser(KID.get(sUserId), new Date(startDate), new Date(endDate), AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
		List<String> serializedEvents = new ArrayList<String>();
		
		for (Event event : events)
		{
			serializedEvents.add(serializeEvent(event));
		}
		
		resp.setContentType("application/json; charset=UTF-8");
		PrintWriter out = resp.getWriter();
		out.write(RestUtil.getRestSuccessDataResponse("{ \"events\": [ " + MiscUtils.implode(serializedEvents, ", ") + " ] }"));
	}
	
	private static String serializeEvent(Event event)
	{
		List<String> props = new ArrayList<String>();
		props.add("\"id\": \"" + event.getId() + "\"");
		props.add("\"startDate\": " + event.getStartDate().getTime());
		props.add("\"name\": \"" + event.getName() + "\"");
		props.add("\"duration\": " + (event.getEndDate().getTime() - event.getStartDate().getTime()) + "");
		
		return "{" + MiscUtils.implode(props, ", ") + "}";
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/events/edit/{id}", method = RequestMethod.GET)
	public ModelAndView edit(@PathVariable("id") String sEventId, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("events/edit");
		Event e = eventService.get(KID.get(sEventId), AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
		
		mv.addObject("event", e);
		mv.addObject("pageTitle", e.getName());
		mv = prepareEdit(mv);
		
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/events/{id}", method = RequestMethod.GET)
	public ModelAndView details(@PathVariable("id") String sEventId, HttpSession session, HttpServletRequest req) throws KommetException
	{
		ModelAndView mv = new ModelAndView("events/details");
		Event e = eventService.get(KID.get(sEventId), AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
		
		mv.addObject("event", e);
		mv.addObject("pageTitle", e.getName());
		
		Breadcrumbs.add(req.getRequestURL().toString(), e.getName(), appConfig.getBreadcrumbMax(), session, getContextPath(session));
		
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/events/save", method = RequestMethod.POST)
	public ModelAndView save(@RequestParam(value = "eventId", required = false) String sEventId,
							@RequestParam(value = "name", required = false) String name,
							@RequestParam(value = "startDate", required = false) String sStartDate,
							@RequestParam(value = "endDate", required = false) String sEndDate,
							@RequestParam(required = false, value = "startDateHour") String sStartDateHour,
							@RequestParam(required = false, value = "endDateHour") String sEndDateHour,
							@RequestParam(value = "description", required = false) String description,
							@RequestParam(value = "ownerId", required = false) String sOwnerId,
							HttpSession session) throws KommetException
	{
		clearMessages();
		
		Event e = new Event();
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		
		if (StringUtils.hasText(sEventId))
		{
			e = eventService.get(KID.get(sEventId), authData, env);
		}
		
		if (!StringUtils.hasText(name))
		{
			addError("Name is empty");
		}
		e.setName(name);
		e.setDescription(description);
		
		Calendar startDate = Calendar.getInstance();
		// set the user's time zone one the due date
		startDate.setTimeZone(TimeZone.getTimeZone(authData.getUser().getTimezone()));
		
		if (!StringUtils.hasText(sStartDate))
		{
			addError("Start date is empty");
		}
		else
		{
			try
			{
				startDate.setTime(MiscUtils.parseDateTime(sStartDate, true));
			}
			catch (ParseException ex)
			{
				addError("Incorrect start date format");
			}
		}
		
		if (!StringUtils.hasText(sStartDateHour))
		{
			addError("Start date hour not selected");
		}
		else
		{
			startDate = parseHour(startDate, sStartDateHour);
		}
			
		e.setStartDate(startDate.getTime());
		
		Calendar endDate = Calendar.getInstance();
		// set the user's time zone one the due date
		endDate.setTimeZone(TimeZone.getTimeZone(authData.getUser().getTimezone()));
		
		if (!StringUtils.hasText(sEndDate))
		{
			addError("End date is empty");
		}
		else
		{
			try
			{
				endDate.setTime(MiscUtils.parseDateTime(sEndDate, true));
			}
			catch (ParseException ex)
			{
				addError("Incorrect end date format");
			}
		}
		
		if (!StringUtils.hasText(sEndDateHour))
		{
			addError("End date hour not selected");
		}
		else
		{
			endDate = parseHour(endDate, sEndDateHour);
		}
			
		e.setEndDate(endDate.getTime());
		
		if (!StringUtils.hasText(sOwnerId))
		{
			addError("Event owner not selected");
		}
		else
		{
			User owner = new User();
			owner.setId(KID.get(sOwnerId));
			e.setOwner(owner);
		}
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("events/edit");
			mv.addObject("event", e);
			mv.addObject("pageTitle", e.getName());
			mv.addObject("errorMsgs", getErrorMsgs());
			mv = prepareEdit(mv);
			return mv;
		}
		
		try
		{
			eventService.save(e, authData, env);
			
			// redirect to event details
			return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/events/" + e.getId());
		}
		catch (Exception ex)
		{
			addError("Saving event failed: " + ex.getMessage());
			ModelAndView mv = new ModelAndView("events/edit");
			mv.addObject("event", e);
			mv.addObject("pageTitle", e.getName());
			mv.addObject("errorMsgs", getErrorMsgs());
			mv = prepareEdit(mv);
			return mv;
		}
	}
	
	private Calendar parseHour(Calendar date, String sTime)
	{
		String[] bits = sTime.split("\\:");
		
		String sHour = bits[0];
		if (sHour.startsWith("0"))
		{
			sHour = sHour.substring(1);
		}
		
		String sMinute = bits[1];
		if (sMinute.startsWith("0"))
		{
			sMinute = sMinute.substring(1);
		}
		
		date.set(Calendar.HOUR, Integer.valueOf(sHour));
		date.set(Calendar.MINUTE, Integer.valueOf(sMinute));
		
		return date;
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/events/delete", method = RequestMethod.POST)
	@ResponseBody
	public void delete (@RequestParam(value = "id", required = false) String sEventId, HttpServletResponse response, HttpSession session) throws KommetException, IOException
	{
		KID eventId = null;
		PrintWriter out = response.getWriter();
		
		try
		{
			eventId = KID.get(sEventId);
		}
		catch (KIDException e)
		{
			out.write(getErrorJSON("Invalid event ID " + sEventId));
			return;
		}
		
		try
		{
			eventService.delete(eventId, AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
			out.write(getSuccessJSON("Text label has been successfully deleted"));
			return;
		}
		catch (ManipulatingReferencedLabelException e)
		{
			out.write(getErrorJSON(e.getMessage()));
			return;
		}
		catch (KommetException e)
		{
			out.write(getErrorJSON("Error deleting event: " + e.getMessage()));
			return;
		}
	}
	
	private ModelAndView prepareEdit(ModelAndView mv)
	{
		List<String> hours = new ArrayList<String>();
		for (Integer i = 0; i < 24; i++)
		{
			String sHour = String.valueOf(i);
			if (sHour.length() == 1)
			{
				sHour = "0" + sHour;
			}
			
			hours.add(sHour + ":00");
			hours.add(sHour + ":30");
		}
		mv.addObject("hourList", hours);
		
		//mv.addObject("startHour", attributeValue)
		
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/events/new", method = RequestMethod.GET)
	public ModelAndView create(HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("events/edit");
		mv.addObject("pageTitle", "New event");
		
		Event event = new Event();
		event.setOwner(AuthUtil.getAuthData(session).getUser());
		mv.addObject("event", event);
		
		mv = prepareEdit(mv);
		
		return mv;
	}
}