/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import java.util.Date;

import kommet.basic.types.SystemTypes;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.TASK_API_NAME)
public class Task extends StandardTypeRecordProxy
{
	private String title;
	private String content;
	private Date dueDate;
	private Integer priority;
	private String status;
	private User assignedUser;
	private UserGroup assignedGroup;
	private Integer progress;
	private KID recordId;
	
	public Task() throws KommetException
	{
		this(null, null);
	}
	
	public Task(Record task, EnvData env) throws KommetException
	{
		super(task, true, env);
	}

	@Property(field = "title")
	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
		setInitialized();
	}
	
	@Property(field = "content")
	public String getContent()
	{
		return content;
	}

	public void setContent(String content)
	{
		this.content = content;
		setInitialized();
	}

	@Property(field = "dueDate")
	public Date getDueDate()
	{
		return dueDate;
	}

	public void setDueDate(Date dueDate)
	{
		this.dueDate = dueDate;
		setInitialized();
	}

	@Property(field = "priority")
	public Integer getPriority()
	{
		return priority;
	}

	public void setPriority(Integer priority)
	{
		this.priority = priority;
		setInitialized();
	}
	
	@Transient
	public TaskPriority getPriorityValue() throws KommetException
	{
		return this.priority != null ? TaskPriority.valueOf(this.priority) : null;
	}

	@Property(field = "status")
	public String getStatus()
	{
		return status;
	}

	public void setStatus(String status)
	{
		this.status = status;
		setInitialized();
	}

	@Property(field = "assignedUser")
	public User getAssignedUser()
	{
		return assignedUser;
	}

	public void setAssignedUser(User assignedUser)
	{
		this.assignedUser = assignedUser;
		setInitialized();
	}

	@Property(field = "assignedGroup")
	public UserGroup getAssignedGroup()
	{
		return assignedGroup;
	}

	public void setAssignedGroup(UserGroup assignedGroup)
	{
		this.assignedGroup = assignedGroup;
		setInitialized();
	}

	@Property(field = "progress")
	public Integer getProgress()
	{
		return progress;
	}

	public void setProgress(Integer progress)
	{
		this.progress = progress;
		setInitialized();
	}
	
	@Transient
	public KID getAssigneeId()
	{
		if (this.assignedUser != null)
		{
			return this.assignedUser.getId();
		}
		else if (this.assignedGroup != null)
		{
			return this.assignedGroup.getId();
		}
		else
		{
			return null;
		}
	}

	@Property(field = "recordId")
	public KID getRecordId()
	{
		return recordId;
	}

	public void setRecordId(KID recordId)
	{
		this.recordId = recordId;
		setInitialized();
	}
}