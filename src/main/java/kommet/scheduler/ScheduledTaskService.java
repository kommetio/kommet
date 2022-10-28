/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.scheduler;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.basic.Class;
import kommet.basic.ScheduledTask;
import kommet.basic.SystemContextAware;
import kommet.data.ExceptionErrorType;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.sharing.SharingService;
import kommet.emailing.EmailService;
import kommet.env.EnvData;
import kommet.koll.SystemContext;
import kommet.koll.SystemContextFactory;
import kommet.koll.compiler.KommetCompiler;
import kommet.utils.ValidationUtil;

@Service
public class ScheduledTaskService
{
	@Inject
	ScheduledTaskDao dao;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	SchedulerFactoryBean schedulerFactory;
	
	@Inject
	EmailService emailService;
	
	@Inject
	SharingService sharingService;
	
	@Inject
	SystemContextFactory sysContextFactory;
	
	private static final Logger log = LoggerFactory.getLogger(ScheduledTaskService.class);
	
	@Transactional(readOnly = true)
	public List<ScheduledTask> get(ScheduledTaskFilter filter, EnvData env) throws KommetException
	{
		return dao.get(filter, env);
	}
	
	@Transactional(readOnly = true)
	public ScheduledTask get(KID taskId, EnvData env) throws KommetException
	{
		ScheduledTaskFilter filter = new ScheduledTaskFilter();
		filter.addTaskId(taskId);
		List<ScheduledTask> tasks = dao.get(filter, env);
		return !tasks.isEmpty() ? tasks.get(0) : null;
	}
	
	@Transactional
	public void unschedule(KID taskId, boolean deleteTask, EnvData env) throws KommetException
	{
		ScheduledTask task = dao.get(taskId, env);
		
		if (task == null)
		{
			throw new ScheduledTaskException("Scheduled task with ID " + taskId + " does not exist");
		}
		
		try
		{
			schedulerFactory.getScheduler().deleteJob(JobKey.jobKey(task.getQuartzJobName(), getJobGroupNameForEnv(env.getId())));
		}
		catch (SchedulerException e)
		{
			throw new ScheduledTaskException("Error unscheduling task: " + e.getMessage());
		}
		
		if (deleteTask)
		{
			dao.delete(taskId, true, null, env);
		}
	}
	
	@Transactional
	public ScheduledTask schedule (Class file, String method, String name, String cronExpression, AuthData authData, EnvData env) throws KommetException
	{	
		ScheduledTask task = new ScheduledTask();
		task.setName(name);
		task.setCronExpression(cronExpression);
		task.setFile(file);
		task.setMethod(method);
		
		return schedule(task, authData, env);
	}

	@Transactional
	public ScheduledTask schedule(ScheduledTask task, AuthData authData, EnvData env) throws KommetException
	{
		if (!ValidationUtil.isValidOptionallyQualifiedResourceName(task.getName()))
		{
			throw new KommetException("Invalid scheduled task name " + task.getName());
		}
		
		java.lang.Class<?> classFile = null;
		
		// check if the class and method exist in the class loader
		try
		{
			// TODO when used with reload = true, this method fails - check why
			classFile = compiler.getClass(task.getFile(), false, env);
		}
		catch (MalformedURLException e)
		{
			throw new ScheduledTaskException("Error getting scheduled task class from class loader: " + e.getMessage());
		}
		catch (ClassNotFoundException e)
		{
			throw new ScheduledTaskException("Scheduled file class " + task.getFile().getPackageName() + "." + task.getFile().getName() + " not found in class loader");
		}
		
		try
		{
			classFile.getMethod(task.getMethod());
		}
		catch (SecurityException e)
		{
			throw new ScheduledTaskException("Security exception while trying to access scheduled task method: " + e.getMessage());
		}
		catch (NoSuchMethodException e)
		{
			throw new ScheduledTaskException("Method " + task.getMethod() + " does not exist in class " + classFile.getName());
		}
		
		task = dao.save(task, authData, env);
		
		try
		{
			createScheduler(task, authData, env);
		}
		catch (Exception e)
		{
			// delete the inserted task
			dao.delete(task.getId(), true, null, env);
			
			throw new ScheduledTaskException("Error scheduling task: " + e.getMessage());
		}
		
		return task;
	}
	
