/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.scheduler;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.ScheduledTask;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class ScheduledTaskFilter extends BasicFilter<ScheduledTask>
{
	private String nameLike;
	private Set<KID> tasksIds;
	private String name;

	public void setNameLike(String nameLike)
	{
		this.nameLike = nameLike;
	}

	public String getNameLike()
	{
		return nameLike;
	}
	
	public void addTaskId(KID id)
	{
		if (this.tasksIds == null)
		{
			this.tasksIds = new HashSet<KID>();
		}
		this.tasksIds.add(id);
	}

	public void setTasksIds(Set<KID> tasksIds)
	{
		this.tasksIds = tasksIds;
	}

	public Set<KID> getTasksIds()
	{
		return tasksIds;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}
}