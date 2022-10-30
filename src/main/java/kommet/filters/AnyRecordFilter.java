/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.filters;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.AnyRecord;
import kommet.data.KID;

public class AnyRecordFilter extends BasicFilter<AnyRecord>
{
	private Set<KID> recordIds;
	
	public void addRecordId(KID id)
	{
		if (this.recordIds == null)
		{
			this.recordIds = new HashSet<KID>();
		}
		this.recordIds.add(id);
	}

	public Set<KID> getRecordIds()
	{
		return recordIds;
	}

	public void setRecordIds(Set<KID> recordIds)
	{
		this.recordIds = recordIds;
	}
}