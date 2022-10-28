/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.UserGroupAssignment;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class UserGroupAssignmentFilter extends BasicFilter<UserGroupAssignment>
{
	private Set<KID> parentGroupIds;
	private Set<KID> childGroupIds;
	private Set<KID> childUserIds;
	private boolean fetchExtendedData;
	private Boolean isApplyPending;
	private Boolean isRemovePending;
	
	public void addParentGroupId (KID id)
	{
		if (this.parentGroupIds == null)
		{
			this.parentGroupIds = new HashSet<KID>();
		}
		this.parentGroupIds.add(id);
	}
	
	public void addChildGroupId (KID id)
	{
		if (this.childGroupIds == null)
		{
			this.childGroupIds = new HashSet<KID>();
		}
		this.childGroupIds.add(id);
	}
	
	public void addChildUserId (KID id)
	{
		if (this.childUserIds == null)
		{
			this.childUserIds = new HashSet<KID>();
		}
		this.childUserIds.add(id);
	}

	public Set<KID> getParentGroupIds()
	{
		return parentGroupIds;
	}

	public void setParentGroupIds(Set<KID> parentGroupIds)
	{
		this.parentGroupIds = parentGroupIds;
	}

	public Set<KID> getChildGroupIds()
	{
		return childGroupIds;
	}

	public void setChildGroupIds(Set<KID> childGroupIds)
	{
		this.childGroupIds = childGroupIds;
	}

	public Set<KID> getChildUserIds()
	{
		return childUserIds;
	}

	public void setChildUserIds(Set<KID> childUserIds)
	{
		this.childUserIds = childUserIds;
	}

	public boolean isFetchExtendedData()
	{
		return fetchExtendedData;
	}

	public void setFetchExtendedData(boolean fetchExtendedData)
	{
		this.fetchExtendedData = fetchExtendedData;
	}

	public Boolean isApplyPending()
	{
		return isApplyPending;
	}

	public void setApplyPending(Boolean isApplyPending)
	{
		this.isApplyPending = isApplyPending;
	}
	
	public Boolean isRemovePending()
	{
		return isRemovePending;
	}

	public void setRemovePending(Boolean isRemovePending)
	{
		this.isRemovePending = isRemovePending;
	}
}