	@Transactional
	public void removeScheduler (ScheduledTask task, boolean isIgnoreNonExistingJobs, AuthData authData, EnvData env) throws ScheduledTaskException
	{
		log.info("Unscheduling job " + task.getName() + "[env " + env.getId() + "]");
		JobDetail jobDetail;
		try
		{
			jobDetail = schedulerFactory.getScheduler().getJobDetail(JobKey.jobKey(task.getQuartzJobName(), getJobGroupNameForEnv(env.getId())));
		}
		catch (SchedulerException e1)
		{
			throw new ScheduledTaskException("Error getting task " + task.getName());
		}
		
		if (jobDetail == null)
		{
			if (isIgnoreNonExistingJobs)
			{
				// just exit the method
				return;
			}
			else
			{
				throw new ScheduledTaskException("Job with name " + task.getName() + " on env " + env.getId() + " not found");
			}
		}
		
		try
		{
			// be sure to use method deleteJob instead of unscheduleJob
			schedulerFactory.getScheduler().deleteJob(JobKey.jobKey(task.getQuartzJobName(), getJobGroupNameForEnv(env.getId())));
		}
		catch (SchedulerException e)
		{
			throw new ScheduledTaskException("Error unscheduling task " + task.getName() + ": " + e.getMessage());
		}
	}
	
	/**
	 * Return all Quartz jobs for scheduled tasks for this environment.
	 * @param envId
	 * @return
	 * @throws ScheduledTaskException
	 */
	public List<ScheduledJob> getScheduledJobs(KID envId) throws ScheduledTaskException
	{
		List<ScheduledJob> jobs = new ArrayList<ScheduledJob>();
		
		try
		{
			Scheduler scheduler = schedulerFactory.getScheduler();
			String jobGroup = getJobGroupNameForEnv(envId);
			// get all job names for the given env
			final GroupMatcher<JobKey> groupMatcher = GroupMatcher.groupEquals(jobGroup);
			for (JobKey jobKey : scheduler.getJobKeys(groupMatcher))
			{
				ScheduledJob job = new ScheduledJob();
				JobDetail quartzJob = scheduler.getJobDetail(jobKey);
				job.setJobDetail((ScheduledTaskDetail)quartzJob);
				job.setTask(((ScheduledTaskDetail)quartzJob).getTask());
				job.setTrigger(scheduler.getTrigger(TriggerKey.triggerKey(getTriggerName(jobKey.getName()), jobGroup)));
				
				jobs.add(job);
			}
			
			return jobs;
		}
		catch (SchedulerException e)
		{
			throw new ScheduledTaskException("Error getting scheduled jobs: " + e.getMessage());
		}
	}
	
	public static String getJobGroupNameForEnv (KID envId)
	{
		return "scheduled-tasks-" + envId.getId(); 
	}
	
	public static String getTriggerName (String jobName)
	{
		return jobName + "_trigger";
	}
	
	@Transactional
	public void createScheduler (ScheduledTask task, AuthData authData, EnvData env) throws ScheduledTaskException
	{
		// check if a job for this task does not already exist
		JobDetail jobDetail;
		try
		{
			jobDetail = schedulerFactory.getScheduler().getJobDetail(JobKey.jobKey(task.getQuartzJobName(), getJobGroupNameForEnv(env.getId())));
		}
		catch (SchedulerException e1)
		{
			throw new ScheduledTaskException("Error getting task " + task.getName());
		}
		
		if (jobDetail != null)
		{
			// be sure to use method deleteJob instead of unscheduleJob
			try
			{
				schedulerFactory.getScheduler().deleteJob(JobKey.jobKey(task.getQuartzJobName(), getJobGroupNameForEnv(env.getId())));
			}
			catch (SchedulerException e)
			{
				throw new ScheduledTaskException("Cannot unschedule job " + task.getQuartzJobName() + " for task " + task.getName() + ". Nested: " + e.getMessage());
			}
		}
		
		// create a new job
		ScheduledTaskDetail newJob = new ScheduledTaskDetail(task, sysContextFactory.get(authData, env), compiler, ScheduledQuartzJob.class, env);
		log.info("Scheduling task " + task.getName() + " with CRON " + task.getCronExpression());
		
    	try
    	{
    		// create a trigger for this job
    		CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(getTriggerName(newJob.getName()), newJob.getGroup()).withSchedule(CronScheduleBuilder.cronSchedule(task.getCronExpression())).build();
    		schedulerFactory.getScheduler().scheduleJob(newJob, trigger);
    	}
    	catch (Exception e)
    	{
    		throw new ScheduledTaskException("Error scheduling task: " + e.getMessage(), ExceptionErrorType.SCHEDULING_TASK_JOB_FAILED);
    	}
	}

	/**
	 * Returns scheduled job information for the given task
	 * @param taskName The name of the task
	 * @param envId Environment ID
	 * @return
	 * @throws ScheduledTaskException
	 */
	public ScheduledJob getScheduledJob(String taskName, KID envId) throws ScheduledTaskException
	{	
		try
		{
			Scheduler scheduler = schedulerFactory.getScheduler();
			String jobGroup = getJobGroupNameForEnv(envId);
			
			ScheduledJob job = new ScheduledJob();
			JobDetail quartzJob = scheduler.getJobDetail(JobKey.jobKey(taskName, jobGroup));
			
			if (quartzJob == null)
			{
				return null;
			}
			
			job.setJobDetail((ScheduledTaskDetail)quartzJob);
			job.setTask(((ScheduledTaskDetail)quartzJob).getTask());
			job.setTrigger(scheduler.getTrigger(TriggerKey.triggerKey(getTriggerName(quartzJob.getKey().getName()), getJobGroupNameForEnv(envId))));
				
			return job;
		}
		catch (SchedulerException e)
		{
			throw new ScheduledTaskException("Error getting scheduled job for task " + taskName + ": " + e.getMessage());
		}
	}
	
