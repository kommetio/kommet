/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.actions;

import java.util.HashSet;
import java.util.Set;

import kommet.data.KID;
import kommet.data.KommetException;

public class StandardActionFilter
{
	private KID typeId;
	private StandardActionType pageType;
	private KID profileId;
	private Set<KID> actionIds;
	private Set<KID> ids;

	public void setTypeId(KID typeId)
	{
		this.typeId = typeId;
	}

	public KID getTypeId()
	{
		return typeId;
	}

	public void setPageType(StandardActionType type)
	{
		this.pageType = type;
	}

	public StandardActionType getPageType()
	{
		return pageType;
	}

	public void setProfileId(KID profileId)
	{
		this.profileId = profileId;
	}

	public KID getProfileId()
	{
		return profileId;
	}
	
	public void addActionId(KID id) throws KommetException
	{
		if (!id.toString().startsWith(KID.ACTION_PREFIX))
		{
			throw new KommetException("The ID passed as parameter " + id + " is not an ID of type action.");
		}
		
		if (this.actionIds == null)
		{
			this.actionIds = new HashSet<KID>();
		}
		this.actionIds.add(id);
	}

	public void setActionIds(Set<KID> pageIds)
	{
		this.actionIds = pageIds;
	}

	public Set<KID> getPageIds()
	{
		return actionIds;
	}
	
	public void addId(KID id)
	{
		if (this.ids == null)
		{
			this.ids = new HashSet<KID>();
		}
		this.ids.add(id);
	}

	public void setIds(Set<KID> ids)
	{
		this.ids = ids;
	}

	public Set<KID> getIds()
	{
		return ids;
	}
}
