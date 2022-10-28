/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.sharing;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.UserRecordSharing;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class UserRecordSharingFilter extends BasicFilter<UserRecordSharing>
{
	private Set<KID> recordIds;
	private Set<KID> userIds;
	private boolean initUser;
	private Set<KID> groupRecordSharingIds;
	private Set<KID> userGroupAssignmentIds;
	private Set<String> reasons;
	private String groupSharingHierarchy;
	private boolean emptyGroupSharingHierarchy;
	private Boolean isGeneric;
	private Set<KID> sharingRuleIds;
	
	public void addRecordId(KID id)
	{
		if (this.recordIds == null)
		{
			this.recordIds = new HashSet<KID>();
		}
		this.recordIds.add(id);
	}
	
	public void addUserId(KID id)
	{
		if (this.userIds == null)
		{
			this.userIds = new HashSet<KID>();
		}
		this.userIds.add(id);
	}
	
	public void addUserGroupAssignmentId(KID id)
	{
		if (this.userGroupAssignmentIds == null)
		{
			this.userGroupAssignmentIds = new HashSet<KID>();
		}
		this.userGroupAssignmentIds.add(id);
	}
	
	public void addGroupRecordSharingId(KID id)
	{
		if (this.groupRecordSharingIds == null)
		{
			this.groupRecordSharingIds = new HashSet<KID>();
		}
		this.groupRecordSharingIds.add(id);
	}

	public void setRecordIds(Set<KID> recordIds)
	{
		this.recordIds = recordIds;
	}

	public Set<KID> getRecordIds()
	{
		return recordIds;
	}

	public void setInitUser(boolean initUser)
	{
		this.initUser = initUser;
	}

	public boolean isInitUser()
	{
		return initUser;
	}

	public void setUserIds(Set<KID> userIds)
	{
		this.userIds = userIds;
	}

	public Set<KID> getUserIds()
	{
		return userIds;
	}

	public Set<KID> getGroupRecordSharingIds()
	{
		return groupRecordSharingIds;
	}

	public void setGroupRecordSharingIds(Set<KID> groupRecordSharingIds)
	{
		this.groupRecordSharingIds = groupRecordSharingIds;
	}

	public Set<KID> getUserGroupAssignmentIds()
	{
		return userGroupAssignmentIds;
	}

	public void setUserGroupAssignmentIds(Set<KID> userGroupAssignmentIds)
	{
		this.userGroupAssignmentIds = userGroupAssignmentIds;
	}

	public String getGroupSharingHierarchy()
	{
		return groupSharingHierarchy;
	}

	public void setGroupSharingHierarchy(String groupSharingHierarchy)
	{
		this.groupSharingHierarchy = groupSharingHierarchy;
	}

	public boolean getEmptyGroupSharingHierarchy()
	{
		return emptyGroupSharingHierarchy;
	}

	public void setEmptyGroupSharingHierarchy(boolean emptyGroupSharingHierarchy)
	{
		this.emptyGroupSharingHierarchy = emptyGroupSharingHierarchy;
	}

	public Boolean getIsGeneric()
	{
		return isGeneric;
	}

	public void setIsGeneric(Boolean isGeneric)
	{
		this.isGeneric = isGeneric;
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
}