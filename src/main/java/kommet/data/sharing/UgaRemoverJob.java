/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.sharing;

import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kommet.auth.AuthData;
import kommet.basic.UserGroupAssignment;
import kommet.dao.UserGroupAssignmentFilter;

public class UgaRemoverJob implements Job
{	
	private static final Logger log = LoggerFactory.getLogger(UgaRemoverJob.class);
	
	private static Long inProgressSince;
	private static long MAX_TIME_MILLIS = 60000;
	
	public UgaRemoverJob()
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
		log.info("[Removing group sharings]");
		
		if (inProgressSince != null)
		{
			long millis = System.currentTimeMillis() - inProgressSince;
			log.info("[Removing group sharings] Previous job still in progress (for " + (millis / 1000) + " seconds)");
			return;
		}
		
		UgaRemoverJobDetail detail = (UgaRemoverJobDetail)ctx.getJobDetail();
		
		try
		{
			setInProgress(System.currentTimeMillis());
			
			AuthData authData = AuthData.getRootAuthData(detail.getEnv());
			
			UserGroupAssignmentFilter filter = new UserGroupAssignmentFilter();
			filter.setRemovePending(true);
			List<UserGroupAssignment> assignments = detail.getUserGroupService().getUserGroupAssignments(filter, authData, detail.getEnv());
			
			log.info("[Removing group sharings] Found sharings to remove: " + assignments.size());
			
			long loopStart = System.currentTimeMillis();
			int index = 1;
			
			for (UserGroupAssignment uga : assignments)
			{
				log.info("[Removing group sharings] Removing UGA " + index + " / " + assignments.size());
				
				int batchCount = 1;
				
				while (true)
				{
					log.info("[Removing group sharings] Removing UGA " + index + " / " + assignments.size() + ", batch " + batchCount + " / ?");
					
					int itemsRemaining = detail.getUserGroupService().propagateDeleteUserGroupAssignment(uga.getId(), 100, detail.getEnv());
					
					if (itemsRemaining <= 0)
					{
						// all URS for this UGA have been removed
						uga.setIsRemovePending(false);
						detail.getUserGroupService().deleteUserGroupAssignment(uga.getId(), authData, detail.getEnv());
						
						break;
					}
					
					batchCount++;
				}
				
				if ((System.currentTimeMillis() - loopStart) > MAX_TIME_MILLIS)
				{
					log.info("[Removing group sharings] Loop timeout");
					break;
				}
				
				index++;
			}
		}
		catch (Exception e)
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
