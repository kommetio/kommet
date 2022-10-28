/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.notifications;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.Notification;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class NotificationFilter extends BasicFilter<Notification>
{
	private Set<KID> notificationIds;
	private Set<KID> assigneeIds;
	
	public void addAssigneeId(KID id)
	{
		if (this.assigneeIds == null)
		{
			this.assigneeIds = new HashSet<KID>();
		}
		this.assigneeIds.add(id);
	}
	
	public void addNotificationId(KID id)
	{
		if (this.notificationIds == null)
		{
			this.notificationIds = new HashSet<KID>();
		}
		this.notificationIds.add(id);
	}

	public void setNotificationIds(Set<KID> notificationIds)
	{
		this.notificationIds = notificationIds;
	}

	public Set<KID> getNotificationIds()
	{
		return notificationIds;
	}

	public void setAssigneeIds(Set<KID> userIds)
	{
		this.assigneeIds = userIds;
	}

	public Set<KID> getAssigneeIds()
	{
		return assigneeIds;
	}
}