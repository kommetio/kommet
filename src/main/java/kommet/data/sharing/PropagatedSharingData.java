/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.sharing;

import java.util.List;

import kommet.data.KID;

public class PropagatedSharingData
{
	private KID groupRecordSharingId;
	private KID userGroupAssignmentId;
	private List<String> groupHierarchyPath;
	
	public PropagatedSharingData (KID groupRecordSharingId, KID userGroupAssignmentId, List<String> groupHierarchyPath)
	{
		this.groupRecordSharingId = groupRecordSharingId;
		this.userGroupAssignmentId = userGroupAssignmentId;
		this.groupHierarchyPath = groupHierarchyPath;
	}

	public KID getGroupRecordSharingId()
	{
		return groupRecordSharingId;
	}

	public void setGroupRecordSharingId(KID groupRecordSharingId)
	{
		this.groupRecordSharingId = groupRecordSharingId;
	}

	public KID getUserGroupAssignmentId()
	{
		return userGroupAssignmentId;
	}

	public void setUserGroupAssignmentId(KID userGroupAssignmentId)
	{
		this.userGroupAssignmentId = userGroupAssignmentId;
	}

	public List<String> getGroupHierarchyPath()
	{
		return groupHierarchyPath;
	}

	public void setGroupHierarchyPath(List<String> groupHierarchyPath)
	{
		this.groupHierarchyPath = groupHierarchyPath;
	}
}