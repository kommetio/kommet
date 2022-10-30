/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.deployment;

import java.util.ArrayList;
import java.util.List;

import kommet.basic.ScheduledTask;

/**
 * Contains items created during deployment that are not rolled back automatically and need to be deleted manually
 * in case the deployment fails.
 * @author Radek Krawiec
 * @since 08/04/2016
 */
public class DeploymentCleanupItems
{
	private List<ScheduledTask> scheduledTasks;
	private List<String> diskFiles;
	
	public void addScheduledTask(ScheduledTask task)
	{
		if (this.scheduledTasks == null)
		{
			this.scheduledTasks = new ArrayList<ScheduledTask>();
		}
		this.scheduledTasks.add(task);
	}
	
	public void addDiskFile(String fileName)
	{
		if (this.diskFiles == null)
		{
			this.diskFiles = new ArrayList<String>();
		}
		this.diskFiles.add(fileName);
	}

	public List<ScheduledTask> getScheduledTasks()
	{
		return scheduledTasks;
	}

	public void setScheduledTasks(List<ScheduledTask> scheduledTasks)
	{
		this.scheduledTasks = scheduledTasks;
	}

	public List<String> getDiskFiles()
	{
		return diskFiles;
	}

	public void setDiskFiles(List<String> diskFiles)
	{
		this.diskFiles = diskFiles;
	}
}