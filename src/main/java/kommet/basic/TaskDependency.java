/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.TASK_DEPENDENCY_API_NAME)
public class TaskDependency extends StandardTypeRecordProxy
{
	private Task parentTask;
	private Task childTask;
	private String dependencyType;
	
	public TaskDependency() throws KommetException
	{
		this(null, null);
	}
	
	public TaskDependency(Record task, EnvData env) throws KommetException
	{
		super(task, true, env);
	}

	@Property(field = "parentTask")
	public Task getParentTask()
	{
		return parentTask;
	}

	public void setParentTask(Task parentTask)
	{
		this.parentTask = parentTask;
		setInitialized();
	}

	@Property(field = "childTask")
	public Task getChildTask()
	{
		return childTask;
	}

	public void setChildTask(Task childTask)
	{
		this.childTask = childTask;
		setInitialized();
	}

	@Property(field = "dependencyType")
	public String getDependencyType()
	{
		return dependencyType;
	}

	public void setDependencyType(String dependencyType)
	{
		this.dependencyType = dependencyType;
		setInitialized();
	}
}