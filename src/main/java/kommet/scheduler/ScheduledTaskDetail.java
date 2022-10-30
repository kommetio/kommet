/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.scheduler;

import org.quartz.Job;
import org.quartz.impl.JobDetailImpl;

import kommet.basic.ScheduledTask;
import kommet.data.KID;
import kommet.env.EnvData;
import kommet.koll.SystemContext;
import kommet.koll.compiler.KommetCompiler;

public class ScheduledTaskDetail extends JobDetailImpl
{
	private static final long serialVersionUID = 5778468708428121361L;
	private ScheduledTask task;
	private SystemContext systemContext;
	private KommetCompiler compiler;
	private KID envId;
	
	public ScheduledTaskDetail (ScheduledTask task, SystemContext systemContext, KommetCompiler compiler, Class<? extends Job> cls, EnvData env)
	{	
		super();
		
		setName(task.getQuartzJobName());
		setGroup(ScheduledTaskService.getJobGroupNameForEnv(env.getId()));
		setJobClass(cls);
		
		this.task = task;
		this.systemContext = systemContext;
		this.compiler = compiler;
		this.envId = env.getId();
	}

	public ScheduledTask getTask()
	{
		return task;
	}

	public SystemContext getSystemContext()
	{
		return systemContext;
	}

	public KommetCompiler getCompiler()
	{
		return compiler;
	}

	public KID getEnvId()
	{
		return envId;
	}
}