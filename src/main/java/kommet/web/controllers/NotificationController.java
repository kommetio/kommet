/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.auth.UserService;
import kommet.basic.Notification;
import kommet.basic.Profile;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.QueryResultOrder;
import kommet.json.JSON;
import kommet.notifications.NotificationFilter;
import kommet.notifications.NotificationService;
import kommet.security.RestrictedAccess;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

@Controller
public class NotificationController extends CommonKommetController
{
	@Inject
	NotificationService notificationService;
	
	@Inject
	EnvService envService;
	
	@Inject
	UserService userService;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/notifications/new", method = RequestMethod.GET)
	public ModelAndView newNotification(@RequestParam(value = "parentDialog", required = false) String parentDialog,
										@RequestParam(value = "assigneeId", required = false) String sUserId,
										HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		ModelAndView mv = new ModelAndView("notifications/edit");
		mv.addObject("assigneeId", sUserId);
		mv.addObject("parentDialog", parentDialog);
		
		if (StringUtils.hasText(parentDialog))
		{
			// if page is run in widget mode, it will always be rendered with blank layout
			addLayoutPath(mv, env.getBlankLayoutId(), env);
		}
		else
		{
			addLayoutPath(uchService, mv, AuthUtil.getAuthData(session), env);
		}
		
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/notifications/save", method = RequestMethod.POST)
	@ResponseBody
	public void save (@RequestParam(value = "id", required = false) String sNotificationId,
						@RequestParam(value = "title", required = false) String title,
						@RequestParam(value = "text", required = false) String text,
						@RequestParam(value = "assigneeId", required = false) String sAssigneeId,
						@RequestParam(value = "parentDialog", required = false) String parentDialog,
						HttpServletResponse response, HttpSession session) throws IOException, KommetException
	{
		clearMessages();
		
		PrintWriter out = response.getWriter();
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		Notification nt = new Notification();
		
		if (StringUtils.hasText(sNotificationId))
		{
			KID id = null;
			try
			{
				id = KID.get(sNotificationId);
			}
			catch (KIDException e)
			{
				out.write(getErrorJSON("Incorrent notification ID " + sNotificationId));
				return;
			}
			
			nt = notificationService.get(id, authData, env);
			if (nt == null)
			{
				out.write(getErrorJSON("Notification with ID " + sNotificationId + " not found or inaccessible"));
				return;
			}
		}
		else
		{
			nt = new Notification();
		}
		
		if (StringUtils.hasText(title))
		{
			nt.setTitle(title);
		}
		else
		{
			addError("Title is required");
		}
		
		if (StringUtils.hasText(text))
		{
			nt.setText(text);
		}
		else
		{
			addError("Text is required");
		}
		
		if (StringUtils.hasText(sAssigneeId))
		{
			nt.setAssignee(userService.getUser(KID.get(sAssigneeId), env));
		}
		else
		{
			addError("Assignee is required");
		}
		
		if (hasErrorMessages())
		{
			out.write(getErrorJSON("Cannot save notification because some fields are not set. " + MiscUtils.implode(getErrorMsgs(), ", ")));
			return;
		}
		
		try
		{
			notificationService.save(nt, authData, env);
		}
		catch (KommetException e)
		{
			out.write(getErrorJSON("Error saving notification: " + e.getMessage()));
			return;
		}
	
		String successJSON = "{ \"status\": \"success\", \"message\": \"Notification saved successfully\", \"parentDialog\": " + (parentDialog != null ? ("\"" + parentDialog + "\"") : "null") + " }";
		out.write(successJSON);
		return;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/notifications/delete", method = RequestMethod.POST)
	@ResponseBody
	public void delete (@RequestParam("id") String sNotificationId, HttpServletResponse response, HttpSession session) throws IOException, KommetException
	{
		KID id = null;
		PrintWriter out = response.getWriter();
		
		try
		{
			id = KID.get(sNotificationId);
		}
		catch (KIDException e)
		{
			out.write(getErrorJSON("Incorrent notification ID " + sNotificationId));
			return;
		}
		
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		Notification notification = notificationService.get(id, authData, env);
		
		if (notification == null)
		{
			out.write(getErrorJSON("Notification with ID " + sNotificationId + " not found or inaccessible"));
			return;
		}
		
		notificationService.delete(notification, authData, env);
		out.write(getSuccessJSON("Notification deleted successfully"));
		return;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/notifications/setviewed", method = RequestMethod.POST)
	@ResponseBody
	public void setNotificationAsViewed (@RequestParam("id") String sNotificationId, HttpServletResponse response, HttpSession session) throws IOException, KommetException
	{
		KID id = null;
		PrintWriter out = response.getWriter();
		
		try
		{
			id = KID.get(sNotificationId);
		}
		catch (KIDException e)
		{
			out.write(getErrorJSON("Incorrent notification ID " + sNotificationId));
			return;
		}
		
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		
		try
		{
			notificationService.setViewedDate(id, new Date(), authData, env);
			out.write(getSuccessJSON("Notification changed successfully"));
			return;
		}
		catch (KommetException e)
		{
			out.write(getErrorJSON("Could not set notification with ID " + sNotificationId + " as viewed. Nested: " + e.getMessage()));
			return;
		}
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/notifications/list", method = RequestMethod.GET)
	public ModelAndView list (HttpSession session) throws IOException, KommetException
	{
		AuthData authData = AuthUtil.getAuthData(session);
		
		ModelAndView mv = new ModelAndView("notifications/list");
		EnvData env = envService.getCurrentEnv(session);
		NotificationFilter filter = new NotificationFilter();
		filter.setOrder(QueryResultOrder.DESC);
		filter.setOrderBy("createdDate");
		
		// query also user name
		List<String> props = new ArrayList<String>();
		props.add("assignee.userName");
		
		mv.addObject("notifications", notificationService.get(filter, props, authData, env)); 
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/notifications/me", method = RequestMethod.GET)
	@ResponseBody
	public void listForUser (@RequestParam(value = "unreadfirst", required = false) String unreadFirst, HttpServletResponse response, HttpSession session) throws IOException, KommetException
	{	
		PrintWriter out = response.getWriter();
		AuthData authData = AuthUtil.getAuthData(session);
		
		if (authData == null)
		{
			out.write(getErrorJSON("Insufficient privileges"));
			return;
		}
		
		EnvData env = null;
		
		try
		{
			env = envService.getCurrentEnv(session);
		}
		catch (KommetException e)
		{
			out.write(getErrorJSON("Error retrieving environment"));
			return;
		}
		
		try
		{
			// get notifications for current user, order by date, newest first
			NotificationFilter filter = new NotificationFilter();
			filter.addAssigneeId(authData.getUserId());
			filter.setOrder(QueryResultOrder.DESC);
			filter.setOrderBy("createdDate");
			
			// retrieve notification using null auth data, i.e. access all
			List<Notification> notifications = notificationService.get(filter, null, env);
			
			if ("1".equals(unreadFirst))
			{
				Collections.sort(notifications, new NotificationViewedDateComparator());
			}
			
			out.write(getSuccessDataJSON(JSON.serializeObjectProxies(notifications, MiscUtils.toSet("id", "title", "text", "viewedDate", "createdDate", "assignee.id"), authData)));
			return;
		}
		catch (KommetException e)
		{
			out.write(getErrorJSON("Error retrieving notifications: " + e.getMessage()));
			return;
		}
	}
	
	public class NotificationViewedDateComparator implements Comparator<Notification>
	{
	    @Override
	    public int compare(Notification o1, Notification o2)
	    {
	        if (o1.getViewedDate() != null && o2.getViewedDate() != null)
	        {
	        	return o1.getCreatedDate().compareTo(o2.getCreatedDate());
	        }
	        else if (o1.getViewedDate() == null)
	        {
	        	return -1;
	        }
	        else
	        {
	        	return 1;
	        }
	    }
	}
}