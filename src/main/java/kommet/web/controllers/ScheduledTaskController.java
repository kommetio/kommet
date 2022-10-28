/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthUtil;
import kommet.basic.Class;
import kommet.basic.ScheduledTask;
import kommet.basic.UniqueCheck;
import kommet.basic.keetle.tags.breadcrumbs.Breadcrumbs;
import kommet.basic.types.ScheduledTaskKType;
import kommet.data.DataService;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.UniqueCheckService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.koll.ClassService;
import kommet.koll.compiler.KommetCompiler;
import kommet.scheduler.ScheduledTaskException;
import kommet.scheduler.ScheduledTaskFilter;
import kommet.scheduler.ScheduledTaskService;
import kommet.utils.AppConfig;
import kommet.utils.PropertyUtilException;
import kommet.utils.UrlUtil;
import kommet.utils.ValidationUtil;

@Controller
public class ScheduledTaskController extends CommonKommetController
{
	@Inject
	ScheduledTaskService schedulerService;
	
	@Inject
	EnvService envService;
	
	@Inject
	ClassService classService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	DataService dataService;
	
	@Inject
	UniqueCheckService uniqueCheckService;
	
	@Inject
	SchedulerFactoryBean schedulerFactory;
	
	@Inject
	AppConfig appConfig;
	
	private static final Logger log = LoggerFactory.getLogger(ScheduledTaskController.class);
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/createscheduledtasktype", method = RequestMethod.GET)
	public ModelAndView createScheduledTaskType (HttpSession session) throws PropertyUtilException, KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		ScheduledTaskKType scheduledTaskType = new ScheduledTaskKType(env.getType(KeyPrefix.get(KID.CLASS_PREFIX)));
		scheduledTaskType.setPackage(scheduledTaskType.getPackage());
		scheduledTaskType = (ScheduledTaskKType)dataService.createType(scheduledTaskType, env);
		
		// create a unique constraint on file id, method and cron expression
		UniqueCheck checkOne = new UniqueCheck();
		checkOne.setName("Unique scheduled file, method and cron expression");
		checkOne.setTypeId(scheduledTaskType.getKID());
		checkOne.addField(scheduledTaskType.getField("file"));
		checkOne.addField(scheduledTaskType.getField("method"));
		checkOne.addField(scheduledTaskType.getField("cronExpression"));
		
		// create a unique constraint on file id, method and cron expression
		UniqueCheck checkTwo = new UniqueCheck();
		checkTwo.setName("UniqueScheduledTaskName");
		checkTwo.setTypeId(scheduledTaskType.getKID());
		checkTwo.addField(scheduledTaskType.getField("name"));
		
		uniqueCheckService.save(checkOne, dataService.getRootAuthData(env), env);
		
		// set name as the default field for file
		dataService.setDefaultField(scheduledTaskType.getKID(), scheduledTaskType.getField("name").getKID(), dataService.getRootAuthData(env), env);
		
