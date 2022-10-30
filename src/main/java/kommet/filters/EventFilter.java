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

public class EventFilter extends BasicFilter<Event>
{
	private Set<KID> eventIds;
	private Set<KID> ownerIds;
	private String name;
	private String nameLike;
	private Date startDateFrom;
	private Date startDateTo;
	private Date endDateFrom;
	private Date endDateTo;
	private Date startDate;
	private Date endDate;
	private String descriptionLike;
	private Set<KID> guestIds;
	private boolean initGuests;

	public Set<KID> getEventIds()
	{
		return eventIds;
	}

	public void setEventIds(Set<KID> eventIds)
	{
		this.eventIds = eventIds;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getNameLike()
	{
		return nameLike;
	}

	public void setNameLike(String nameLike)
	{
		this.nameLike = nameLike;
	}

	public Date getStartDateFrom()
	{
		return startDateFrom;
	}

	public void setStartDateFrom(Date startDateFrom)
	{
		this.startDateFrom = startDateFrom;
	}

	public Date getStartDateTo()
	{
		return startDateTo;
	}

	public void setStartDateTo(Date startDateTo)
	{
		this.startDateTo = startDateTo;
	}

	public Date getEndDateFrom()
	{
		return endDateFrom;
	}

	public void setEndDateFrom(Date endDateFrom)
	{
		this.endDateFrom = endDateFrom;
	}

	public Date getEndDateTo()
	{
		return endDateTo;
	}

	public void setEndDateTo(Date endDateTo)
	{
		this.endDateTo = endDateTo;
	}

	public String getDescriptionLike()
	{
		return descriptionLike;
	}

	public void setDescriptionLike(String descriptionLike)
	{
		this.descriptionLike = descriptionLike;
	}
	
	public void addEventId(KID id)
	{
		if (this.eventIds == null)
		{
			this.eventIds = new HashSet<KID>();
		}
		this.eventIds.add(id);
	}

	public Set<KID> getOwnerIds()
	{
		return ownerIds;
	}

	public void setOwnerIds(Set<KID> ownerIds)
	{
		this.ownerIds = ownerIds;
	}
	
	public void addOwnerId(KID id)
	{
		if (this.ownerIds == null)
		{
			this.ownerIds = new HashSet<KID>();
		}
		this.ownerIds.add(id);
	}

	public boolean isInitGuests()
	{
		return initGuests;
	}

	public void setInitGuests(boolean initGuests)
	{
		this.initGuests = initGuests;
	}
	
	public void addGuestId (KID id)
	{
		if (this.guestIds == null)
		{
			this.guestIds = new HashSet<KID>();
		}
		this.guestIds.add(id);
	}

	/**
	 * List of guests for which events will be searched. The find method will return all events, which has at least one guest from the guestIds list.
	 * @return
	 */
	public Set<KID> getGuestIds()
	{
		return guestIds;
	}

	public void setGuestIds(Set<KID> guestIds)
	{
		this.guestIds = guestIds;
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