/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.reminders;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kommet.auth.AuthData;
import kommet.data.KommetException;

public class ReminderCheckerJob implements Job
{	
	private static final Logger log = LoggerFactory.getLogger(ReminderCheckerJob.class);
	
	public ReminderCheckerJob()
	{
		// empty
	}
	
	public void execute(JobExecutionContext ctx) throws JobExecutionException
	{	
		// call the job
		log.info("Checking reminders");
		
		ReminderCheckerJobDetail detail = (ReminderCheckerJobDetail)ctx.getJobDetail();
		
		try
		{
			detail.getReminderService().runReminders(AuthData.getRootAuthData(detail.getEnv()), detail.getEnv());
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			throw new JobExecutionException("Error executing scheduled task: " + e.getMessage(), e);
		} 
	}
}