		return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/me");
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/scheduledtasks", method = RequestMethod.GET)
	public ModelAndView scheduledTasks (HttpSession session, HttpServletRequest req) throws PropertyUtilException, KommetException
	{
		Breadcrumbs.add(req.getRequestURL().toString(), "Scheduled tasks", appConfig.getBreadcrumbMax(), session);
		
		ModelAndView mv = new ModelAndView("scheduledtasks/list");
		mv.addObject("tasks", schedulerService.get(new ScheduledTaskFilter(), envService.getCurrentEnv(session)));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/scheduledtasks/new", method = RequestMethod.GET)
	public ModelAndView createTask (HttpSession session) throws PropertyUtilException, KommetException
	{
		ModelAndView mv = new ModelAndView("scheduledtasks/edit");
		//mv.addObject("kollFiles", classService.getClasses(null, envService.getCurrentEnv(session)));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/scheduledtasks/jobs", method = RequestMethod.GET)
	public ModelAndView jobList (HttpSession session) throws PropertyUtilException, KommetException
	{
		ModelAndView mv = new ModelAndView("scheduledtasks/joblist");
		mv.addObject("jobs", schedulerService.getScheduledJobs(envService.getCurrentEnv(session).getId()));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/scheduledtask/{taskId}", method = RequestMethod.GET)
	public ModelAndView details (@PathVariable("taskId") String sTaskId, HttpSession session, HttpServletRequest req) throws PropertyUtilException, KommetException
	{
		KID taskId = null;
		try
		{
			taskId = KID.get(sTaskId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid ID " + sTaskId);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		ScheduledTask task = schedulerService.get(taskId, env);
		
		if (task == null)
		{
			return getErrorPage("Scheduled task with ID " + taskId + " not found");
		}
		
		Breadcrumbs.add(req.getRequestURL().toString(), task.getName(), appConfig.getBreadcrumbMax(), session);
		
		ModelAndView mv = new ModelAndView("scheduledtasks/details");
		mv.addObject("task", task);
		mv.addObject("scheduledJob", schedulerService.getScheduledJob(task.getQuartzJobName(), env.getId()));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/scheduledtasks/edit/{taskId}", method = RequestMethod.GET)
	public ModelAndView edit (@PathVariable("taskId") String sTaskId, HttpSession session) throws PropertyUtilException, KommetException
	{
		KID taskId = null;
		try
		{
			taskId = KID.get(sTaskId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid ID " + sTaskId);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		ScheduledTask task = schedulerService.get(taskId, env);
		
		if (task == null)
		{
			return getErrorPage("Scheduled task with ID " + taskId + " not found");
		}
		
		ModelAndView mv = new ModelAndView("scheduledtasks/edit");
		//mv.addObject("kollFiles", classService.getClasses(null, env));
		mv.addObject("task", task);
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/scheduledtasks/execute", method = RequestMethod.POST)
	@ResponseBody
	public void execute (@RequestParam("taskId") String sTaskId, HttpSession session, HttpServletResponse resp) throws IOException, KommetException
	{
		PrintWriter out = resp.getWriter();
		
		KID taskId = KID.get(sTaskId);
		EnvData env = envService.getCurrentEnv(session);
		
		ScheduledTask task = schedulerService.get(taskId, env);
		if (task == null)
		{
			out.write(getErrorJSON("Task with ID " + sTaskId + " does not exist"));
			return;
		}
		
		try
		{
			ScheduledTaskService.execute(task, schedulerFactory.getScheduler(), env.getId());
			out.write(getSuccessJSON("Task executed successfully"));
			return;
		}
		catch (ScheduledTaskException e)
		{
			e.printStackTrace();
			out.write(getErrorJSON("Error executing task: " + e.getMessage()));
			return;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/scheduledtasks/delete", method = RequestMethod.POST)
	@ResponseBody
	public void delete (@RequestParam(value = "taskId", required = false) String sTaskId, HttpSession session,
								HttpServletResponse resp) throws IOException
	{
		PrintWriter out = resp.getWriter();
		
		if (!StringUtils.hasText(sTaskId))
		{
			out.write(getErrorJSON("Empty task ID"));
			return;
		}
		
		KID taskId = null;
		try
		{
			taskId = KID.get(sTaskId);
		}
		catch (KIDException e)
		{
			out.write(getErrorJSON("Invalid task ID " + sTaskId));
			return;
		}
		
		try
		{
			schedulerService.unschedule(taskId, true, envService.getCurrentEnv(session));
			out.write(getSuccessJSON("Task deleted"));
			return;
		}
		catch (KommetException e)
		{
			out.write(getErrorJSON("Error deleting task: " + e.getMessage()));
			return;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/scheduledtasks/save", method = RequestMethod.POST)
	public ModelAndView save (@RequestParam(value = "taskId", required = false) String sTaskId,
			@RequestParam(value = "method", required = false) String method,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "cronExpression", required = false) String cronExpression,
			@RequestParam(value = "fileId", required = false) String sFileId,
			HttpSession session) throws PropertyUtilException, KommetException
	{	
		clearMessages();
		EnvData env = envService.getCurrentEnv(session);
		
		ScheduledTask task = null;
		boolean isNewTask = !StringUtils.hasText(sTaskId);
		
		if (!isNewTask)
		{
			KID taskId = null;
			try
			{
				taskId = KID.get(sTaskId);
			}
			catch (KIDException e)
			{
				return getErrorPage("Invalid ID " + sTaskId);
			}
			
			task = schedulerService.get(taskId, env);
			
			if (task == null)
			{
				return getErrorPage("Scheduled task with ID " + taskId + " not found");
			}
		}
		else
		{
			task = new ScheduledTask();
		}
		
		if (!StringUtils.hasText(method))
		{
			addError("Method is empty");
		}
		else
		{
			task.setMethod(method);
		}
		
		if (!StringUtils.hasText(name))
		{
			addError("Task name is empty");
		}
		else if (!ValidationUtil.isValidOptionallyQualifiedResourceName(name))
		{
			addError("Invalid scheduled task name " + name);
		}
		else
		{
			task.setName(name);
		}
		
		if (!StringUtils.hasText(cronExpression))
		{
			addError("CRON expression name is empty");
		}
		else
		{
			task.setCronExpression(cronExpression);
		}
		
		
		Class file = null;
		if (!StringUtils.hasText(sFileId))
		{
			addError("File not specified");
		}
		else
		{
			file = classService.getClass(KID.get(sFileId), env);
			task.setFile(file);
		}
		
		// get class from env
		java.lang.Class<?> compiledClass = null;
		
		if (StringUtils.hasText(method))
		{
			try
			{
				compiledClass = compiler.getClass(file, false, env);
			}
			catch (MalformedURLException e)
			{
				addError("Error getting compiled class " + file.getQualifiedName() + ": " + e.getMessage());
			}
			catch (ClassNotFoundException e)
			{
				addError("Compiled class not found: " + file.getQualifiedName());
			}
			
			if (compiledClass != null)
			{
				try
				{
					compiledClass.getMethod(method);
				}
				catch (SecurityException e)
				{
					addError("Method " + method + " is not public");
				}
				catch (NoSuchMethodException e)
				{
					addError("Method " + method + " does not exist in class " + file.getQualifiedName());
				}
			}
		}
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("scheduledtasks/edit");
			//mv.addObject("kollFiles", classService.getClasses(null, env));
			mv.addObject("task", task);
			mv.addObject("errorMsgs", getErrorMsgs());
			return mv;
		}
		
		// first unschedule any previous jobs for this task
		if (!isNewTask)
		{
			log.debug("Rescheduling task - unscheduling old task");
			schedulerService.unschedule(task.getId(), false, env);
		}
		
		try
		{
			// schedule task
			schedulerService.schedule(task, AuthUtil.getAuthData(session), env);
		}
		catch (KommetException e)
		{
			ModelAndView mv = new ModelAndView("scheduledtasks/edit");
			//mv.addObject("kollFiles", classService.getClasses(null, env));
			mv.addObject("task", task);
			mv.addObject("errorMsgs", getMessage(e.getMessage()));
			return mv;
		}
		
		ModelAndView mv = new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/scheduledtask/" + task.getId());
		return mv;
	}
}