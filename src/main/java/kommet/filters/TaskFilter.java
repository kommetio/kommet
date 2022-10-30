/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.filters;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import kommet.basic.Task;
import kommet.data.KID;

public class TaskFilter extends BasicFilter<Task>
{
	private String titleLike;
	private Set<KID> taskIds;
	private String contentLike;
	private Date dueDateFrom;
	private Date dueDateTo;
	private Set<Integer> priorities;
	private Set<String> statuses;
	private Set<KID> assignedUserIds;
	private Set<KID> assignedGroupIds;
	private Set<KID> recordIds;
	private boolean fetchAssigneeData = false;

	public String getTitleLike()
	{
		return titleLike;
	}

	public void setTitleLike(String titleLike)
	{
		this.titleLike = titleLike;
	}

	public String getContentLike()
	{
		return contentLike;
	}

	public void setContentLike(String contentLike)
	{
		this.contentLike = contentLike;
	}

	public Date getDueDateFrom()
	{
		return dueDateFrom;
	}

	public void setDueDateFrom(Date dueDateFrom)
	{
		this.dueDateFrom = dueDateFrom;
	}

	public Date getDueDateTo()
	{
		return dueDateTo;
	}

	public void setDueDateTo(Date dueDateTo)
	{
		this.dueDateTo = dueDateTo;
	}

	public Set<Integer> getPriorities()
	{
		return priorities;
	}

	public void setPriorities(Set<Integer> priorities)
	{
		this.priorities = priorities;
	}

	public Set<String> getStatuses()
	{
		return statuses;
	}

	public void setStatuses(Set<String> statuses)
	{
		this.statuses = statuses;
	}
	
	public void addStatus (String status)
	{
		if (this.statuses == null)
		{
			this.statuses = new HashSet<String>();
		}
		this.statuses.add(status);
	}
	
	public void addPriority (Integer priority)
	{
		if (this.priorities == null)
		{
			this.priorities = new HashSet<Integer>();
		}
		this.priorities.add(priority);
	}

	public Set<KID> getAssignedUserIds()
	{
		return assignedUserIds;
	}

	public void setAssignedUserIds(Set<KID> assignedUserIds)
	{
		this.assignedUserIds = assignedUserIds;
	}

	public Set<KID> getAssignedGroupIds()
	{
		return assignedGroupIds;
	}

	public void setAssignedGroupIds(Set<KID> assignedGroupIds)
	{
		this.assignedGroupIds = assignedGroupIds;
	}
	
	public void addAssignedUserId (KID id)
	{
		if (this.assignedUserIds == null)
		{
			this.assignedUserIds = new HashSet<KID>();
		}
		this.assignedUserIds.add(id);
	}
	
	public void addAssignedGroupId (KID id)
	{
		if (this.assignedGroupIds == null)
		{
			this.assignedGroupIds = new HashSet<KID>();
		}
		this.assignedGroupIds.add(id);
	}
	
	public void addTaskId(KID id)
	{
		if (this.taskIds == null)
		{
			this.taskIds = new HashSet<KID>();
		}
		this.taskIds.add(id);
	}

	public Set<KID> getTaskIds()
	{
		return taskIds;
	}

	public void setTaskIds(Set<KID> taskIds)
	{
		this.taskIds = taskIds;
	}

	public boolean isFetchAssigneeData()
	{
		return fetchAssigneeData;
	}

	public void setFetchAssigneeData(boolean fetchAssigneeData)
	{
		this.fetchAssigneeData = fetchAssigneeData;
	}

	public Set<KID> getRecordIds()
	{
		return recordIds;
	}

	public void setRecordIds(Set<KID> recordIds)
	{
		this.recordIds = recordIds;
	}
	
	public void addRecordId (KID id)
	{
		if (this.recordIds == null)
		{
			this.recordIds = new HashSet<KID>();
		}
		this.recordIds.add(id);
	}
}