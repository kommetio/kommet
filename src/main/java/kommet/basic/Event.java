/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import java.util.ArrayList;
import java.util.Date;

import kommet.basic.types.SystemTypes;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.EVENT_API_NAME)
public class Event extends StandardTypeRecordProxy
{
	private String name;
	private String description;
	private Date startDate;
	private Date endDate;
	private User owner;
	private ArrayList<EventGuest> guests;
	
	public Event() throws KommetException
	{
		this(null, null);
	}
	
	public Event(Record r, EnvData env) throws KommetException
	{
		super(r, true, env);
	}

	@Property(field = "name")
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
		setInitialized();
	}

	@Property(field = "description")
	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
		setInitialized();
	}

	@Property(field = "startDate")
	public Date getStartDate()
	{
		return startDate;
	}

	public void setStartDate(Date startDate)
	{
		this.startDate = startDate;
		setInitialized();
	}

	@Property(field = "endDate")
	public Date getEndDate()
	{
		return endDate;
	}

	public void setEndDate(Date endDate)
	{
		this.endDate = endDate;
		setInitialized();
	}

	@Property(field = "owner")
	public User getOwner()
	{
		return owner;
	}

	public void setOwner(User owner)
	{
		this.owner = owner;
		setInitialized();
	}

	@Property(field = "guests")
	public ArrayList<EventGuest> getGuests()
	{
		return guests;
	}

	public void setGuests(ArrayList<EventGuest> guests)
	{
		this.guests = guests;
		setInitialized();
	}
}