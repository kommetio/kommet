/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.triggers;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.TypeTrigger;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class TypeTriggerFilter extends BasicFilter<TypeTrigger>
{
	private Set<KID> typeIds;
	private Set<KID> triggerFileIds;
	private Set<KID> typeTriggerIds;
	private boolean initClassCode;
	
	public void addTypeId(KID typeId)
	{
		if (this.typeIds == null)
		{
			this.typeIds = new HashSet<KID>();
		}
		this.typeIds.add(typeId);
	}
	
	public void addTriggerFileId(KID fileId)
	{
		if (this.triggerFileIds == null)
		{
			this.triggerFileIds = new HashSet<KID>();
		}
		this.triggerFileIds.add(fileId);
	}

	public Set<KID> getTypeIds()
	{
		return typeIds;
	}

	public Set<KID> getTriggerFileIds()
	{
		return triggerFileIds;
	}

	public void setTypeTriggerIds(Set<KID> typeTriggerIds)
	{
		this.typeTriggerIds = typeTriggerIds;
	}

	public Set<KID> getTypeTriggerIds()
	{
		return typeTriggerIds;
	}
	
	public void addTypeTriggerId(KID id)
	{
		if (this.typeTriggerIds == null)
		{
			this.typeTriggerIds = new HashSet<KID>();
		}
		this.typeTriggerIds.add(id);
	}

	public boolean isInitClassCode()
	{
		return initClassCode;
	}

	public void setInitClassCode(boolean initClassCode)
	{
		this.initClassCode = initClassCode;
	}
}