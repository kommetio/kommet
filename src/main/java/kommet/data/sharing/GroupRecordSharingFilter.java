/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.sharing;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.GroupRecordSharing;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class GroupRecordSharingFilter extends BasicFilter<GroupRecordSharing>
{
	private Set<KID> recordIds;
	private Set<KID> groupIds;
	private Set<String> reasons;
	private Boolean isGeneric;
	private Set<KID> sharingRuleIds;
	private boolean isExcludeSystemObjects = false;
	
	public void addRecordId(KID id)
	{
		if (this.recordIds == null)
		{
			this.recordIds = new HashSet<KID>();
		}
		this.recordIds.add(id);
	}
	
	public void addGroupId(KID id)
	{
		if (this.groupIds == null)
		{
			this.groupIds = new HashSet<KID>();
		}
		this.groupIds.add(id);
	}

	public void setRecordIds(Set<KID> recordIds)
	{
		this.recordIds = recordIds;
	}

	public Set<KID> getRecordIds()
	{
		return recordIds;
	}

	public void setGroupIds(Set<KID> userIds)
	{
		this.groupIds = userIds;
	}

	public Set<KID> getGroupIds()
	{
		return groupIds;
	}
	
	public void addReason (String reason)
	{
		if (this.reasons == null)
		{
			this.reasons = new HashSet<String>();
		}
		this.reasons.add(reason);
	}
	
	public Set<String> getReasons()
	{
		return reasons;
	}

	public void setReasons(Set<String> reasons)
	{
		this.reasons = reasons;
	}

	public Boolean getIsGeneric()
	{
		return isGeneric;
	}

	public void setIsGeneric(Boolean isGeneric)
	{
		this.isGeneric = isGeneric;
	}
	
	public Set<KID> getSharingRuleIds()
	{
		return sharingRuleIds;
	}

	public void setSharingRuleIds(Set<KID> sharingRuleIds)
	{
		this.sharingRuleIds = sharingRuleIds;
	}
	
	public void addSharingRuleId (KID id)
	{
		if (this.sharingRuleIds == null)
		{
			this.sharingRuleIds = new HashSet<KID>();
		}
		this.sharingRuleIds.add(id);
	}

	public boolean isExcludeSystemObjects()
	{
		return isExcludeSystemObjects;
	}

	public void setExcludeSystemObjects(boolean isExcludeSystemObjects)
	{
		this.isExcludeSystemObjects = isExcludeSystemObjects;
	}
}