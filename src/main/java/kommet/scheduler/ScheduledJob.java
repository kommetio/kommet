/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.scheduler;

import org.quartz.Trigger;

import kommet.basic.ScheduledTask;

/**
 * Represents a singled scheduled Quartz job with its trigger info.
 * @author Radek Krawiec
 * @date 24/05/2014
 */
public class ScheduledJob
{
	private ScheduledTask task;
	private ScheduledTaskDetail jobDetail;
	private Trigger trigger;

	public void setTask(ScheduledTask task)
	{
		this.task = task;
	}

	public ScheduledTask getTask()
	{
		return task;
	}

	public void setJobDetail(ScheduledTaskDetail jobDetail)
	{
		this.jobDetail = jobDetail;
	}

	public ScheduledTaskDetail getJobDetail()
	{
		return jobDetail;
	}

	public void setTrigger(Trigger trigger)
	{
		this.trigger = trigger;
	}

	public Trigger getTrigger()
	{
		return trigger;
	}
}