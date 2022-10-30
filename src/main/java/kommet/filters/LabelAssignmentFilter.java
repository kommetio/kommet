/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.filters;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.LabelAssignment;
import kommet.data.KID;

public class LabelAssignmentFilter extends BasicFilter<LabelAssignment>
{
	private Set<KID> labelIds;
	private Set<KID> recordIds;

	public Set<KID> getLabelIds()
	{
		return labelIds;
	}

	public void setLabelIds(Set<KID> labelIds)
	{
		this.labelIds = labelIds;
	}

	public Set<KID> getRecordIds()
	{
		return recordIds;
	}

	public void setRecordIds(Set<KID> recordIds)
	{
		this.recordIds = recordIds;
	}
	
	public void addLabelId(KID id)
	{
		if (this.labelIds == null)
		{
			this.labelIds = new HashSet<KID>();
		}
		this.labelIds.add(id);
	}
	
	public void addRecordId(KID id)
	{
		if (this.recordIds == null)
		{
			this.recordIds = new HashSet<KID>();
		}
		this.recordIds.add(id);
	}
}