	public static void execute (ScheduledTask task, Scheduler scheduler, KID envId) throws ScheduledTaskException
	{
		java.lang.Class<?> cls = null;
		ScheduledTaskDetail detail = getScheduledTaskDetail(task, scheduler, envId);
		SystemContext sys = detail.getSystemContext();
		
		try
		{
			cls = sys.getCompiler().getClass(detail.getTask().getFile(), false, sys.getEnv());
		}
		catch (MalformedURLException e)
		{
			throw new ScheduledTaskExecutionException("Error running scheduled task: " + e.getMessage());
		}
		catch (ClassNotFoundException e)
		{
			throw new ScheduledTaskExecutionException("Scheduled file " + detail.getTask().getFile().getName() + " not found");
		}
		catch (KommetException e)
		{
			throw new ScheduledTaskExecutionException("Error executing scheduled task: " + e.getMessage());
		}
		
		Object instance = null;
		
		try
		{
			instance = cls.newInstance();
		}
		catch (Exception e)
		{
			throw new ScheduledTaskExecutionException("Error instantiating scheduled task class: " + e.getMessage());
		}
		
		java.lang.Class<?> systemContextAware = null;
		try
		{
			systemContextAware = detail.getCompiler().getClass(SystemContextAware.class.getName(), false, sys.getEnv());
		}
		catch (KommetException e1)
		{
			throw new ScheduledTaskExecutionException("Error getting interface " + SystemContextAware.class.getName() + " from class loader");
		}
		catch (ClassNotFoundException e1)
		{
			throw new ScheduledTaskExecutionException("Interface " + SystemContextAware.class.getName() + " not found");
		}
		
		if (systemContextAware == null)
		{
			throw new ScheduledTaskExecutionException("SystemContextAwara interface not found in environment class loader");
		}
		
		// if scheduler class implements SystemContextAware, system context needs to be injected
		if (systemContextAware.isAssignableFrom(cls))
		{
			try
			{
				Method sysContextSetter = cls.getMethod("setSystemContext", sys.getClass());
				sysContextSetter.invoke(instance, sys);
			}
			catch (Exception e)
			{
				throw new ScheduledTaskExecutionException("Error injecting system context into scheduled class: " + e.getMessage());
			}
		}
		
		Method method = null;
		
		// add auth data to the thread so that classes are invoked in this context
		sys.getEnv().addAuthData(sys.getAuthData());
		
		try
		{
			method = cls.getMethod(detail.getTask().getMethod());
		}
		catch (Exception e)
		{
			throw new ScheduledTaskExecutionException("Error getting method " + detail.getTask().getMethod() + " in file " + detail.getTask().getFile().getName());
		}
		
		// call scheduled method
		try
		{
			method.invoke(instance);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new ScheduledTaskExecutionException("Error executing scheduled task: " + e.getMessage());
		}
		finally
		{
			sys.getEnv().clearAuthData();
		}
	}

	public static ScheduledTaskDetail getScheduledTaskDetail(ScheduledTask task, Scheduler scheduler, KID envId) throws ScheduledTaskException
	{	
		try
		{
			return (ScheduledTaskDetail)scheduler.getJobDetail(JobKey.jobKey(task.getQuartzJobName(), getJobGroupNameForEnv(envId)));
		}
		catch (SchedulerException e)
		{
			throw new ScheduledTaskException("Error getting job detail for task: " + e.getMessage());
		}
	}

	@Transactional(readOnly = true)
	public ScheduledTask getByName(String name, AuthData authData, EnvData env) throws KommetException
	{
		ScheduledTaskFilter filter = new ScheduledTaskFilter();
		filter.setName(name);
		
		List<ScheduledTask> tasks = dao.get(filter, env);
		return tasks.isEmpty() ? null : tasks.get(0);
	}
	
	/**
	 * Removes all scheduled jobs for the given environment, while leaving the scheduled tasks in the database
	 * @throws SchedulerException 
	 * @throws ScheduledTaskException 
	 */
	public void clearAllScheduledTasks(KID envId) throws ScheduledTaskException, SchedulerException
	{
		List<ScheduledJob> jobsForEnv = getScheduledJobs(envId);
		Scheduler scheduler = schedulerFactory.getScheduler();
		
		for (ScheduledJob job : jobsForEnv)
		{
			scheduler.deleteJob(JobKey.jobKey(job.getJobDetail().getName(), job.getJobDetail().getGroup()));
		}
	}
}