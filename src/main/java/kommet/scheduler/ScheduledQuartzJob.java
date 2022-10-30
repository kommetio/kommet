/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.scheduler;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduledQuartzJob implements Job
{	
	private static final Logger log = LoggerFactory.getLogger(ScheduledQuartzJob.class);
	
	public ScheduledQuartzJob()
	{
		// empty
	}
	
	public void execute(JobExecutionContext ctx) throws JobExecutionException
	{
		ScheduledTaskDetail detail = (ScheduledTaskDetail)ctx.getJobDetail();
		
		String jobName = detail.getKey().getName();
		
		// call the job
		log.info("Calling scheduled job " + jobName + " [" + detail.getGroup() + "]");
		
		try
		{
			ScheduledTaskService.execute(detail.getTask(), ctx.getScheduler(), detail.getEnvId());
		}
		catch (ScheduledTaskException e)
		{
			e.printStackTrace();
			throw new JobExecutionException("Error executing scheduled task: " + e.getMessage(), e);
		} 
	}
}