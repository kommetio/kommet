/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import java.util.Date;

import kommet.basic.types.SystemTypes;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.NOTIFICATION_API_NAME)
public class Notification extends StandardTypeRecordProxy
{
	private String text;
	private String title;
	private User assignee;
	private Date viewedDate;
	
	public Notification() throws KommetException
	{
		this(null, null);
	}
	
	public Notification(Record notification, EnvData env) throws KommetException
	{
		super(notification, true, env);
	}

	public void setText(String text)
	{
		this.text = text;
		setInitialized();
	}

	@Property(field = "text")
	public String getText()
	{
		return text;
	}

	public void setAssignee(User assignee)
	{
		this.assignee = assignee;
		setInitialized();
	}

	@Property(field = "assignee")
	public User getAssignee()
	{
		return assignee;
	}

	public void setViewedDate(Date viewedDate)
	{
		this.viewedDate = viewedDate;
		setInitialized();
	}

	@Property(field = "viewedDate")
	public Date getViewedDate()
	{
		return viewedDate;
	}

	public void setTitle(String title)
	{
		this.title = title;
		setInitialized();
	}

	@Property(field = "title")
	public String getTitle()
	{
		return title;
	}
}