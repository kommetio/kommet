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

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.EVENT_GUEST_API_NAME)
public class EventGuest extends StandardTypeRecordProxy
{
	private String response;
	private String responseComment;
	private Event event;
	private User guest;
	
	public EventGuest() throws KommetException
	{
		this(null, null);
	}
	
	public EventGuest(Record r, EnvData env) throws KommetException
	{
		super(r, true, env);
	}

	@Property(field = "response")
	public String getResponse()
	{
		return response;
	}

	public void setResponse(String response)
	{
		this.response = response;
		setInitialized();
	}

	@Property(field = "responseComment")
	public String getResponseComment()
	{
		return responseComment;
	}

	public void setResponseComment(String responseComment)
	{
		this.responseComment = responseComment;
		setInitialized();
	}

	@Property(field = "event")
	public Event getEvent()
	{
		return event;
	}

	public void setEvent(Event event)
	{
		this.event = event;
		setInitialized();
	}

	@Property(field = "guest")
	public User getGuest()
	{
		return guest;
	}

	public void setGuest(User guest)
	{
		this.guest = guest;
		setInitialized();
	}
}