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
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.Profile;
import kommet.basic.Task;
import kommet.basic.User;
import kommet.basic.UserGroup;
import kommet.data.FieldValidationException;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.ValidationMessage;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.rest.RestUtil;
import kommet.security.RestrictedAccess;
import kommet.services.ReminderService;
import kommet.services.TaskService;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

@Controller
public class TaskController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	TaskService taskService;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@Inject
	ReminderService reminderService;
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/tasks/list", method = RequestMethod.GET)
	public ModelAndView list(HttpSession session) throws KommetException
	{
		return new ModelAndView("tasks/list");
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/tasks/userlist", method = RequestMethod.GET)
	public ModelAndView listForUser(@RequestParam(required = false, value = "id") String sTaskId, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("tasks/userlist");
		addLayoutPath(uchService, mv, AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
		mv.addObject("taskId", sTaskId);
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/tasks/delete", method = RequestMethod.POST)
	@ResponseBody
	public void deleteTask(@RequestParam(required = false, value = "id") String sTaskId, HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		
		if (!StringUtils.hasText(sTaskId))
		{
			out.write(RestUtil.getRestErrorResponse("Task id not specified"));
			return;
		}
		
		KID taskId = null;
		
		try
		{
			taskId = KID.get(sTaskId);
		}
		catch (KIDException e)
		{
			out.write(RestUtil.getRestErrorResponse("Invalid task ID '" + sTaskId + "'"));
			return;
		}
		
		AuthData authData = AuthUtil.getAuthData(session);
		
		// delete task
		taskService.delete(taskId, authData, envService.getCurrentEnv(session));
		
		out.write(RestUtil.getRestSuccessResponse(authData.getI18n().get("tasks.successfully.deleted")));
		return;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/tasks/new", method = RequestMethod.GET)
	public ModelAndView create(HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("tasks/edit");
		mv.addObject("pageTitle", AuthUtil.getAuthData(session).getI18n().get("tasks.newtask.title"));
		
		Task newTask = new Task();
		newTask.setPriority(3);
		mv.addObject("task", newTask);
		
		mv = prepareTaskEdit(mv);
		
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/tasks/save", method = RequestMethod.POST)
	public ModelAndView save(@RequestParam(required = false, value = "taskId") String sTaskId,
							@RequestParam(required = false, value = "title") String title,
							@RequestParam(required = false, value = "content") String content,
							@RequestParam(required = false, value = "priority") String sPriority,
							@RequestParam(required = false, value = "dueDate") String sDueDate,
							@RequestParam(required = false, value = "hour") String sHour,
							@RequestParam(required = false, value = "minute") String sMinute,
							@RequestParam(required = false, value = "status") String status,
							@RequestParam(required = false, value = "progress") String sProgress,
							@RequestParam(required = false, value = "assignedUserId") String sAssignedUserId,
							@RequestParam(required = false, value = "assignedGroupId") String sAssignedGroupId,
							HttpSession session) throws KommetException
	{
		clearMessages();
		
		Task task = null;
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		if (StringUtils.hasText(sTaskId))
		{
			KID taskId = null;
			
			try
			{
				taskId = KID.get(sTaskId);
			}
			catch (KIDException e)
			{
				return getErrorPage("Invalid app ID " + sTaskId);
			}
			
			task = taskService.get(taskId, authData, env);
		}
		else
		{
			task = new Task();
		}
		
		task.setTitle(title);
		task.setContent(content);
		task.setStatus(status);
		
		Integer progress = 0;
		
		/*if (!StringUtils.hasText(content))
		{
			addError(authData.getI18n().get("task.err.empty.content"));
		}*/
		
		if (!StringUtils.hasText(title))
		{
			addError(authData.getI18n().get("task.err.empty.title"));
		}
		
		if (!StringUtils.hasText(sPriority))
		{
			addError(authData.getI18n().get("task.err.empty.priority"));
		}
		else
		{
			task.setPriority(Integer.valueOf(sPriority));
		}
		
		Calendar dueDate = Calendar.getInstance();
		// set the user's time zone one the due date
		dueDate.setTimeZone(TimeZone.getTimeZone(authData.getUser().getTimezone()));
		
		if (!StringUtils.hasText(sDueDate))
		{
			addError(authData.getI18n().get("task.err.empty.duedate"));
		}
		else
		{
			try
			{
				dueDate.setTime(MiscUtils.parseDateTime(sDueDate, true));
			}
			catch (ParseException e)
			{
				addError(authData.getI18n().get("task.err.incorrect.date"));
			}
		}
		
		if (!StringUtils.hasText(sHour))
		{
			addError(authData.getI18n().get("task.err.empty.hour"));
		}
		else
		{
			dueDate.set(Calendar.HOUR, Integer.valueOf(sHour));
		}
		
		if (!StringUtils.hasText(sMinute))
		{
			addError(authData.getI18n().get("task.err.empty.minute"));
		}
		else
		{
			dueDate.set(Calendar.MINUTE, Integer.valueOf(sMinute));
		}
			
		task.setDueDate(dueDate.getTime());
		
		int assigneeCount = 0;
		
		if (StringUtils.hasText(sAssignedUserId))
		{
			User assignedUser = new User();
			assignedUser.setId(KID.get(sAssignedUserId));
			task.setAssignedUser(assignedUser);
			
			assigneeCount++;
		}
		
		if (StringUtils.hasText(sAssignedGroupId))
		{
			UserGroup assignedGroup = new UserGroup();
			assignedGroup.setId(KID.get(sAssignedGroupId));
			task.setAssignedGroup(assignedGroup);
			
			assigneeCount++;
		}
		
		if (assigneeCount == 0)
		{
			addError(authData.getI18n().get("task.err.no.assignee"));
		}
		else if (assigneeCount > 1)
		{
			addError(authData.getI18n().get("task.err.both.assignee.types"));
		}
		
		if (!StringUtils.hasText(status))
		{
			addError(authData.getI18n().get("task.err.empty.status"));
		}
		
		if (!StringUtils.hasText(status))
		{
			status = "Open";
		}
		
		if (StringUtils.hasText(sProgress))
		{
			progress = Integer.valueOf(sProgress);
			task.setProgress(progress);
		}
		
		task.setStatus(status);
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("tasks/edit");
			mv.addObject("task", task);
			mv.addObject("pageTitle", task.getId() != null ? task.getTitle() + " - edit" : authData.getI18n().get("tasks.newtask.title"));
			mv.addObject("errorMsgs", getErrorMsgs());
			prepareTaskEdit(mv);
			return mv;
		}
		
		try
		{
			// save task
			task = taskService.save(task, authData, env);
		}
		catch (FieldValidationException e)
		{
			for (ValidationMessage msg : e.getMessages())
      		{
      			addError(msg.getText());
      		}
			
			ModelAndView mv = new ModelAndView("tasks/edit");
			mv.addObject("task", task);
			mv.addObject("pageTitle", task.getTitle() + " - edit");
			mv.addObject("errorMsgs", getErrorMsgs());
			prepareTaskEdit(mv);
			return mv;
		}
		catch (Exception e)
		{
			ModelAndView mv = new ModelAndView("tasks/edit");
			mv.addObject("task", task);
			mv.addObject("pageTitle", task.getTitle() + " - edit");
			mv.addObject("errorMsgs", getMessage(e.getMessage()));
			prepareTaskEdit(mv);
			return mv;
		}
		
		// redirect to app details
		return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/tasks/" + task.getId());
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/tasklist/save", method = RequestMethod.POST)
	@ResponseBody
	public void saveTaskForList(@RequestParam(required = false, value = "taskId") String sTaskId,
							@RequestParam(required = false, value = "title") String title,
							@RequestParam(required = false, value = "content") String content,
							@RequestParam(required = false, value = "priority") String sPriority,
							@RequestParam(required = false, value = "dueDate") String sDueDate,
							@RequestParam(required = false, value = "status") String status,
							@RequestParam(required = false, value = "progress") String sProgress,
							@RequestParam(required = false, value = "assignedUserId") String sAssignedUserId,
							@RequestParam(required = false, value = "recordId") String sRecordId,
							@RequestParam(required = false, value = "assignedGroupId") String sAssignedGroupId,
							HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		clearMessages();
		
		Task task = null;
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		PrintWriter out = resp.getWriter();
		
		if (StringUtils.hasText(sTaskId))
		{
			KID taskId = null;
			
			try
			{
				taskId = KID.get(sTaskId);
			}
			catch (KIDException e)
			{
				out.write(RestUtil.getRestErrorResponse("Invalid app ID " + sTaskId));
				return;
			}
			
			task = taskService.get(taskId, authData, env);
		}
		else
		{
			task = new Task();
		}
		
		task.setTitle(title);
		task.setContent(content);
		
		if (!StringUtils.hasText(status))
		{
			// default status
			status = "Open";
		}
		
		task.setStatus(status);
		
		Integer progress = 0;
		
		/*if (!StringUtils.hasText(content))
		{
			addError(authData.getI18n().get("task.err.empty.content"));
		}*/
		
		if (!StringUtils.hasText(title))
		{
			addError(authData.getI18n().get("task.err.empty.title"));
		}
		
		if (!StringUtils.hasText(sPriority))
		{
			addError(authData.getI18n().get("task.err.empty.priority"));
		}
		else
		{
			task.setPriority(Integer.valueOf(sPriority));
		}
		
		Calendar dueDate = Calendar.getInstance();
		// set the user's time zone one the due date
		//dueDate.setTimeZone(TimeZone.getTimeZone(authData.getUser().getTimezone()));
		
		if (StringUtils.hasText(sDueDate))
		{
			if (NumberUtils.isNumber(sDueDate))
			{
				task.setDueDate(new Date(Long.parseLong(sDueDate)));
			}
			else
			{
				try
				{
					// the date has format with hours and minutes, but without seconds
					// so we add seconds so that it matches MiscUtils.DATE_TIME_FORMAT_DEFAULT
					dueDate.setTime(MiscUtils.parseDateTime(sDueDate + ":00", true));
					task.setDueDate(dueDate.getTime());
				}
				catch (ParseException e)
				{
					addError(authData.getI18n().get("task.err.incorrect.date"));
				}
			}
		}
		
		/*if (!StringUtils.hasText(sHour))
		{
			addError(authData.getI18n().get("task.err.empty.hour"));
		}
		else
		{
			dueDate.set(Calendar.HOUR, Integer.valueOf(sHour));
		}
		
		if (!StringUtils.hasText(sMinute))
		{
			addError(authData.getI18n().get("task.err.empty.minute"));
		}
		else
		{
			dueDate.set(Calendar.MINUTE, Integer.valueOf(sMinute));
		}*/
		
		int assigneeCount = 0;
		
		if (StringUtils.hasText(sAssignedUserId))
		{
			User assignedUser = new User();
			assignedUser.setId(KID.get(sAssignedUserId));
			task.setAssignedUser(assignedUser);
			
			assigneeCount++;
		}
		
		if (StringUtils.hasText(sAssignedGroupId))
		{
			UserGroup assignedGroup = new UserGroup();
			assignedGroup.setId(KID.get(sAssignedGroupId));
			task.setAssignedGroup(assignedGroup);
			
			assigneeCount++;
		}
		
		if (assigneeCount > 1)
		{
			addError(authData.getI18n().get("task.err.both.assignee.types"));
		}
		
		if (!StringUtils.hasText(status))
		{
			status = "Open";
		}
		
		if (StringUtils.hasText(sProgress))
		{
			progress = Integer.valueOf(sProgress);
			task.setProgress(progress);
		}
		
		task.setStatus(status);
		
		if (hasErrorMessages())
		{
			out.write(RestUtil.getRestErrorResponse(getErrorMsgs()));
			return;
		}
		
		if (StringUtils.hasText(sRecordId))
		{
			// assign task to a specific record
			task.setRecordId(KID.get(sRecordId));
		}
		
		//boolean isNewTask = task.getId() == null;
		
		try
		{
			// save task
			task = taskService.save(task, authData, env);
			
			/*if (isNewTask && task.getAssignedUser() != null)
			{
				// create reminder
				Reminder reminder = new Reminder();
				reminder.setTitle("Task reminder: " + task.getTitle());
				reminder.setContent("Task is due");
				reminder.setRecordId(task.getId());
				reminder.setReferencedField(env.getType(KeyPrefix.get(KID.TASK_PREFIX)).getField("dueDate").getKID());
				reminder.setIntervalUnit("minute");
				reminder.setIntervalValue(1);
				reminder.setMedia("notification");
				reminder.setAssignedUser(task.getAssignedUser());
				reminderService.save(reminder, authData, env);
			}*/
		}
		catch (FieldValidationException e)
		{
			e.printStackTrace();
			for (ValidationMessage msg : e.getMessages())
      		{
      			addError(msg.getText());
      		}
			
			out.write(RestUtil.getRestErrorResponse(getErrorMsgs()));
			return;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			out.write(RestUtil.getRestErrorResponse(e.getMessage()));
			return;
		}
		
		// redirect to app details
		out.write(RestUtil.getRestSuccessResponse("Task saved"));
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/tasks/edit/{taskId}", method = RequestMethod.GET)
	public ModelAndView edit(@PathVariable("taskId") String sTaskId, HttpSession session) throws KommetException
	{
		clearMessages();
		
		KID taskId = null;
		try
		{
			taskId = KID.get(sTaskId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid task ID " + sTaskId);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		// find task
		Task task = taskService.get(taskId, authData, env);
		if (task == null)
		{
			return getErrorPage("Task with ID " + taskId + " not found");
		}
		
		ModelAndView mv = new ModelAndView("tasks/edit");
		mv.addObject("task", task);
		mv.addObject("pageTitle", task.getTitle() + " - edit");
		mv = prepareTaskEdit(mv);
		return mv;
	}
	
	private ModelAndView prepareTaskEdit(ModelAndView mv)
	{
		List<Integer> hours = new ArrayList<Integer>();
		for (Integer i = 0; i < 24; i++)
		{
			hours.add(i);
		}
		mv.addObject("hourList", hours);
		
		List<Integer> minutes = new ArrayList<Integer>();
		for (Integer i = 0; i < 60; i++)
		{
			minutes.add(i);
		}
		mv.addObject("minuteList", minutes);
		
		return mv;
	}

	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/tasks/{taskId}", method = RequestMethod.GET)
	public ModelAndView details(@PathVariable("taskId") String sTaskId, HttpSession session) throws KommetException
	{
		clearMessages();
		
		KID taskId = null;
		try
		{
			taskId = KID.get(sTaskId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid task ID " + sTaskId);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		// find task with assignee data (user group name and user name)
		Task task = taskService.get(taskId, true, authData, env);
		if (task == null)
		{
			return getErrorPage("Task with ID " + taskId + " not found");
		}
		
		ModelAndView mv = new ModelAndView("tasks/details");
		mv.addObject("task", task);
		return mv;
	}
}