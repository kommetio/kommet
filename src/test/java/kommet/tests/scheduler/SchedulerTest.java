/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import kommet.auth.AuthData;
import kommet.basic.Class;
import kommet.basic.ScheduledTask;
import kommet.data.DataService;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.koll.ClassService;
import kommet.koll.compiler.CompilationResult;
import kommet.koll.compiler.KommetCompiler;
import kommet.scheduler.ScheduledJob;
import kommet.scheduler.ScheduledTaskDetail;
import kommet.scheduler.ScheduledTaskException;
import kommet.scheduler.ScheduledTaskService;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;

public class SchedulerTest extends BaseUnitTest
{
	@Inject
	ScheduledTaskService schedulerService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	ClassService classService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	SchedulerFactoryBean schedulerFactory;
	
	@Inject
	DataService dataService;
	
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	
	@Before
	public void setUpStreams()
	{
	    System.setOut(new PrintStream(outContent));
	}
	
	@Test
	public void testScheduledTask() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		AuthData authData = dataHelper.getRootAuthData(env);
		
		Class taskFile = getTestKollFile(env);
		classService.save(taskFile, authData, env);
		CompilationResult result = compiler.compile(taskFile, env);
		assertTrue(result.getDescription(), result.isSuccess());
		
		ScheduledTask task = null;
		try
		{
			task = schedulerService.schedule(taskFile, "nonExistingMethod", "TaskOne", "0 0 * * * ?", authData, env);
			fail("Scheduling task class with non-existing method should fail");
		}
		catch (ScheduledTaskException e)
		{
			assertTrue("Invalid error message: " + e.getMessage(), e.getMessage().startsWith("Method nonExistingMethod does not exist in class"));
		}
		
		// now use existing method and test that the task can be successfully scheduled
		task = schedulerService.schedule(taskFile, "execute", "TaskOne", "0 0 * * * ?", authData, env);
		assertNotNull(task.getId());
		
		// retrieve scheduled task by id
		task = schedulerService.get(task.getId(), env);
		assertNotNull(task);
		assertNotNull(schedulerService.getByName(task.getName(), authData, env));
		
		// try to save the task again with a different name
		task.setName("TaskOneBis");
		task = schedulerService.schedule(task, authData, env);
		
		// make sure a scheduled job has been created
		ScheduledJob job = schedulerService.getScheduledJob(task.getQuartzJobName(), env.getId());
		assertNotNull(job);
		assertNotNull(job.getJobDetail());
		assertTrue(job.getJobDetail() instanceof ScheduledTaskDetail);
		assertNotNull(job.getTrigger());
		assertEquals(task.getId(), job.getTask().getId());
		
		// now retrieve all scheduled tasks for this environment
		List<ScheduledJob> jobs = schedulerService.getScheduledJobs(env.getId());
		assertNotNull(jobs);
		assertEquals(1, jobs.size());
		assertEquals(job.getTask().getId(), jobs.get(0).getTask().getId());
		
		// get scheduled task detail
		ScheduledTaskDetail taskDetail = ScheduledTaskService.getScheduledTaskDetail(task, schedulerFactory.getScheduler(), env.getId());
		assertNotNull(taskDetail);
		assertEquals(env.getId(), taskDetail.getEnvId());
		assertEquals(ScheduledTaskService.getJobGroupNameForEnv(env.getId()), taskDetail.getGroup());
		assertNotNull(taskDetail.getSystemContext());
		
		// now test running this task
		ScheduledTaskService.execute(task, schedulerFactory.getScheduler(), env.getId());
		
		// the scheduled task should print something to stdout, so we check it here
		assertEquals("hello from scheduled task", outContent.toString());
		
		// now unschedule the job, but do not delete the task
		schedulerService.unschedule(task.getId(), false, env);
		assertNull(schedulerService.getScheduledJob(task.getName(), env.getId()));
		assertNotNull("Task should have not been deleted together with the job", schedulerService.get(task.getId(), env));
		
		assertNull("Scheduled task detail should not have been found, because the task has been unscheduled", ScheduledTaskService.getScheduledTaskDetail(task, schedulerFactory.getScheduler(), env.getId()));
		
		testDeleteTask(taskFile, env);
	}
	
	private void testDeleteTask(Class taskFile, EnvData env) throws KommetException
	{
		AuthData authData = dataHelper.getRootAuthData(env);
		ScheduledTask task = null;
		try
		{
			task = schedulerService.schedule(taskFile, "nonExistingMethod", "TaskTwo", "0 0 10 * * ?", authData, env);
			fail("Scheduling task class with non-existing method should fail");
		}
		catch (ScheduledTaskException e)
		{
			assertTrue(e.getMessage().startsWith("Method nonExistingMethod does not exist in class"));
		}
		
		// now use existing method and test that the task can be successfully scheduled
		task = schedulerService.schedule(taskFile, "execute", "TaskTwo", "0 0 10 * * ?", authData, env);
		assertNotNull(task.getId());
		
		schedulerService.unschedule(task.getId(), true, env);
		assertNull(schedulerService.getScheduledJob(task.getName(), env.getId()));
		assertNull(schedulerService.getScheduledJob(task.getQuartzJobName(), env.getId()));
		assertNull("Task should have been deleted together with the job", schedulerService.get(task.getId(), env));
	}

	private Class getTestKollFile(EnvData env) throws KommetException
	{		
		Class file = new Class();
		file.setIsSystem(false);
		
		String kollCode = "package kommet.test; public class Task { public void execute() { System.out.print(\"hello from scheduled task\"); } }";
		
		file.setKollCode(kollCode);
		file.setName("Task");
		file.setPackageName("kommet.test");
		file.setJavaCode(classService.getKollTranslator(env).kollToJava(kollCode, true, dataHelper.getRootAuthData(env), env));
		return file;
	}
}
