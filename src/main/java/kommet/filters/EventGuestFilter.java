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

import kommet.basic.Event;
import kommet.data.KID;

public class EventGuestFilter extends BasicFilter<Event>
{
	private Set<KID> guestIds;
	private Set<KID> eventIds;
	private Date startDate;
	private Date endDate;
	
	public void addEventId (KID id)
	{
		if (this.eventIds == null)
		{
			this.eventIds = new HashSet<KID>();
		}
		this.eventIds.add(id);
	}
	
	public void addGuestId (KID id)
	{
		if (this.guestIds == null)
		{
			this.guestIds = new HashSet<KID>();
		}
		this.guestIds.add(id);
	}

	public Set<KID> getGuestIds()
	{
		return guestIds;
	}

	public void setGuestIds(Set<KID> guestIds)
	{
		this.guestIds = guestIds;
	}

	public Set<KID> getEventIds()
	{
		return eventIds;
	}

	public void setEventIds(Set<KID> eventIds)
	{
		this.eventIds = eventIds;
	}

	public Date getStartDate()
	{
		return startDate;
	}

	public void setStartDate(Date startDate)
	{
		this.startDate = startDate;
	}

	public Date getEndDate()
	{
		return endDate;
	}

	public void setEndDate(Date endDate)
	{
		this.endDate = endDate;
	}
}