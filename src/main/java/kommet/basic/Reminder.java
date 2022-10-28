/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.REMINDER_API_NAME)
public class Reminder extends StandardTypeRecordProxy
{
	private KID referencedField;
	private KID recordId;
	private String media;
	private String intervalUnit;
	private Integer intervalValue;
	private String title;
	private String content;
	private User assignedUser;
	private UserGroup assignedGroup;
	private String status;
	
	public Reminder() throws KommetException
	{
		this(null, null);
	}
	
	public Reminder(Record reminder, EnvData env) throws KommetException
	{
		super(reminder, true, env);
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

	@Property(field = "referencedField")
	public KID getReferencedField()
	{
		return referencedField;
	}

	public void setReferencedField(KID referencedField)
	{
		this.referencedField = referencedField;
		setInitialized();
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

	@Property(field = "media")
	public String getMedia()
	{
		return media;
	}

	public void setMedia(String media)
	{
		this.media = media;
		setInitialized();
	}

	@Property(field = "intervalUnit")
	public String getIntervalUnit()
	{
		return intervalUnit;
	}

	public void setIntervalUnit(String intervalUnit)
	{
		this.intervalUnit = intervalUnit;
		setInitialized();
	}

	@Property(field = "intervalValue")
	public Integer getIntervalValue()
	{
		return intervalValue;
	}

	public void setIntervalValue(Integer intervalValue)
	{
		this.intervalValue = intervalValue;
		setInitialized();
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
}