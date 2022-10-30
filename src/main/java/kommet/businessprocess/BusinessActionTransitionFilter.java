/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.businessprocess;

import java.util.HashSet;
import java.util.Set;

import kommet.data.KID;

public class BusinessActionTransitionFilter
{
	private Set<KID> processIds;
	
	public Set<KID> getProcessIds()
	{
		return processIds;
	}

	public void setProcessIds(Set<KID> businessProcessIds)
	{
		this.processIds = businessProcessIds;
	}
	
	public void addProcessId(KID id)
	{
		if (this.processIds == null)
		{
			this.processIds = new HashSet<KID>();
		}
		this.processIds.add(id);
	}
}