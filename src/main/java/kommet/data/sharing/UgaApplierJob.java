/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.sharing;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kommet.auth.AuthData;
import kommet.data.KommetException;

public class UgaApplierJob implements Job
{	
	private static final Logger log = LoggerFactory.getLogger(UgaApplierJob.class);
	
	private static Long inProgressSince;
	
	public UgaApplierJob()
	{
		// empty
	}
	
	synchronized private static void setInProgress (Long startTime)
	{
		inProgressSince = startTime;
	}
	
	public void execute(JobExecutionContext ctx) throws JobExecutionException
	{	
		// call the job
		log.info("[Applying group sharings]");
		
		if (inProgressSince != null)
		{
			long millis = System.currentTimeMillis() - inProgressSince;
			log.info("[Applying group sharings] Previous job still in progress (for " + (millis / 1000) + " seconds)");
			return;
		}
		
		UgaApplierJobDetail detail = (UgaApplierJobDetail)ctx.getJobDetail();
		
		try
		{
			setInProgress(System.currentTimeMillis());
			detail.getUserGroupService().batchPropagatePendingUserGroupSharings(AuthData.getRootAuthData(detail.getEnv()), detail.getEnv());
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			throw new JobExecutionException("Error executing scheduled task: " + e.getMessage(), e);
		}
		finally
		{
			// release lock
			setInProgress(null);
		}
	}
}