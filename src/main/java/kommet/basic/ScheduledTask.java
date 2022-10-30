/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.ComponentType;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.deployment.Deployable;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.SCHEDULED_TASK_API_NAME)
public class ScheduledTask extends StandardTypeRecordProxy implements Deployable
{
	private Class file;
	private String name;
	private String cronExpression;
	private String method;
	
	public ScheduledTask() throws KommetException
	{
		this(null, null);
	}
	
	public ScheduledTask (Record task, EnvData env) throws KommetException
	{
		super(task, false, env);
	}

	public void setFile(Class file)
	{
		this.file = file;
		setInitialized();
	}

	@Property(field = "file")
	public Class getFile()
	{
		return file;
	}

	public void setName(String name)
	{
		this.name = name;
		setInitialized();
	}

	@Property(field = "name")
	public String getName()
	{
		return name;
	}

	public void setCronExpression(String cronExpression)
	{
		this.cronExpression = cronExpression;
		setInitialized();
	}

	@Property(field = "cronExpression")
	public String getCronExpression()
	{
		return cronExpression;
	}

	public void setMethod(String method)
	{
		this.method = method;
		setInitialized();
	}

	@Property(field = "method")
	public String getMethod()
	{
		return method;
	}
	
	@Transient
	public ComponentType getComponentType()
	{
		return ComponentType.SCHEDULED_TASK;
	}
	
	/**
	 * Returns the name of the quartz job associated with this task.
	 * @return
	 */
	@Transient
	public String getQuartzJobName()
	{
		return "scheduled-job-" + this.id;
	}
}