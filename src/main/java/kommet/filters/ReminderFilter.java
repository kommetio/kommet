/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.filters;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.Reminder;
import kommet.data.KID;

public class ReminderFilter extends BasicFilter<Reminder>
{
	private String titleLike;
	private Set<KID> reminderIds;
	private Set<KID> recordIds;
	private String contentLike;
	private Set<KID> assignedUserIds;
	private Set<KID> assignedGroupIds;

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
	
	public void addReminderId(KID id)
	{
		if (this.reminderIds == null)
		{
			this.reminderIds = new HashSet<KID>();
		}
		this.reminderIds.add(id);
	}

	public Set<KID> getReminderIds()
	{
		return reminderIds;
	}

	public void setReminderIds(Set<KID> ids)
	{
		this.reminderIds = ids;
	}
	
	public void addRecordId(KID id)
	{
		if (this.recordIds == null)
		{
			this.recordIds = new HashSet<KID>();
		}
		this.recordIds.add(id);
	}

	public Set<KID> getRecordIds()
	{
		return recordIds;
	}

	public void setRecordIds(Set<KID> ids)
	{
		this.recordIds = ids;
	}
}