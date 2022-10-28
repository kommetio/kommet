/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.sharing;

import java.util.ArrayList;
import java.util.List;

import kommet.basic.UserGroup;
import kommet.data.KID;

public class GroupHierarchyPath
{
	private List<KID> groupToGroupAssignmentIds;
	private UserGroup topGroup;
	
	public GroupHierarchyPath (UserGroup topGroup)
	{
		this.topGroup = topGroup;
	}

	public void addBottomGroupAssignmentId(KID groupId)
	{
		if (groupToGroupAssignmentIds == null)
		{
			this.groupToGroupAssignmentIds = new ArrayList<KID>();
		}
		this.groupToGroupAssignmentIds.add(groupId);
	}
	
	public void addTopGroupId(KID groupId)
	{
		if (groupToGroupAssignmentIds == null)
		{
			this.groupToGroupAssignmentIds = new ArrayList<KID>();
		}
		this.groupToGroupAssignmentIds.add(0, groupId);
	}
	
	public List<KID> getGroupToGroupAssignmentIds()
	{
		return groupToGroupAssignmentIds;
	}

	public void setGroupToGroupAssignmentIds(List<KID> groupIds)
	{
		this.groupToGroupAssignmentIds = groupIds;
	}

	public UserGroup getTopGroup()
	{
		return topGroup;
	}

	public void setTopGroup(UserGroup topGroup)
	{
		this.topGroup = topGroup;
	}
}